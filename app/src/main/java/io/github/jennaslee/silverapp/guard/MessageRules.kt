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
 * 점수가 높을수록 위험. 기준값은 실제 문자로 시험하면서 조정한다.
 */
object MessageRules {

    private const val SUSPICIOUS_SCORE = 2
    private const val HIGH_RISK_SCORE = 5

    private val URL = Regex(
        """(?i)\bhttps?://\S+|\bwww\.\S+|\b[a-z0-9-]+(\.[a-z0-9-]+)*\.(com|net|org|kr|co\.kr|me|do|ly|gl|io|xyz|top|cc|link|site|shop|info|example)(/\S*)?""",
    )
    private val SHORT_URL_HOSTS = listOf(
        "bit.ly", "me2.do", "han.gl", "vo.la", "url.kr", "t.ly", "tinyurl.com", "goo.gl", "is.gd", "buly.kr", "c11.kr", "abit.ly", "t.co",
    )
    private val APK = Regex("""(?i)\.apk\b""")

    /** 인증번호만 알려주는 문자: 링크가 없으면 정상으로 본다 */
    private val OTP = Regex("""(인증\s*번호|인증\s*코드|확인\s*코드|verification code|OTP)[^0-9]{0,15}\d{4,8}""", RegexOption.IGNORE_CASE)

    private val KEYWORD_GROUPS: Map<String, List<String>> = mapOf(
        "delivery" to listOf("택배", "배송", "주소 불일치", "주소불일치", "운송장", "관세", "반송"),
        "gov" to listOf("검찰", "지검", "경찰", "법원", "수사", "금감원", "금융감독원", "건강보험", "국민연금", "환급", "과태료", "범칙금", "벌점", "무단투기"),
        "family" to listOf("액정", "폰 고장", "폰이 고장", "임시폰", "엄마 나", "아빠 나", "엄마나", "아빠나"),
        "personal_info" to listOf("신분증", "카드 앞뒤", "카드 사진", "비밀번호", "인증번호 알려", "인증번호 보내", "원격"),
        "payment" to listOf("해외결제", "해외 결제", "결제 승인", "승인 완료", "결제완료", "본인 아닐", "본인이 아닐"),
        "invitation" to listOf("부고", "별세", "청첩장", "모바일 초대장", "돌잔치"),
        "loan" to listOf("대출", "대환", "저금리", "한도 조회", "정부지원"),
        "investment" to listOf("리딩방", "수익률", "급등", "원금 보장", "원금보장", "코인", "고수익"),
        "money" to listOf("계좌", "송금", "이체", "입금", "현금", "상품권"),
        "urgency" to listOf("급해", "급하게", "즉시", "오늘까지", "긴급", "24시간 이내"),
    )

    private val FOREIGN_TAGS = listOf("[국외발신]", "[국제발신]", "[해외발신]")

    fun check(msg: IncomingMessage): RuleResult {
        val body = msg.body
        val hits = mutableListOf<String>()
        var score = 0

        // 링크
        val urls = URL.findAll(body).map { it.value.lowercase() }.toList()
        val hasShortUrl = urls.any { u -> SHORT_URL_HOSTS.any { u.contains(it) } }
        when {
            APK.containsMatchIn(body) -> { hits += "apk"; score += 5 }
            hasShortUrl -> { hits += "short_url"; score += 3 }
            urls.isNotEmpty() -> { hits += "url"; score += 2 }
        }

        // 인증번호 문자는 링크가 없으면 정상
        if (urls.isEmpty() && OTP.containsMatchIn(body)) {
            return RuleResult(RuleResult.Level.SAFE, 0, listOf("otp"))
        }

        // 의심 낱말 (종류별로 한 번씩만)
        for ((group, words) in KEYWORD_GROUPS) {
            val word = words.firstOrNull { body.contains(it) } ?: continue
            hits += "keyword:$word"
            // 돈과 개인정보를 요구하는 말은 링크가 없어도 위험 신호가 크다
            score += if (group == "money" || group == "personal_info") 2 else 1
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
            val risky = hits.any { it == "apk" || it == "short_url" || it == "url" } ||
                KEYWORD_GROUPS.getValue("money").any { body.contains(it) }
            if (!risky) return RuleResult(RuleResult.Level.SAFE, 0, hits)
            score -= 1
        }

        val level = when {
            score >= HIGH_RISK_SCORE -> RuleResult.Level.HIGH_RISK
            score >= SUSPICIOUS_SCORE -> RuleResult.Level.SUSPICIOUS
            else -> RuleResult.Level.SAFE
        }
        return RuleResult(level, score, hits)
    }
}
