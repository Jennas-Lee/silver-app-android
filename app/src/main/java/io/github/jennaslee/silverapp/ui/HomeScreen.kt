package io.github.jennaslee.silverapp.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.jennaslee.silverapp.data.remote.ApiClient
import io.github.jennaslee.silverapp.guard.GuardRecord
import io.github.jennaslee.silverapp.guard.GuardStore
import io.github.jennaslee.silverapp.guard.SmsGuardService
import io.github.jennaslee.silverapp.guard.categoryLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

/** 권한과 설정 상태. 설정 화면에서 돌아올 때마다 다시 확인한다. */
private data class Permissions(
    val guardOn: Boolean,
    val notificationsOn: Boolean,
    val contactsOn: Boolean,
    val smsOn: Boolean,
)

private fun readPermissions(context: Context) = Permissions(
    guardOn = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context),
    notificationsOn = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
    contactsOn = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED,
    smsOn = SmsGuardService.hasReceiveSms(context),
)

private fun openGuardSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
            Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
            ComponentName(context, SmsGuardService::class.java).flattenToString(),
        )
    } else {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }
    runCatching { context.startActivity(intent) }
        .onFailure { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
}

@Composable
fun HomeScreen(
    selectedRecordId: String?,
    onOpenRecord: (String) -> Unit,
    onCloseRecord: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val server by vm.server.collectAsStateWithLifecycle()
    val records by GuardStore.records.collectAsStateWithLifecycle()
    var permissions by remember { mutableStateOf(readPermissions(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permissions = readPermissions(context) }

    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissions = readPermissions(context)
    }

    val selected = selectedRecordId?.let { id -> records.firstOrNull { it.id == id } }
    if (selected != null) {
        RecordDetailScreen(
            record = selected,
            onBack = onCloseRecord,
            onDelete = {
                GuardStore.remove(selected.id)
                onCloseRecord()
            },
        )
        return
    }

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
                Text("실버 도우미", style = MaterialTheme.typography.headlineLarge)
            }

            item {
                GuardStatusCard(
                    permissions = permissions,
                    onTurnOnGuard = { openGuardSettings(context) },
                    onAllowNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onAllowContacts = { requestPermission.launch(Manifest.permission.READ_CONTACTS) },
                    onAllowSms = { requestPermission.launch(Manifest.permission.RECEIVE_SMS) },
                )
            }

            item {
                Text("걸러낸 문자", style = MaterialTheme.typography.headlineMedium)
            }

            if (records.isEmpty()) {
                item {
                    Text(
                        "아직 걸러낸 문자가 없어요.\n사기 문자가 오면 여기에 모아 둘게요.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                items(records, key = { it.id }) { record ->
                    RecordCard(record = record, onClick = { onOpenRecord(record.id) })
                }
            }

            item {
                ServerStatusLine(server = server, onRetry = vm::checkServer)
                Spacer(Modifier.heightIn(min = 24.dp))
            }
        }
    }
}

@Composable
private fun GuardStatusCard(
    permissions: Permissions,
    onTurnOnGuard: () -> Unit,
    onAllowNotifications: () -> Unit,
    onAllowContacts: () -> Unit,
    onAllowSms: () -> Unit,
) {
    val on = permissions.guardOn
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (on) "문자 지킴이가 켜져 있어요" else "문자 지킴이가 꺼져 있어요",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                if (on) {
                    "사기 문자나 카톡이 오면 치우고 알려 드려요."
                } else {
                    "켜 두시면 사기 문자를 대신 걸러 드려요.\n다음 화면에서 실버 도우미를 찾아 켜 주세요."
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!on) {
                BigButton("문자 지킴이 켜기", onTurnOnGuard)
            }
            if (!permissions.notificationsOn) {
                Text("사기 문자가 왔을 때 알려 드리려면 알림을 허용해 주세요.", style = MaterialTheme.typography.bodyMedium)
                BigButton("알림 허용하기", onAllowNotifications)
            }
            if (on && !permissions.smsOn) {
                Text("문자 내용을 직접 확인해야 사기 문자를 빠짐없이 찾을 수 있어요.", style = MaterialTheme.typography.bodyMedium)
                BigButton("문자 확인 허용하기", onAllowSms)
            }
            if (on && !permissions.contactsOn) {
                Text("아는 사람의 문자는 검사하지 않도록 연락처를 확인할게요.", style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(
                    onClick = onAllowContacts,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp),
                ) {
                    Text("연락처 확인 허용하기", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun RecordCard(record: GuardRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    categoryLabel(record.category),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    if (record.action == GuardRecord.Action.HIDDEN) "치웠어요" else "경고했어요",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Text("${record.sender} · ${formatTime(record.receivedAt)}", style = MaterialTheme.typography.bodyMedium)
            Text(record.reason, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ServerStatusLine(server: ServerState, onRetry: () -> Unit) {
    val text = when (server) {
        ServerState.Checking -> "서버 연결 확인 중…"
        is ServerState.Ok -> "서버 연결됨 (${server.version})"
        is ServerState.Error -> "서버에 연결하지 못했어요. 눌러서 다시 확인"
    }
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (server is ServerState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .clickable(onClick = onRetry)
            .padding(vertical = 12.dp),
    )
}

@Composable
fun BigButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

private val TIME_FORMAT = DateTimeFormatter.ofPattern("M월 d일 a h시 mm분", Locale.KOREAN)

fun formatTime(epochMillis: Long): String =
    TIME_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
