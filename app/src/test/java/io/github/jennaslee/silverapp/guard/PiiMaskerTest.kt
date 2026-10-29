package io.github.jennaslee.silverapp.guard

import org.junit.Assert.assertEquals
import org.junit.Test

class PiiMaskerTest {

    @Test
    fun `주민번호를 가린다`() {
        assertEquals("주민번호 [주민번호] 확인", PiiMasker.mask("주민번호 900101-1234567 확인"))
    }

    @Test
    fun `카드번호를 가린다`() {
        assertEquals("카드 [카드번호] 승인", PiiMasker.mask("카드 1234-5678-9012-3456 승인"))
    }

    @Test
    fun `계좌번호를 가린다`() {
        assertEquals("[계좌번호]로 보내줘", PiiMasker.mask("110-123-456789로 보내줘"))
        assertEquals("[계좌번호]로 보내줘", PiiMasker.mask("3333012345678로 보내줘"))
    }

    @Test
    fun `숫자 바로 앞뒤에 한글이 붙어도 가린다`() {
        // 안드로이드의 \b는 한글을 글자로 취급해서 이런 경우를 놓쳤다 (회귀 방지)
        assertEquals("주민번호는[주민번호]이고", PiiMasker.mask("주민번호는900101-1234567이고"))
        assertEquals("카드[카드번호]로", PiiMasker.mask("카드1234-5678-9012-3456로"))
        assertEquals("계좌[계좌번호]에", PiiMasker.mask("계좌110-123-456789에"))
    }

    @Test
    fun `전화번호와 인증번호는 남긴다`() {
        val text = "문의 010-1234-5678, 1588-1234, 02-123-4567, 인증번호 482913"
        assertEquals(text, PiiMasker.mask(text))
        assertEquals("01012345678로 전화", PiiMasker.mask("01012345678로 전화"))
    }
}
