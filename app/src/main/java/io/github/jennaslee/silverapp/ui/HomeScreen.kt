package io.github.jennaslee.silverapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.jennaslee.silverapp.BuildConfig
import io.github.jennaslee.silverapp.data.remote.ApiClient
import io.github.jennaslee.silverapp.ui.theme.SilverTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ServerState {
    data object Checking : ServerState
    data class Ok(val version: String) : ServerState
    data class Error(val message: String) : ServerState
}

class HomeViewModel : ViewModel() {
    private val _server = MutableStateFlow<ServerState>(ServerState.Checking)
    val server: StateFlow<ServerState> = _server

    init {
        checkServer()
    }

    fun checkServer() {
        _server.value = ServerState.Checking
        viewModelScope.launch {
            _server.value = runCatching { ApiClient.api.health() }
                .fold(
                    onSuccess = { ServerState.Ok(it.version) },
                    onFailure = { ServerState.Error(it.message ?: "알 수 없는 오류") },
                )
        }
    }
}

/** 0단계 홈 화면: 서버 연결 상태만 보여준다. 기능 화면은 다음 단계에서 추가. */
@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val server by vm.server.collectAsStateWithLifecycle()
    HomeContent(server = server, onRetry = vm::checkServer)
}

@Composable
private fun HomeContent(server: ServerState, onRetry: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("실버 도우미", style = MaterialTheme.typography.headlineLarge)

            val (text, isError) = when (server) {
                ServerState.Checking -> "서버에 연결하는 중이에요…" to false
                is ServerState.Ok -> "서버와 연결됐어요 (${server.version})" to false
                is ServerState.Error -> "서버에 연결하지 못했어요" to true
            }
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
            )
            if (server is ServerState.Error) {
                Text(server.message, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
            ) {
                Text("다시 확인", style = MaterialTheme.typography.labelLarge)
            }

            Text(
                "서버 주소: ${BuildConfig.API_BASE_URL}\n버전: ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    SilverTheme { HomeContent(server = ServerState.Ok("a1b2c3d"), onRetry = {}) }
}
