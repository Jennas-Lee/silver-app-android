package io.github.jennaslee.silverapp.ui.guardian

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jennaslee.silverapp.data.remote.ApiClient
import io.github.jennaslee.silverapp.data.remote.LinkRequest
import io.github.jennaslee.silverapp.data.remote.LinkedSenior
import io.github.jennaslee.silverapp.data.remote.RecordDto
import io.github.jennaslee.silverapp.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** 불러오기 상태 */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Ready<T>(val value: T) : Loadable<T>
    data class Failed(val message: String) : Loadable<Nothing>
}

class GuardianViewModel : ViewModel() {
    private val _seniors = MutableStateFlow<Loadable<List<LinkedSenior>>>(Loadable.Loading)
    val seniors: StateFlow<Loadable<List<LinkedSenior>>> = _seniors

    private val _records = MutableStateFlow<Loadable<List<RecordDto>>>(Loadable.Loading)
    val records: StateFlow<Loadable<List<RecordDto>>> = _records

    private val _linkResult = MutableStateFlow<String?>(null)
    /** 연결 시도 결과 메시지 (성공·실패 모두) */
    val linkResult: StateFlow<String?> = _linkResult

    private val _linking = MutableStateFlow(false)
    val linking: StateFlow<Boolean> = _linking

    fun loadSeniors() {
        viewModelScope.launch {
            if (_seniors.value !is Loadable.Ready) _seniors.value = Loadable.Loading
            runCatching { ApiClient.api.links() }
                .onSuccess { _seniors.value = Loadable.Ready(it.seniors.orEmpty()) }
                .onFailure { _seniors.value = Loadable.Failed(ApiClient.errorMessage(it)) }
        }
    }

    fun link(code: String, onLinked: () -> Unit) {
        val digits = code.filter { it.isDigit() }
        if (digits.length != 6) {
            _linkResult.value = "숫자 6자리를 입력해 주세요."
            return
        }
        _linking.value = true
        _linkResult.value = null
        viewModelScope.launch {
            runCatching { ApiClient.api.link(LinkRequest(digits)) }
                .onSuccess {
                    _linkResult.value = "${it.senior.name} 님과 연결됐어요."
                    loadSeniors()
                    onLinked()
                }
                .onFailure { _linkResult.value = ApiClient.errorMessage(it) }
            _linking.value = false
        }
    }

    fun clearLinkResult() {
        _linkResult.value = null
    }

    fun unlink(seniorId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { ApiClient.api.unlink(seniorId) }
            loadSeniors()
            onDone()
        }
    }

    fun loadRecords(seniorId: String) {
        _records.value = Loadable.Loading
        viewModelScope.launch {
            runCatching { ApiClient.api.seniorRecords(seniorId) }
                .onSuccess { _records.value = Loadable.Ready(it.records) }
                .onFailure { _records.value = Loadable.Failed(ApiClient.errorMessage(it)) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { ApiClient.api.logout() }
            SessionStore.clear()
        }
    }
}
