package io.github.jennaslee.silverapp

import io.github.jennaslee.silverapp.data.remote.ApiClient
import io.github.jennaslee.silverapp.data.remote.HealthResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthResponseTest {
    @Test
    fun `health 응답을 파싱하고 모르는 필드는 무시한다`() {
        val body = """{"status":"ok","version":"a1b2c3d","time":"2026-10-28T09:00:00.000Z","extra":1}"""

        val parsed = ApiClient.json.decodeFromString(HealthResponse.serializer(), body)

        assertEquals("ok", parsed.status)
        assertEquals("a1b2c3d", parsed.version)
    }
}
