package io.github.jennaslee.silverapp.guard

/**
 * 서버로 보내기 전에 개인정보를 가린다.
 * 전화번호와 인증번호는 사기 판단에 필요하므로 남긴다.
 */
object PiiMasker {

    // 숫자 경계는 \b 대신 "앞뒤에 숫자가 붙어 있지 않음"으로 확인한다.
    // \b는 안드로이드에서 한글도 글자로 취급해서, "110-123-456789로"처럼 조사가 바로 붙으면 맞지 않는다.
    private val RRN = Regex("""(?<!\d)\d{6}\s?-\s?[1-4]\d{6}(?!\d)""")
    private val CARD = Regex("""(?<!\d)\d{4}[- ]\d{4}[- ]\d{4}[- ]\d{4}(?!\d)|(?<!\d)\d{16}(?!\d)""")

    // 숫자-숫자-숫자 꼴. 전화번호 모양이 아니면 계좌번호로 본다.
    private val DASHED_NUMBER = Regex("""(?<![\d-])\d{2,6}-\d{2,6}-\d{2,8}(-\d{1,3})?(?![\d-])""")
    private val PHONE = Regex("""^(0\d{1,2}-\d{3,4}-\d{4}|1\d{3}-\d{4})$""")

    // 하이픈 없는 10~14자리 숫자 중 전화번호가 아닌 것
    private val LONG_NUMBER = Regex("""(?<!\d)\d{10,14}(?!\d)""")
    private val PHONE_PLAIN = Regex("""^(01\d{8,9}|0[2-6]\d{7,9}|070\d{8}|1[5-9]\d{6})$""")

    fun mask(text: String): String {
        var out = RRN.replace(text, "[주민번호]")
        out = CARD.replace(out, "[카드번호]")
        out = DASHED_NUMBER.replace(out) { m -> if (PHONE.matches(m.value)) m.value else "[계좌번호]" }
        out = LONG_NUMBER.replace(out) { m -> if (PHONE_PLAIN.matches(m.value)) m.value else "[계좌번호]" }
        return out
    }
}
