package io.github.jennaslee.silverapp.guard

/** 서버 판정 종류를 어르신이 알아듣는 말로 */
fun categoryLabel(category: String): String = when (category) {
    "delivery_scam" -> "택배 사칭"
    "gov_impersonation" -> "기관 사칭"
    "family_impersonation" -> "가족·지인 사칭"
    "payment_scam" -> "결제 사칭"
    "invitation_scam" -> "부고·청첩장 사칭"
    "loan" -> "대출 권유"
    "investment" -> "투자 권유"
    "rule" -> "의심 문자"
    else -> "의심 문자"
}

/** 인터넷 주소 판정 종류 */
fun urlCategoryLabel(category: String): String = when (category) {
    "phishing" -> "가짜 사이트"
    "malware" -> "위험한 프로그램"
    "scam" -> "사기 사이트"
    else -> "의심 주소"
}

fun sourceLabel(source: String): String = when (source) {
    "sms" -> "문자"
    "kakao" -> "카카오톡"
    else -> "메시지"
}
