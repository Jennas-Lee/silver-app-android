package io.github.jennaslee.silverapp.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlExtractorTest {
    private val chrome = "com.android.chrome"

    @Test
    fun `주소처럼 생긴 글자를 알아본다`() {
        assertTrue(UrlExtractor.looksLikeUrl("naver.com"))
        assertTrue(UrlExtractor.looksLikeUrl("https://kbstar-login.top/auth?x=1"))
        assertTrue(UrlExtractor.looksLikeUrl("192.168.0.1/login"))
        assertFalse(UrlExtractor.looksLikeUrl("검색어 입력"))
        assertFalse(UrlExtractor.looksLikeUrl("hello world.com"))
        assertFalse(UrlExtractor.looksLikeUrl("3.14"))
    }

    @Test
    fun `크롬 주소창에서 주소를 읽는다`() {
        val texts = listOf(
            ScreenText("$chrome:id/url_bar", "kbstar-login.top/auth", focused = false, top = 100),
            ScreenText(null, "naver.com", focused = false, top = 50),
        )
        assertEquals("kbstar-login.top/auth", UrlExtractor.find(chrome, texts, 2400, allowHeuristic = true))
    }

    @Test
    fun `주소를 입력하는 중이면 판정하지 않는다`() {
        val texts = listOf(ScreenText("$chrome:id/url_bar", "kbstar", focused = true, top = 100))
        assertNull(UrlExtractor.find(chrome, texts, 2400, allowHeuristic = true))
    }

    @Test
    fun `주소창 ID를 모르면 화면 위쪽의 주소를 쓴다`() {
        val texts = listOf(
            ScreenText(null, "가짜 은행", focused = false, top = 80),
            ScreenText(null, "fake-bank.xyz", focused = false, top = 150),
            ScreenText(null, "other-site.com", focused = false, top = 1200),
        )
        assertEquals("fake-bank.xyz", UrlExtractor.find("com.opera.browser", texts, 2400, allowHeuristic = true))
    }

    @Test
    fun `화면 아래쪽 글자나 허용되지 않은 화면은 보지 않는다`() {
        val texts = listOf(ScreenText(null, "fake-bank.xyz", focused = false, top = 1200))
        assertNull(UrlExtractor.find("com.opera.browser", texts, 2400, allowHeuristic = true))
        val top = listOf(ScreenText(null, "fake-bank.xyz", focused = false, top = 100))
        assertNull(UrlExtractor.find("com.kakao.talk", top, 2400, allowHeuristic = false))
    }

    @Test
    fun `카카오톡은 인터넷 화면일 때만 위쪽 글자를 본다`() {
        assertTrue(UrlExtractor.allowHeuristic("com.kakao.talk", "com.kakao.talk.activity.browser.InAppBrowserActivity"))
        assertFalse(UrlExtractor.allowHeuristic("com.kakao.talk", "com.kakao.talk.activity.chatroom.ChatRoomActivity"))
        assertTrue(UrlExtractor.allowHeuristic(chrome, null))
        assertFalse(UrlExtractor.allowHeuristic("com.example.game", "Main"))
    }

    @Test
    fun `주소를 줄일 때 검색어와 개인정보가 든 뒷부분을 버린다`() {
        assertEquals("example.com/a/b", UrlExtractor.cleanKey("https://Example.com/a/b/?name=kim#top"))
        assertEquals("naver.com", UrlExtractor.cleanKey("naver.com/"))
        assertEquals("evil.top/login", UrlExtractor.cleanKey("http://naver.com@evil.top/login"))
        assertNull(UrlExtractor.cleanKey("검색어"))
        assertEquals("evil.top", UrlExtractor.hostOf("evil.top/login"))
    }
}
