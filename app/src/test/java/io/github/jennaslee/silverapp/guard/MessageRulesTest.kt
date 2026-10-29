package io.github.jennaslee.silverapp.guard

import io.github.jennaslee.silverapp.guard.IncomingMessage.Source
import io.github.jennaslee.silverapp.guard.RuleResult.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageRulesTest {

    private fun check(body: String, sender: String = "01012345678", inContacts: Boolean = false) =
        MessageRules.check(IncomingMessage(Source.SMS, sender, body, inContacts))

    // 사기 문자 → 서버에 판정 요청 (SUSPICIOUS 이상)

    @Test
    fun `해외번호 택배 단축링크는 고위험`() {
        val r = check("[Web발신] 고객님 택배가 주소 불일치로 보류중입니다. 주소 확인 bit.ly/3xYzAbc", sender = "+447700900123")
        assertEquals(Level.HIGH_RISK, r.level)
        assertTrue("short_url" in r.hits)
        assertTrue("international" in r.hits)
    }

    @Test
    fun `apk 설치 유도는 고위험`() {
        val r = check("[서울중앙지검] 귀하 명의 계좌가 범죄에 연루. 앱 설치 후 확인 http://spo-check.example/app.apk")
        assertEquals(Level.HIGH_RISK, r.level)
    }

    @Test
    fun `가족 사칭 신분증 요구는 의심`() {
        val r = check("엄마 나 폰 액정 깨져서 임시폰으로 연락해. 신분증 사진 좀 보내줘")
        assertTrue(r.level >= Level.SUSPICIOUS)
    }

    @Test
    fun `070 해외결제 승인 문자는 의심`() {
        val r = check("[국외발신] 해외결제 승인 980,000원 본인 아닐 경우 문의", sender = "07012345678")
        assertTrue(r.level >= Level.SUSPICIOUS)
    }

    @Test
    fun `부고 링크는 의심`() {
        val r = check("[부고] 故 김OO님께서 별세하셨습니다. 장례식장 안내 https://bugo-notice.example/x7")
        assertTrue(r.level >= Level.SUSPICIOUS)
    }

    @Test
    fun `연락처에 있는 사람이라도 송금 요구는 의심`() {
        val r = check("엄마 나 급해서 그러는데 지금 이 계좌로 200만원만 보내줘", sender = "딸", inContacts = true)
        assertTrue(r.level >= Level.SUSPICIOUS)
    }

    // 데이터셋 평가에서 찾은 문제 (회귀 방지)

    @Test
    fun `줄바꿈이 있는 가짜 결제 승인 문자도 잡는다`() {
        val r = check("[Web발신]\n㈜카카오페이\n승인번호 607488\n868.000원 결제완료 되었습니다.\n070-8064-1374㈜카카오페이")
        assertTrue(r.level >= Level.SUSPICIOUS)
        assertTrue(r.hits.any { it.startsWith("payment_callback") })
    }

    @Test
    fun `가족 사칭 변형 표현을 잡는다`() {
        assertTrue(check("엄마 나 핸드폰 고장나서 수리맡기고 문자나라로 문자하는거야 문자보면 답줘").level >= Level.SUSPICIOUS)
        assertTrue(check("엄마 나 폰고장나서 이번호 0105810001 카톡추가하고 톡줘~").level >= Level.SUSPICIOUS)
        assertTrue(check("아빠~ 지금 구글기프트카드가 좀 필요한데 대신 먼저 구매해줄수 잇어?").level >= Level.SUSPICIOUS)
    }

    @Test
    fun `낯선 끝자리 주소와 문서 열람 유도를 잡는다`() {
        val r = check("[Web발신] (국민건강보험공단) 종합검진 진단서 발송완료 내용확인 : nnop.djv3.pet")
        assertTrue(r.level >= Level.SUSPICIOUS)
        assertTrue("url" in r.hits)
    }

    @Test
    fun `한글 바로 뒤에 붙은 주소와 번호도 잡는다`() {
        // \b 대신 앞뒤 글자를 직접 확인하도록 바꾼 부분 (안드로이드에서는 \b가 한글을 글자로 취급)
        assertTrue("short_url" in check("택배 보류 주소확인bit.ly/3xYz").hits)
        assertTrue(check("주문하신 냉장고 988,000원 결제완료 문의02-318-2714").hits.any { it.startsWith("payment_callback") })
        assertTrue("apk" in check("검찰청 사건조회 설치파일check.apk를 받으세요").hits)
    }

    @Test
    fun `통신사 사칭 주의 안내는 서버 판정 전에 경고하지 않는다`() {
        val r = check("[Web발신] [SKT] 정부 기관 및 가족 사칭 문자 주의 안내. 검찰을 사칭하며 신분증, 계좌 비밀번호를 요구하는 문자에 주의하세요.")
        assertTrue(r.level != Level.HIGH_RISK)
    }

    // 정상 문자 → 서버에 묻지 않음 (SAFE)

    @Test
    fun `인증번호 문자는 정상`() {
        val r = check("[Web발신] [OO은행] 인증번호 [482913]을 입력해주세요. 타인 노출 금지", sender = "15881234")
        assertEquals(Level.SAFE, r.level)
    }

    @Test
    fun `연락처에 있는 사람의 평범한 대화는 정상`() {
        val r = check("형님 이번 주 토요일 동창회 장소 바뀌었어요. 종로 OO식당 12시입니다", sender = "김철수", inContacts = true)
        assertEquals(Level.SAFE, r.level)
    }

    @Test
    fun `평범한 문자는 정상`() {
        val r = check("내일 점심 같이 먹어요")
        assertEquals(Level.SAFE, r.level)
    }

    @Test
    fun `링크 없는 택배 배송 예정 문자는 서버로 가지 않는다`() {
        val r = check("[CJ대한통운] 고객님의 상품이 오늘 14~16시 배송 예정입니다", sender = "15880000")
        assertEquals(Level.SAFE, r.level)
    }
}
