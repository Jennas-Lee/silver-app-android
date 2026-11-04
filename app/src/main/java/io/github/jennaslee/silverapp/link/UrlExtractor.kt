package io.github.jennaslee.silverapp.link

/** 화면에서 읽은 글자 한 칸 */
data class ScreenText(
    /** 예: com.android.chrome:id/url_bar */
    val viewId: String?,
    val text: String,
    /** 입력 중인 칸 (주소를 치는 중이면 판정하지 않음) */
    val focused: Boolean,
    /** 화면 위쪽에서 떨어진 거리(px) */
    val top: Int,
)

/** 인터넷 창 화면에서 지금 열린 주소를 찾는다. 안드로이드 기능을 쓰지 않아 단위 시험이 가능하다. */
object UrlExtractor {
    /** 주소창 ID를 알고 있는 인터넷 창 */
    private val URL_BAR_IDS = mapOf(
        "com.android.chrome" to listOf("url_bar"),
        "com.sec.android.app.sbrowser" to listOf("location_bar_edit_text", "custom_tab_toolbar_url_bar_text"),
        "com.naver.whale" to listOf("url_bar"),
        "com.microsoft.emmx" to listOf("url_bar"),
        "com.brave.browser" to listOf("url_bar"),
        "org.mozilla.firefox" to listOf("mozac_browser_toolbar_url_view"),
    )

    /** 인터넷 창 전용 앱: 화면 위쪽의 주소처럼 생긴 글자도 주소창으로 본다 */
    val BROWSERS: Set<String> = URL_BAR_IDS.keys + setOf("com.opera.browser", "com.duckduckgo.mobile.android")

    /** 앱 안에서 인터넷 화면을 여는 앱: 인터넷 화면일 때만 위쪽 글자를 본다 (대화 내용을 주소로 착각하지 않게) */
    val IN_APP_BROWSERS: Set<String> = setOf("com.kakao.talk", "com.nhn.android.search", "com.google.android.googlequicksearchbox")

    /** 화면 위쪽 이 비율 안에 있는 글자만 주소창 후보로 본다 */
    private const val TOP_RATIO = 0.2

    private val URL_LIKE = Regex(
        "^(https?://)?" +
            "(([a-z0-9]([a-z0-9-]*[a-z0-9])?\\.)+[a-z]{2,24}|\\d{1,3}(\\.\\d{1,3}){3})" +
            "(:\\d{1,5})?([/?#]\\S*)?$",
        RegexOption.IGNORE_CASE,
    )

    /** 주소처럼 생긴 글자인지 (띄어쓰기 없이 도메인 형태) */
    fun looksLikeUrl(text: String): Boolean {
        val t = text.trim()
        return t.length in 4..2000 && URL_LIKE.matches(t)
    }

    /** 이 앱의 이 화면에서 위쪽 글자로 주소를 찾아도 되는지 */
    fun allowHeuristic(pkg: String, windowClass: String?): Boolean = when (pkg) {
        in BROWSERS -> true
        in IN_APP_BROWSERS -> windowClass != null && Regex("browser|webview|web", RegexOption.IGNORE_CASE).containsMatchIn(windowClass)
        else -> false
    }

    /** 지금 열린 주소. 없거나 주소를 입력하는 중이면 null */
    fun find(pkg: String, texts: List<ScreenText>, screenHeight: Int, allowHeuristic: Boolean): String? {
        val ids = URL_BAR_IDS[pkg].orEmpty().map { "$pkg:id/$it" }
        val bar = texts.firstOrNull { it.viewId in ids }
        if (bar != null) {
            if (bar.focused) return null
            return bar.text.trim().takeIf(::looksLikeUrl)
        }
        if (!allowHeuristic) return null
        val limit = (screenHeight * TOP_RATIO).toInt()
        val candidate = texts
            .filter { it.top in 0..limit && looksLikeUrl(it.text) }
            .minByOrNull { it.top }
            ?: return null
        return if (candidate.focused) null else candidate.text.trim()
    }

    /**
     * 판정·저장용으로 주소를 줄인다: 소문자 도메인 + 경로. http://, ? 뒤(검색어·로그인 정보), # 뒤는 버린다.
     * 예: "https://Example.com/a/b?x=1" → "example.com/a/b"
     */
    fun cleanKey(url: String): String? {
        var t = url.trim().replace(Regex("^[a-z][a-z0-9+.-]*://", RegexOption.IGNORE_CASE), "")
        t = t.substringBefore('?').substringBefore('#')
        val host = t.substringBefore('/').substringAfterLast('@').lowercase().trimEnd('.')
        if (host.isEmpty() || !looksLikeUrl(host)) return null
        val path = t.substringAfter('/', "").trimEnd('/')
        return if (path.isEmpty()) host else "$host/$path"
    }

    /** 줄인 주소에서 도메인만 (포트 제외) */
    fun hostOf(key: String): String = key.substringBefore('/').substringBefore(':')
}
