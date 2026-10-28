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
