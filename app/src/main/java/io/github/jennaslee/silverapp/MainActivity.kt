package io.github.jennaslee.silverapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.jennaslee.silverapp.data.remote.ApiClient
import io.github.jennaslee.silverapp.data.remote.LinkedSenior
import io.github.jennaslee.silverapp.guard.GuardNotifier
import io.github.jennaslee.silverapp.guard.RecordSync
import io.github.jennaslee.silverapp.kiosk.KioskScreen
import io.github.jennaslee.silverapp.session.Session
import io.github.jennaslee.silverapp.session.SessionStore
import io.github.jennaslee.silverapp.ui.HomeScreen
import io.github.jennaslee.silverapp.ui.guardian.GuardianHomeScreen
import io.github.jennaslee.silverapp.ui.guardian.GuardianViewModel
import io.github.jennaslee.silverapp.ui.guardian.SeniorDetailScreen
import io.github.jennaslee.silverapp.ui.onboarding.OnboardingScreen
import io.github.jennaslee.silverapp.ui.senior.LinkRequiredScreen
import io.github.jennaslee.silverapp.ui.theme.SilverTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** 경고 알림을 눌러 들어오면 해당 기록을 바로 연다 */
    private var selectedRecordId by mutableStateOf<String?>(null)

    /** 키오스크 도우미 화면을 보여주는 중인지 */
    private var showKiosk by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GuardNotifier.ensureChannel(this)
        selectedRecordId = intent.getStringExtra(GuardNotifier.EXTRA_RECORD_ID)

        enableEdgeToEdge()
        setContent {
            SilverTheme {
                // 계정 유형에 따라 화면이 나뉜다: 처음 실행 / 보호자 연결 전 어르신 / 어르신 / 보호자
                val session by SessionStore.session.collectAsStateWithLifecycle()
                // 연결되면 그동안 못 올린 기록을 올린다
                LaunchedEffect(session?.hasGuardian) { RecordSync.syncSoon() }
                when (session?.role) {
                    null -> OnboardingScreen()
                    Session.Role.GUARDIAN -> GuardianApp()
                    Session.Role.SENIOR -> if (session?.hasGuardian != true) {
                        LinkRequiredScreen()
                    } else if (showKiosk) {
                        KioskScreen(onClose = { showKiosk = false })
                    } else {
                        HomeScreen(
                            selectedRecordId = selectedRecordId,
                            onOpenRecord = { selectedRecordId = it },
                            onCloseRecord = { selectedRecordId = null },
                            onOpenKiosk = { showKiosk = true },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 보호자가 연결을 끊었을 수 있으니 열 때마다 확인한다 (인터넷이 안 되면 저장된 값을 그대로 쓴다)
        if (SessionStore.current?.role == Session.Role.SENIOR) {
            lifecycleScope.launch {
                runCatching { ApiClient.api.me() }.onSuccess { me ->
                    me.user.hasGuardian?.let(SessionStore::setHasGuardian)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(GuardNotifier.EXTRA_RECORD_ID)?.let {
            showKiosk = false
            selectedRecordId = it
        }
    }
}

/** 보호자 화면: 어르신 목록 ↔ 어르신 상세. 문자 지킴이·키오스크 기능은 없다. */
@Composable
private fun GuardianApp(vm: GuardianViewModel = viewModel()) {
    var openedSenior by remember { mutableStateOf<LinkedSenior?>(null) }
    val senior = openedSenior
    if (senior == null) {
        GuardianHomeScreen(vm, onOpenSenior = { openedSenior = it })
    } else {
        SeniorDetailScreen(senior, vm, onBack = { openedSenior = null })
    }
}
