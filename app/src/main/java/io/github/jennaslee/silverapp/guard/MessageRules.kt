package io.github.jennaslee.silverapp.guard

/** 감시 대상 앱에서 받은 메시지 한 건 */
data class IncomingMessage(
    val source: Source,
    val sender: String,
    val body: String,
    val senderInContacts: Boolean,
) {
    enum class Source(val apiValue: String) { SMS("sms"), KAKAO("kakao"), OTHER("other") }
}

/** 1차 규칙 검사 결과 */
data class RuleResult(
    val level: Level,
    val score: Int,
    val hits: List<String>,
) {
    enum class Level {
        /** 서버에 묻지 않고 그대로 둔다 */
        SAFE,

        /** 서버에 판정을 맡긴다 */
        SUSPICIOUS,

        /** 서버에 판정을 맡기고, 서버가 응답하지 못해도 경고한다 */
        HIGH_RISK,
    }
}

/**
 * 휴대폰 안에서 바로 하는 1차 검사. 대부분의 평범한 메시지는 여기서 끝나고 서버로 가지 않는다.
 * 점수가 높을수록 위험.
 *
 * 기준값과 낱말은 한국어 스미싱 데이터셋으로 평가하며 조정한다 (RulesDatasetEval).
 * 목표: 스미싱을 SAFE로 놓치는 비율은 낮게, 정상을 HIGH_RISK로 잘못 경고하는 비율은 더 낮게.
 */
object MessageRules {

    private const val SUSPICIOUS_SCORE = 2
    private const val HIGH_RISK_SCORE = 5

    // 링크: 프로토콜/www, 또는 "도메인/경로", 또는 사기에 자주 쓰이는 끝자리 도메인.
    // "https:// 주소"처럼 중간에 띄어쓰기를 넣어 숨긴 경우도 잡는다.
    private val URL = Regex(
        """(?i)\bhttps?:/\s?/\s?\S+|\bwww\.\S+|\b[a-z0-9-]+(\.[a-z0-9-]+)*\.[a-z]{2,6}/\S*|""" +
            """\b[a-z0-9-]+(\.[a-z0-9-]+)*\.(com|net|org|kr|co\.kr|me|do|ly|gl|gd|io|xyz|top|cc|link|site|shop|info|host|cool|guru|su|cn|im|la|at|pe|id|click|online|live|example)\b|""" +
            // "nnop.djv3.pet"처럼 짧은 이름 세 마디로 된 주소 (사기 문자에서 흔함)
            """\b[a-z0-9]{1,8}\.[a-z0-9]{2,8}\.[a-z]{2,6}\b""",
    )
    private val SHORT_URL_HOSTS = listOf(
        "bit.ly", "bit/ly", "me2.do", "han.gl", "vo.la", "url.kr", "t.ly", "tinyurl.com", "goo.gl", "is.gd", "buly.kr",
        "c11.kr", "abit.ly", "t.co", "s.id", "kr.pe", "shorturl.at", "soo.gd", "tsu.im", "naver.me", "tny.im",
    )
    private val APK = Regex("""(?i)\.apk\b""")

    /** 인증번호만 알려주는 문자: 링크가 없으면 정상으로 본다 */
    private val OTP = Regex("""(인증\s*번호|인증\s*코드|확인\s*코드|verification code|OTP)[^0-9]{0,15}\d{4,8}""", RegexOption.IGNORE_CASE)

    /** 낱말 묶음. 묶음마다 한 번만 점수를 준다. */
    private class Group(val name: String, val weight: Int, val words: List<String> = emptyList(), val patterns: List<Regex> = emptyList()) {
        fun find(body: String): String? =
            words.firstOrNull { body.contains(it) } ?: patterns.firstNotNullOfOrNull { it.find(body)?.value?.take(12) }
    }

    private val GROUPS = listOf(
        Group("delivery", 1, listOf("택배", "배송", "운송장", "관세", "소포", "등기")),
        // 택배 "문제"를 핑계로 주소 확인을 시키는 말
        Group(
            "delivery_problem", 2,
            listOf("반송", "도로명", "미배달", "배송불가", "배송 불가", "배달이 잘못", "주소불명", "주소 불명", "주소지", "보류", "분실"),
            listOf(Regex("""주소\s?(확인|불일치|수정|오류|틀|재확인)""")),
        ),
        Group("gov", 1, listOf("검찰", "지검", "경찰", "법원", "수사", "금감원", "금융감독원", "건강보험", "국민연금", "과태료", "범칙금", "법칙금", "벌점", "무단투기", "통지서", "위반")),
        // "검진 통지서 내용확인:주소"처럼 문서를 열어보게 하는 말
        Group(
            "document_lure", 2,
            patterns = listOf(Regex("""(통지서|진단서|통보서|고지서|검진|결과\s?보기|사실\s?확인).{0,25}(확인|보기|조회)\s?[:：\[(]?\s?[a-z0-9]""", RegexOption.IGNORE_CASE)),
        ),
        // 가족·지인 사칭: 링크 없이 오는 경우가 많아 무게를 크게 둔다
        Group(
            "family", 3,
            listOf("문자나라", "피씨용", "PC용", "안심번호", "임시폰", "임시 폰", "임시번호", "엄마 나", "아빠 나", "엄마나", "아빠나"),
            listOf(
                Regex("""(핸드폰|휴대폰|폰)\s?(이\s?)?(고장|망가|액정|깨져|떨어뜨|수리)"""),
                Regex("""액정\s?(이\s?)?(나가|깨|고장)"""),
                Regex("""(컴터|컴퓨터|피시)로"""),
                Regex("""(이|새|여기)\s?번호로.{0,15}(톡|카톡|추가|연락|문자)"""),
                Regex("""카톡\s?추가"""),
                Regex("""편의점.{0,6}(다녀|갔다|갓다|가줄|갈 수|갈수)"""),
                // 기프트카드·상품권을 대신 사 달라는 부탁
                Regex("""(기프트\s?카드|상품권|만원권).{0,25}(사\s?줄|사줘|구매\s?해|사다|사와|구입\s?해)"""),
            ),
        ),
        Group("personal_info", 2, listOf("신분증", "카드 앞뒤", "카드 사진", "비밀번호", "인증번호 알려", "인증번호 보내", "원격 제어", "원격제어", "원격 앱", "팀뷰어")),
        Group("payment", 1, listOf("해외결제", "해외 결제", "결제 승인", "승인 완료", "결제완료", "본인 아닐", "본인이 아닐")),
        // 큰 금액 결제·승인을 알리고 일반 전화번호(02, 031, 070 등)로 연락하게 함. 진짜 카드사는 대표번호(15xx 등)를 쓴다.
        Group(
            "payment_callback", 3,
            patterns = listOf(
                Regex("""(결제|승인|출고|주문|확인)\s?완료.{0,80}\b0(2|[3-6]\d|70)\s?-?\s?\d{3,4}\s?-?\s?\d{4}"""),
                Regex("""\b0(2|[3-6]\d|70)\s?-?\s?\d{3,4}\s?-?\s?\d{4}.{0,40}(결제|승인)\s?완료"""),
                Regex("""본인\s?아닐.{0,30}\b0(2|[3-6]\d|70)\s?-?\s?\d{3,4}"""),
            ),
        ),
        Group("invitation", 1, listOf("부고", "별세", "청첩장", "모바일 초대장", "돌잔치", "결혼식")),
        Group("loan", 2, listOf("대출", "대환", "저금리", "한도 조회", "정부지원", "정부 지원", "특례", "무이자", "보증지원", "긴급자금", "생활자금", "긴급지원")),
        Group("investment", 1, listOf("리딩방", "수익률", "급등", "원금 보장", "원금보장", "코인", "고수익", "암호화폐", "거래소", "자산을")),
        // 계정 도용 경고를 사칭 ("해외IP에서 로그인되었습니다", "자산이 위험합니다")
        Group(
            "account_alert", 2,
            listOf("해외IP", "해외 IP", "해외아이피"),
            listOf(Regex("""(로그인|접속).{0,15}(차단|해외|위험)"""), Regex("""자산이?\s?위험""")),
        ),
        Group("money", 2, listOf("계좌", "송금", "이체", "입금", "현금")),
        Group("voucher", 1, listOf("상품권", "기프트카드")),
        Group("urgency", 1, listOf("급해", "급하게", "급히", "즉시", "오늘까지", "긴급", "24시간 이내")),
    )

    private val FOREIGN_TAGS = listOf("[국외발신]", "[국제발신]", "[해외발신]")

    /**
     * 정상 광고 표시, 또는 통신사·은행의 "사칭 문자 주의 안내".
     * 이게 있으면 서버 판정 없이 경고하지는 않는다 (서버 판정은 받음).
     */
    private val AD_MARKER = Regex("""\(광고\).*(수신\s?거부|무료\s?거부)|(사칭|스미싱|피싱|스팸).{0,20}(주의|조심|유의)""")

    /** 줄바꿈·특수 공백·여러 종류의 하이픈을 정리해서 패턴이 줄을 넘어 맞도록 한다 */
    private fun normalize(text: String): String = text
        .replace(Regex("[\u200B-\u200D\uFEFF]"), "")
        .replace(Regex("[\u2010-\u2015\u2212]"), "-")
        .replace(Regex("""\s+"""), " ")
        .trim()

    fun check(msg: IncomingMessage): RuleResult {
        val body = normalize(msg.body)
        val hits = mutableListOf<String>()
        var score = 0

        // 링크
        val urls = URL.findAll(body).map { it.value.lowercase() }.toList()
        val lowered = body.lowercase()
        val hasShortUrl = SHORT_URL_HOSTS.any { lowered.contains(it) }
        when {
            APK.containsMatchIn(body) -> { hits += "apk"; score += 5 }
            hasShortUrl -> { hits += "short_url"; score += 3 }
            urls.isNotEmpty() -> { hits += "url"; score += 2 }
        }

        // 인증번호 문자는 링크가 없으면 정상
        if (hits.isEmpty() && OTP.containsMatchIn(body)) {
            return RuleResult(RuleResult.Level.SAFE, 0, listOf("otp"))
        }

        // 의심 낱말 (묶음별로 한 번씩만)
        for (group in GROUPS) {
            val word = group.find(body) ?: continue
            hits += "${group.name}:$word"
            score += group.weight
        }

        // 보낸 사람
        val sender = msg.sender.replace(" ", "").replace("-", "")
        when {
            sender.startsWith("+") && !sender.startsWith("+82") -> { hits += "international"; score += 2 }
            FOREIGN_TAGS.any { body.contains(it) } -> { hits += "foreign_tag"; score += 2 }
        }
        if (sender.startsWith("070") || sender.startsWith("+8270")) {
            hits += "070"; score += 1
        }

        // 연락처에 있는 사람: 링크·돈 이야기가 없으면 정상
        if (msg.senderInContacts) {
            val risky = hits.any { it == "apk" || it == "short_url" || it == "url" || it.startsWith("money:") }
            if (!risky) return RuleResult(RuleResult.Level.SAFE, 0, hits)
            score -= 1
        }

        var level = when {
            score >= HIGH_RISK_SCORE -> RuleResult.Level.HIGH_RISK
            score >= SUSPICIOUS_SCORE -> RuleResult.Level.SUSPICIOUS
            else -> RuleResult.Level.SAFE
        }
        // 수신거부 안내가 있는 광고 문자는 서버 판정을 받기 전에는 경고하지 않는다
        if (level == RuleResult.Level.HIGH_RISK && AD_MARKER.containsMatchIn(body) && "apk" !in hits) {
            level = RuleResult.Level.SUSPICIOUS
            hits += "ad_marker"
        }
        return RuleResult(level, score, hits)
    }
}
