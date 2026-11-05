package io.github.jennaslee.silverapp.ui.senior

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jennaslee.silverapp.link.LinkGuardService
import io.github.jennaslee.silverapp.link.LinkRecord
import io.github.jennaslee.silverapp.link.LinkStore
import io.github.jennaslee.silverapp.ui.BigButton
import io.github.jennaslee.silverapp.ui.formatTime

/** 어르신 첫 화면: 링크 지킴이 켜기와 최근에 막은 주소 */
@Composable
fun LinkGuardCard() {
    val context = LocalContext.current
    var on by remember { mutableStateOf(LinkGuardService.isEnabled(context)) }
    val records by LinkStore.records.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { on = LinkGuardService.isEnabled(context) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (on) "링크 지킴이가 켜져 있어요" else "링크 지킴이가 꺼져 있어요", style = MaterialTheme.typography.titleLarge)
            if (on) {
                Text("위험한 인터넷 주소에 들어가면 바로 뒤로 돌아가고 알려 드려요.", style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(
                    "켜 두시면 문자나 카톡 속 위험한 주소를 대신 막아 드려요.\n다음 화면에서 '실버 도우미 링크 지킴이'를 찾아 켜 주세요.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                BigButton("링크 지킴이 켜기") { openAccessibilitySettings(context) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Text(
                        "회색으로 눌리지 않으면, 아래 버튼을 누르고 오른쪽 위 점 세 개 → '제한된 설정 허용'을 누른 뒤 다시 켜 주세요.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(
                        onClick = { openAppInfo(context) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    ) { Text("실버 도우미 정보 열기", style = MaterialTheme.typography.labelLarge) }
                }
            }
            val recent = records.take(3)
            if (recent.isNotEmpty()) {
                HorizontalDivider()
                Text("최근에 막은 주소", style = MaterialTheme.typography.titleMedium)
                recent.forEach { RecentLink(it) }
            }
        }
    }
}

@Composable
private fun RecentLink(record: LinkRecord) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            "${if (record.action == LinkRecord.Action.BLOCKED) "막았어요" else "경고했어요"} · ${formatTime(record.visitedAt)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(record.reason, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

private fun openAccessibilitySettings(context: Context) {
    // 이 기능만 바로 여는 화면은 기기마다 막혀 있을 수 있어서, 안 되면 접근성 설정 첫 화면을 연다
    val detail = Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS")
        .putExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName(context, LinkGuardService::class.java).flattenToString())
    runCatching { context.startActivity(detail) }
        .onFailure { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
}

private fun openAppInfo(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
    )
}
