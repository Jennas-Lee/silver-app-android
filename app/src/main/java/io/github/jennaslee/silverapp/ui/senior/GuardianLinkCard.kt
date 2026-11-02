package io.github.jennaslee.silverapp.ui.senior

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.jennaslee.silverapp.data.remote.ApiClient
import io.github.jennaslee.silverapp.data.remote.LinkedGuardian
import io.github.jennaslee.silverapp.ui.BigButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

data class LinkCodeUi(val code: String, val expiresAt: Instant)

class GuardianLinkViewModel : ViewModel() {
    private val _guardians = MutableStateFlow<List<LinkedGuardian>>(emptyList())
    val guardians: StateFlow<List<LinkedGuardian>> = _guardians

    private val _code = MutableStateFlow<LinkCodeUi?>(null)
    val code: StateFlow<LinkCodeUi?> = _code

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun refresh() {
        viewModelScope.launch {
            runCatching { ApiClient.api.links() }
                .onSuccess {
                    val before = _guardians.value.size
                    _guardians.value = it.guardians.orEmpty()
                    // 새 보호자가 연결되면 코드는 다 쓴 것이므로 닫는다
                    if (_guardians.value.size > before) _code.value = null
                }
        }
    }

    fun requestCode() {
        _error.value = null
        viewModelScope.launch {
            runCatching { ApiClient.api.linkCode() }
                .onSuccess { _code.value = LinkCodeUi(it.code, Instant.parse(it.expiresAt)) }
                .onFailure { _error.value = ApiClient.errorMessage(it) }
        }
    }

    fun closeCode() {
        _code.value = null
    }
}

/** 어르신 홈: 보호자 연결 카드. 코드를 받으면 크게 보여주고, 연결될 때까지 몇 초마다 확인한다. */
@Composable
fun GuardianLinkCard(vm: GuardianLinkViewModel = viewModel()) {
    val guardians by vm.guardians.collectAsStateWithLifecycle()
    val code by vm.code.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    // 코드를 보여주는 동안 연결되었는지 확인하고, 시간이 지나면 닫는다
    LaunchedEffect(code) {
        val c = code ?: return@LaunchedEffect
        while (Instant.now().isBefore(c.expiresAt)) {
            delay(5_000)
            vm.refresh()
        }
        vm.closeCode()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("보호자 연결", style = MaterialTheme.typography.titleLarge)
            if (guardians.isEmpty()) {
                Text("가족을 연결하면 걸러낸 사기 문자를 가족도 함께 볼 수 있어요.", style = MaterialTheme.typography.bodyLarge)
            } else {
                Text("연결된 가족: ${guardians.joinToString(", ") { it.name }}", style = MaterialTheme.typography.bodyLarge)
            }

            val c = code
            if (c != null) {
                Text("가족 휴대폰의 실버 도우미에 이 숫자를 넣어 주세요.", style = MaterialTheme.typography.bodyLarge)
                Text(
                    c.code.chunked(3).joinToString(" "),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                val minutes = Duration.between(Instant.now(), c.expiresAt).toMinutes().coerceAtLeast(1)
                Text("${minutes}분 안에 넣어 주세요.", style = MaterialTheme.typography.bodyMedium)
            } else {
                BigButton(if (guardians.isEmpty()) "보호자 연결하기" else "가족 더 연결하기", vm::requestCode)
            }
            error?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error) }
        }
    }
}
