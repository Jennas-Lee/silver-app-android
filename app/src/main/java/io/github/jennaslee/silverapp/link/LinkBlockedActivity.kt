package io.github.jennaslee.silverapp.link

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jennaslee.silverapp.guard.urlCategoryLabel
import io.github.jennaslee.silverapp.kiosk.Speaker
import io.github.jennaslee.silverapp.ui.BigButton
import io.github.jennaslee.silverapp.ui.theme.SilverTheme

/** 위험한 주소를 막은 뒤 크게 보여주고 소리 내어 읽어주는 경고 화면 */
class LinkBlockedActivity : ComponentActivity() {
    private var speaker: Speaker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reason = intent.getStringExtra(EXTRA_REASON).orEmpty()
        val category = intent.getStringExtra(EXTRA_CATEGORY).orEmpty()
        val host = intent.getStringExtra(EXTRA_HOST).orEmpty()

        speaker = Speaker(this).also { it.speak("위험한 곳이라 막았어요. $reason") }

        enableEdgeToEdge()
        setContent {
            SilverTheme {
                Scaffold { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Text("위험한 곳이라 막았어요", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.error)
                        Text(urlCategoryLabel(category), style = MaterialTheme.typography.titleLarge)
                        Text(reason, style = MaterialTheme.typography.headlineSmall)
                        Text("들어가려던 주소: $host", style = MaterialTheme.typography.bodyLarge)
                        Text("돈을 보내거나 개인정보를 넣으라고 하면 가족에게 먼저 물어보세요.", style = MaterialTheme.typography.bodyLarge)
                        BigButton("알겠어요") { finish() }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        speaker?.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_REASON = "reason"
        private const val EXTRA_CATEGORY = "category"
        private const val EXTRA_HOST = "host"

        fun start(context: Context, record: LinkRecord) {
            context.startActivity(
                Intent(context, LinkBlockedActivity::class.java)
                    .putExtra(EXTRA_REASON, record.reason)
                    .putExtra(EXTRA_CATEGORY, record.category)
                    .putExtra(EXTRA_HOST, record.host)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            )
        }
    }
}
