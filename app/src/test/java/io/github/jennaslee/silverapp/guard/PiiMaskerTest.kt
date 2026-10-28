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
    fun `전화번호와 인증번호는 남긴다`() {
        val text = "문의 010-1234-5678, 1588-1234, 02-123-4567, 인증번호 482913"
        assertEquals(text, PiiMasker.mask(text))
        assertEquals("01012345678로 전화", PiiMasker.mask("01012345678로 전화"))
    }
}
