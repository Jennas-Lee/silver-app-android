package io.github.jennaslee.silverapp.kiosk

import io.github.jennaslee.silverapp.data.remote.ApiClient
import io.github.jennaslee.silverapp.data.remote.KioskGuideResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KioskImageTest {

    @Test
    fun `긴 변을 1024로 줄이고 비율을 유지한다`() {
        assertEquals(768 to 1024, KioskImage.targetSize(3024, 4032))
        assertEquals(1024 to 576, KioskImage.targetSize(1920, 1080))
    }

    @Test
    fun `이미 작은 사진은 그대로 둔다`() {
        assertEquals(800 to 600, KioskImage.targetSize(800, 600))
    }

    @Test
    fun `안내 응답을 파싱한다 (누를 곳이 없는 경우 포함)`() {
        val withTarget = ApiClient.json.decodeFromString<KioskGuideResponse>(
            """{"sessionId":"s1","speech":"버거를 눌러 주세요.","status":"in_progress","target":{"label":"버거","box":[0.1,0.2,0.3,0.4]}}""",
        )
        assertEquals(listOf(0.1f, 0.2f, 0.3f, 0.4f), withTarget.target?.box)

        val noTarget = ApiClient.json.decodeFromString<KioskGuideResponse>(
            """{"sessionId":"s1","speech":"다시 비춰 주세요.","status":"unclear","target":null}""",
        )
        assertNull(noTarget.target)
    }
}
