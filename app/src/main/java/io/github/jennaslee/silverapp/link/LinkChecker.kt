package io.github.jennaslee.silverapp.link

import android.util.Log
import io.github.jennaslee.silverapp.data.remote.ApiClient
import io.github.jennaslee.silverapp.data.remote.UrlCheckRequest
import io.github.jennaslee.silverapp.data.remote.UrlCheckResult

/**
 * 주소가 위험한지 서버에 묻는다. 같은 곳을 다시 열 때 기다리지 않도록 휴대폰 안에 잠시 기억한다.
 * 서버에는 ? 뒤를 버린 주소(도메인 + 경로)만 보낸다.
 */
object LinkChecker {
    private const val TAG = "LinkChecker"
    private const val MAX_ENTRIES = 300
    private const val MINUTE = 60_000L

    /** 여러 사람이 같이 쓰는 도메인: 경로까지 따로 기억한다 (서버의 isSharedHost와 비슷하게) */
    private val SHARED_HOSTS = setOf(
        "sites.google.com", "docs.google.com", "drive.google.com", "storage.googleapis.com", "forms.gle",
        "blog.naver.com", "m.blog.naver.com", "cafe.naver.com", "m.cafe.naver.com", "naver.me",
        "open.kakao.com", "pf.kakao.com", "kko.to", "bit.ly", "me2.do", "han.gl", "vo.la", "url.kr", "t.ly",
        "tinyurl.com", "is.gd", "buly.kr", "c11.kr", "goo.gl", "t.co", "cutt.ly", "shorturl.at", "rb.gy",
    )

    private data class Entry(val result: UrlCheckResult, val expiresAt: Long)

    private val cache = object : LinkedHashMap<String, Entry>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?) = size > MAX_ENTRIES
    }

    private fun cacheKey(key: String): String {
        val host = UrlExtractor.hostOf(key)
        return if (host in SHARED_HOSTS) key else host
    }

    /** 판정 결과. 서버에 연결하지 못하면 null (막지 않음) */
    suspend fun check(key: String): UrlCheckResult? {
        val ck = cacheKey(key)
        synchronized(cache) {
            cache[ck]?.let { if (it.expiresAt > System.currentTimeMillis()) return it.result }
        }
        val result = runCatching { ApiClient.api.checkUrls(UrlCheckRequest(listOf(key))).results.firstOrNull() }
            .onFailure { Log.w(TAG, "주소 판정 실패: ${it.message}") }
            .getOrNull() ?: return null
        val ttl = when (result.verdict) {
            "malicious" -> 24 * 60 * MINUTE
            "safe", "suspicious" -> 60 * MINUTE
            else -> MINUTE // unknown: 곧 다시 물어본다
        }
        synchronized(cache) { cache[ck] = Entry(result, System.currentTimeMillis() + ttl) }
        return result
    }
}
