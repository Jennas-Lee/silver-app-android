package io.github.jennaslee.silverapp.kiosk

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.jennaslee.silverapp.data.remote.ApiClient
import io.github.jennaslee.silverapp.data.remote.KioskGuideRequest
import io.github.jennaslee.silverapp.data.remote.KioskGuideResponse
import io.github.jennaslee.silverapp.data.remote.KioskHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** 키오스크 도우미 화면의 단계 */
sealed interface KioskStep {
    /** ① 무엇을 하고 싶은지 묻기 */
    data object AskGoal : KioskStep

    /** ② 화면 찍기. hint는 직전 안내 (다음 화면을 찍을 때 다시 보여줌) */
    data class Camera(val hint: String?) : KioskStep

    /** ③ 서버가 사진을 살펴보는 중 */
    data class Thinking(val photo: KioskPhoto) : KioskStep

    /** ④ 안내 결과 */
    data class Answer(val photo: KioskPhoto, val result: KioskGuideResponse) : KioskStep

    /** 문제가 생김 */
    data class Failed(val message: String, val photo: KioskPhoto?) : KioskStep
}

class KioskViewModel(app: Application) : AndroidViewModel(app) {
    private val _step = MutableStateFlow<KioskStep>(KioskStep.AskGoal)
    val step: StateFlow<KioskStep> = _step

    private val _goal = MutableStateFlow("")
    val goal: StateFlow<String> = _goal

    private val speaker = Speaker(app)
    private var sessionId: String? = null
    private val history = mutableListOf<KioskHistoryItem>()
    private var lastPhoto: KioskPhoto? = null

    fun setGoal(text: String) {
        val goal = text.trim()
        if (goal.isEmpty()) return
        _goal.value = goal
        sessionId = null
        history.clear()
        _step.value = KioskStep.Camera(hint = null)
        speaker.speak("주문하는 화면 전체가 보이게 비추고, 아래 큰 버튼을 눌러 주세요.")
    }

    /** 찍은 사진으로 다음 안내를 받는다 */
    fun onPhoto(photo: KioskPhoto) {
        lastPhoto = photo
        request(photo, userReply = null)
    }

    /** "세트로 하실래요?" 같은 질문에 대한 어르신의 대답을 보내고, 같은 사진으로 다시 묻는다 */
    fun reply(text: String) {
        val photo = lastPhoto ?: return
        if (text.isBlank()) return
        request(photo, userReply = text.trim())
    }

    /** 안내대로 눌렀으니 다음 화면을 찍는다 */
    fun next() {
        speaker.stop()
        val hint = (step.value as? KioskStep.Answer)?.result?.speech
        _step.value = KioskStep.Camera(hint = hint)
        speaker.speak("바뀐 화면을 다시 비추고 큰 버튼을 눌러 주세요.")
    }

    fun retake() {
        speaker.stop()
        _step.value = KioskStep.Camera(hint = null)
    }

    fun replay() {
        (step.value as? KioskStep.Answer)?.let { speaker.speak(it.result.speech) }
    }

    fun restart() {
        speaker.stop()
        sessionId = null
        history.clear()
        lastPhoto = null
        _goal.value = ""
        _step.value = KioskStep.AskGoal
    }

    fun stopSpeaking() = speaker.stop()

    private fun request(photo: KioskPhoto, userReply: String?) {
        speaker.stop()
        _step.value = KioskStep.Thinking(photo)
        viewModelScope.launch {
            runCatching {
                ApiClient.api.kioskGuide(
                    KioskGuideRequest(
                        sessionId = sessionId,
                        goal = _goal.value,
                        image = photo.base64Jpeg,
                        history = history.toList(),
                        userReply = userReply,
                    ),
                )
            }.onSuccess { res ->
                sessionId = res.sessionId
                history += KioskHistoryItem(res.speech)
                _step.value = KioskStep.Answer(photo, res)
                speaker.speak(res.speech)
            }.onFailure { e ->
                Log.w(TAG, "안내 요청 실패", e)
                val message = when ((e as? HttpException)?.code()) {
                    413 -> "사진이 너무 커요. 다시 찍어 주세요."
                    429 -> "지금 요청이 많아요. 잠시 뒤에 다시 해 주세요."
                    422 -> "이 화면은 안내해 드릴 수 없어요."
                    else -> "안내를 받지 못했어요. 인터넷 연결을 확인하고 다시 해 주세요."
                }
                _step.value = KioskStep.Failed(message, photo)
                speaker.speak(message)
            }
        }
    }

    override fun onCleared() {
        speaker.shutdown()
        super.onCleared()
    }

    private companion object {
        const val TAG = "KioskViewModel"
    }
}
