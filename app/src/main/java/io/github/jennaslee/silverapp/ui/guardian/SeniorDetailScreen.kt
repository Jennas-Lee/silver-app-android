package io.github.jennaslee.silverapp.ui.guardian

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jennaslee.silverapp.data.remote.LinkedSenior
import io.github.jennaslee.silverapp.data.remote.RecordDto
import io.github.jennaslee.silverapp.data.remote.UrlRecordDto
import io.github.jennaslee.silverapp.guard.categoryLabel
import io.github.jennaslee.silverapp.guard.sourceLabel
import io.github.jennaslee.silverapp.guard.urlCategoryLabel
import io.github.jennaslee.silverapp.ui.formatTime
import java.time.Instant

/** 보호자: 어르신 한 분의 차단된 문자 / 막은 링크 / 보이스피싱 내역 */
@Composable
fun SeniorDetailScreen(senior: LinkedSenior, vm: GuardianViewModel, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var opened by remember { mutableStateOf<RecordDto?>(null) }
    var openedUrl by remember { mutableStateOf<UrlRecordDto?>(null) }
    var confirmUnlink by remember { mutableStateOf(false) }
    val records by vm.records.collectAsStateWithLifecycle()
    val urlRecords by vm.urlRecords.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)
    LaunchedEffect(senior.id) {
        vm.loadRecords(senior.id)
        vm.loadUrlRecords(senior.id)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                OutlinedButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("뒤로") }
                Text("${senior.name} 님", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { confirmUnlink = true }) { Text("연결 끊기") }
            }

            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("차단된 문자", style = MaterialTheme.typography.bodyLarge) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("막은 링크", style = MaterialTheme.typography.bodyLarge) })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("보이스피싱", style = MaterialTheme.typography.bodyLarge) })
            }

            when (tab) {
                0 -> RecordsTab(records, onOpen = { opened = it }, onRetry = { vm.loadRecords(senior.id) })
                1 -> UrlRecordsTab(urlRecords, onOpen = { openedUrl = it }, onRetry = { vm.loadUrlRecords(senior.id) })
                else -> Text(
                    "전화 기능이 준비되면, 의심 전화와 통화 뒤 위험한 행동(원격 조종 프로그램 설치 등)을 여기에 보여 드려요.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }
    }

    opened?.let { record -> RecordDialog(record) { opened = null } }
    openedUrl?.let { record -> UrlRecordDialog(record) { openedUrl = null } }

    if (confirmUnlink) {
        AlertDialog(
            onDismissRequest = { confirmUnlink = false },
            title = { Text("연결을 끊을까요?") },
            text = { Text("${senior.name} 님의 차단 문자와 막은 링크를 더 이상 볼 수 없어요. 다시 연결하려면 어르신 휴대폰에서 새 숫자를 받아야 해요.") },
            confirmButton = { TextButton(onClick = { confirmUnlink = false; vm.unlink(senior.id, onBack) }) { Text("연결 끊기") } },
            dismissButton = { TextButton(onClick = { confirmUnlink = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun RecordsTab(records: Loadable<List<RecordDto>>, onOpen: (RecordDto) -> Unit, onRetry: () -> Unit) {
    when (records) {
        Loadable.Loading -> CircularProgressIndicator(modifier = Modifier.padding(20.dp))
        is Loadable.Failed -> Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(records.message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onRetry) { Text("다시 불러오기") }
        }
        is Loadable.Ready -> if (records.value.isEmpty()) {
            Text("아직 걸러낸 문자가 없어요.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(20.dp))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Text("", modifier = Modifier.heightIn(min = 4.dp)) }
                items(records.value, key = { it.id }) { record -> RecordRow(record) { onOpen(record) } }
                item { Text("", modifier = Modifier.heightIn(min = 16.dp)) }
            }
        }
    }
}

@Composable
private fun RecordRow(record: RecordDto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(categoryLabel(record.category), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                Text(if (record.action == "HIDDEN") "치움" else "경고", style = MaterialTheme.typography.titleLarge)
            }
            Text("${record.sender} · ${formatTime(Instant.parse(record.receivedAt).toEpochMilli())}", style = MaterialTheme.typography.bodyMedium)
            Text(record.body, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RecordDialog(record: RecordDto, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(categoryLabel(record.category)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(record.reason, style = MaterialTheme.typography.bodyLarge)
                Text("받은 ${sourceLabel(record.source)} · ${record.sender}", style = MaterialTheme.typography.bodyMedium)
                Text(formatTime(Instant.parse(record.receivedAt).toEpochMilli()), style = MaterialTheme.typography.bodyMedium)
                Text(record.body, style = MaterialTheme.typography.bodyLarge)
                Text("계좌·카드·주민번호는 가려서 보여 드려요.", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
private fun UrlRecordsTab(records: Loadable<List<UrlRecordDto>>, onOpen: (UrlRecordDto) -> Unit, onRetry: () -> Unit) {
    when (records) {
        Loadable.Loading -> CircularProgressIndicator(modifier = Modifier.padding(20.dp))
        is Loadable.Failed -> Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(records.message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onRetry) { Text("다시 불러오기") }
        }
        is Loadable.Ready -> if (records.value.isEmpty()) {
            Text(
                "아직 막은 링크가 없어요. 위험한 링크에 들어가려 하면 여기에 보여 드려요.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(20.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Text("", modifier = Modifier.heightIn(min = 4.dp)) }
                items(records.value, key = { it.id }) { record -> UrlRecordRow(record) { onOpen(record) } }
                item { Text("", modifier = Modifier.heightIn(min = 16.dp)) }
            }
        }
    }
}

@Composable
private fun UrlRecordRow(record: UrlRecordDto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(urlCategoryLabel(record.category), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                Text(if (record.action == "BLOCKED") "막음" else "경고", style = MaterialTheme.typography.titleLarge)
            }
            Text("${record.app} · ${formatTime(Instant.parse(record.visitedAt).toEpochMilli())}", style = MaterialTheme.typography.bodyMedium)
            Text(record.host.ifEmpty { record.url }, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun UrlRecordDialog(record: UrlRecordDto, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(urlCategoryLabel(record.category)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(record.reason, style = MaterialTheme.typography.bodyLarge)
                Text("${record.app}에서 열려고 함 · ${if (record.action == "BLOCKED") "자동으로 막음" else "경고만 함"}", style = MaterialTheme.typography.bodyMedium)
                Text(formatTime(Instant.parse(record.visitedAt).toEpochMilli()), style = MaterialTheme.typography.bodyMedium)
                Text(record.url, style = MaterialTheme.typography.bodyLarge)
                Text("주소 뒤쪽의 개인정보는 지우고 보여 드려요.", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}
