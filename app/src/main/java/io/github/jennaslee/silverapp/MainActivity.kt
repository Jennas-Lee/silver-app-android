package io.github.jennaslee.silverapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jennaslee.silverapp.guard.GuardNotifier
import io.github.jennaslee.silverapp.guard.GuardStore
import io.github.jennaslee.silverapp.ui.HomeScreen
import io.github.jennaslee.silverapp.ui.theme.SilverTheme

class MainActivity : ComponentActivity() {

    /** 경고 알림을 눌러 들어오면 해당 기록을 바로 연다 */
    private var selectedRecordId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GuardStore.init(this)
        GuardNotifier.ensureChannel(this)
        selectedRecordId = intent.getStringExtra(GuardNotifier.EXTRA_RECORD_ID)

        enableEdgeToEdge()
        setContent {
            SilverTheme {
                HomeScreen(
                    selectedRecordId = selectedRecordId,
                    onOpenRecord = { selectedRecordId = it },
                    onCloseRecord = { selectedRecordId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(GuardNotifier.EXTRA_RECORD_ID)?.let { selectedRecordId = it }
    }
}
