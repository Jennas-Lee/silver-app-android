package io.github.jennaslee.silverapp.ui.senior

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.jennaslee.silverapp.ui.BigButton

/**
 * 보호자와 연결되지 않은 어르신에게 보여주는 첫 화면. 연결하기 전에는 다른 기능을 쓸 수 없다.
 * 들어오면 바로 연결 숫자를 받아 크게 보여주고, 가족이 숫자를 넣으면 저절로 첫 화면으로 넘어간다.
 */
@Composable
fun LinkRequiredScreen(vm: GuardianLinkViewModel = viewModel()) {
    val code by vm.code.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
    LaunchedEffect(Unit) { vm.requestCode() }
    WatchLinkCode(vm, code)

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.heightIn(min = 8.dp))
            Text("가족과 연결해 주세요", style = MaterialTheme.typography.headlineLarge)
            Text(
                "실버 도우미는 가족과 연결해야 쓸 수 있어요.\n연결하면 가족도 사기 문자와 위험한 주소를 함께 살펴 드려요.",
                style = MaterialTheme.typography.bodyLarge,
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val c = code
                    if (c != null) {
                        LinkCodeBox(c)
                        Text("가족이 숫자를 넣으면 저절로 다음 화면으로 넘어가요.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("아래 단추를 누르면 가족에게 알려 줄 숫자가 나와요.", style = MaterialTheme.typography.bodyLarge)
                        BigButton("보호자 연결하기", vm::requestCode)
                    }
                    error?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error) }
                }
            }

            Text("가족은 이렇게 해 주세요", style = MaterialTheme.typography.titleLarge)
            Text(
                "1. 가족 휴대폰에 실버 도우미를 깔아요.\n" +
                    "2. '보호자'로 가입해요.\n" +
                    "3. '어르신 연결하기'를 누르고 위의 숫자 6자리를 넣어요.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.heightIn(min = 24.dp))
        }
    }
}
