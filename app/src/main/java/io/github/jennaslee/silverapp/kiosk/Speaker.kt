package io.github.jennaslee.silverapp.kiosk

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * 안내를 소리 내어 읽어준다. 어르신이 알아듣기 쉽게 보통보다 조금 느리게 읽는다.
 * 엔진이 준비되기 전에 들어온 문장은 준비되면 바로 읽는다.
 */
class Speaker(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var pending: String? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "음성 읽기 엔진을 시작하지 못함: $status")
            return
        }
        val result = tts.setLanguage(Locale.KOREAN)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "한국어 음성이 설치되어 있지 않음")
        }
        tts.setSpeechRate(SPEECH_RATE)
        ready = true
        pending?.let { speak(it) }
        pending = null
    }

    fun speak(text: String) {
        if (!ready) {
            pending = text
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kiosk")
    }

    fun stop() {
        pending = null
        if (ready) tts.stop()
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    private companion object {
        const val TAG = "Speaker"
        const val SPEECH_RATE = 0.85f
    }
}
