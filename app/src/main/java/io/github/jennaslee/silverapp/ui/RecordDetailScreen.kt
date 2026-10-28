package io.github.jennaslee.silverapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jennaslee.silverapp.guard.GuardRecord
import io.github.jennaslee.silverapp.guard.categoryLabel
import io.github.jennaslee.silverapp.guard.sourceLabel

@Composable
fun RecordDetailScreen(record: GuardRecord, onBack: () -> Unit, onDelete: () -> Unit) {
    BackHandler(onBack = onBack)

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.heightIn(min = 56.dp)) {
                Text("뒤로", style = MaterialTheme.typography.labelLarge)
            }

            Text(
                categoryLabel(record.category),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.error,
            )
            Text(record.reason, style = MaterialTheme.typography.titleLarge)

            Text(
                if (record.action == GuardRecord.Action.HIDDEN) {
                    "이 문자는 알림에서 치웠어요. 문자함에는 그대로 남아 있으니, 안에 있는 주소는 누르지 마세요."
                } else {
                    "이 문자는 조심하시라고 알려 드렸어요. 안에 있는 주소는 누르지 마시고, 걱정되면 가족에게 물어보세요."
                },
                style = MaterialTheme.typography.bodyLarge,
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("받은 ${sourceLabel(record.source)}", style = MaterialTheme.typography.titleLarge)
                    Text("보낸 사람: ${record.sender}", style = MaterialTheme.typography.bodyLarge)
                    Text("받은 때: ${formatTime(record.receivedAt)}", style = MaterialTheme.typography.bodyLarge)
                    Text(record.body, style = MaterialTheme.typography.bodyLarge)
                }
            }

            BigButton("목록에서 지우기", onDelete)
        }
    }
}
