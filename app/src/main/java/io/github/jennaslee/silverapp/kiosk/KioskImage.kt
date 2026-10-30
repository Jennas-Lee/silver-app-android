package io.github.jennaslee.silverapp.kiosk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** 서버로 보낼 사진: 화면에 보여줄 비트맵과 base64 JPEG를 함께 들고 다닌다 */
class KioskPhoto(val bitmap: Bitmap, val base64Jpeg: String)

object KioskImage {
    /** 서버 권장: 긴 변 1024px 이하 (Vercel 요청 크기 제한) */
    const val MAX_SIDE = 1024
    private const val JPEG_QUALITY = 80

    /** 긴 변이 maxSide를 넘으면 비율을 유지해 줄인 크기 */
    fun targetSize(width: Int, height: Int, maxSide: Int = MAX_SIDE): Pair<Int, Int> {
        val longest = max(width, height)
        if (longest <= maxSide) return width to height
        val scale = maxSide.toFloat() / longest
        return (width * scale).roundToInt() to (height * scale).roundToInt()
    }

    /** 카메라 사진을 바로 세우고(회전) 줄여서 서버로 보낼 형태로 만든다 */
    fun prepare(source: Bitmap, rotationDegrees: Int = 0): KioskPhoto {
        val rotated = if (rotationDegrees != 0) {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, Matrix().apply { postRotate(rotationDegrees.toFloat()) }, true)
        } else {
            source
        }
        val (w, h) = targetSize(rotated.width, rotated.height)
        val scaled = if (w != rotated.width) Bitmap.createScaledBitmap(rotated, w, h, true) else rotated
        val bytes = ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
        return KioskPhoto(scaled, Base64.encodeToString(bytes, Base64.NO_WRAP))
    }

    /** 디버그용: 저장된 사진을 불러온다 */
    fun fromUri(context: Context, uri: Uri): KioskPhoto? {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
        return prepare(bitmap)
    }
}
