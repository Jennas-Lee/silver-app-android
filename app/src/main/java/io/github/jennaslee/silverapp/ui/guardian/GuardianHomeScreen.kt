package io.github.jennaslee.silverapp.ui.guardian

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jennaslee.silverapp.data.remote.LinkedSenior
import io.github.jennaslee.silverapp.session.SessionStore
import io.github.jennaslee.silverapp.ui.BigButton
import io.github.jennaslee.silverapp.ui.formatTime
import java.time.Instant

/** 보호자 홈: 연결한 어르신 목록. 문자 지킴이·키오스크 기능은 없다. */
@Composable
fun GuardianHomeScreen(vm: GuardianViewModel, onOpenSenior: (LinkedSenior) -> Unit) {
    val seniors by vm.seniors.collectAsStateWithLifecycle()
    val linkResult by vm.linkResult.collectAsStateWithLifecycle()
    val linking by vm.linking.collectAsStateWithLifecycle()
    var addingSenior by remember { mutableStateOf(false) }
    val name = SessionStore.current?.name.orEmpty()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.loadSeniors() }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Spacer(Modifier.heightIn(min = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("실버 도우미", style = MaterialTheme.typography.headlineLarge)
                        Text("$name 님 (보호자)", style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = vm::logout, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("로그아웃", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                if (addingSenior) {
                    LinkSeniorCard(
                        linking = linking,
                        message = linkResult,
                        onSubmit = { code -> vm.link(code) { addingSenior = false } },
                        onCancel = { addingSenior = false; vm.clearLinkResult() },
                    )
                } else {
                    BigButton("어르신 연결하기") { addingSenior = true; vm.clearLinkResult() }
                    linkResult?.let { Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp)) }
                }
            }

            item { Text("연결한 어르신", style = MaterialTheme.typography.headlineMedium) }

            when (val s = seniors) {
                Loadable.Loading -> item { CircularProgressIndicator() }
                is Loadable.Failed -> item {
                    Text(s.message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = vm::loadSeniors, modifier = Modifier.padding(top = 8.dp)) { Text("다시 불러오기") }
                }
                is Loadable.Ready -> if (s.value.isEmpty()) {
                    item {
                        Text(
                            "아직 연결한 어르신이 없어요.\n어르신 휴대폰의 실버 도우미에서 \"보호자 연결하기\"를 누르면 숫자 6자리가 나와요. 그 숫자를 위의 \"어르신 연결하기\"에 넣어 주세요.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                } else {
                    items(s.value, key = { it.id }) { senior -> SeniorCard(senior) { onOpenSenior(senior) } }
                }
            }

            item { Spacer(Modifier.heightIn(min = 24.dp)) }
        }
    }
}

@Composable
private fun LinkSeniorCard(linking: Boolean, message: String?, onSubmit: (String) -> Unit, onCancel: () -> Unit) {
    var code by remember { mutableStateOf("") }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("어르신 연결하기", style = MaterialTheme.typography.titleLarge)
            Text("어르신 휴대폰에 나온 숫자 6자리를 넣어 주세요.", style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = code,
                onValueChange = { v -> code = v.filter { it.isDigit() }.take(6) },
                label = { Text("숫자 6자리") },
                textStyle = MaterialTheme.typography.headlineMedium,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            message?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error) }
            BigButton(if (linking) "연결하는 중…" else "연결하기") { if (!linking) onSubmit(code) }
            TextButton(onClick = onCancel) { Text("취소", style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Composable
private fun SeniorCard(senior: LinkedSenior, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${senior.name} 님", style = MaterialTheme.typography.titleLarge)
            Text(
                if (senior.recentCount > 0) "최근 7일 동안 사기 의심 문자 ${senior.recentCount}건" else "최근 7일 동안 걸러낸 문자가 없어요",
                style = MaterialTheme.typography.bodyLarge,
                color = if (senior.recentCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            if (senior.recentUrlCount > 0) {
                Text(
                    "최근 7일 동안 위험한 링크 ${senior.recentUrlCount}건",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            senior.lastRecordAt?.let {
                Text("마지막: ${formatTime(Instant.parse(it).toEpochMilli())}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
