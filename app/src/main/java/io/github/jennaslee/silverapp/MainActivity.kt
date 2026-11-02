package io.github.jennaslee.silverapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.jennaslee.silverapp.data.remote.LinkedSenior
import io.github.jennaslee.silverapp.guard.GuardNotifier
import io.github.jennaslee.silverapp.kiosk.KioskScreen
import io.github.jennaslee.silverapp.session.Session
import io.github.jennaslee.silverapp.session.SessionStore
import io.github.jennaslee.silverapp.ui.HomeScreen
import io.github.jennaslee.silverapp.ui.guardian.GuardianHomeScreen
import io.github.jennaslee.silverapp.ui.guardian.GuardianViewModel
import io.github.jennaslee.silverapp.ui.guardian.SeniorDetailScreen
import io.github.jennaslee.silverapp.ui.onboarding.OnboardingScreen
import io.github.jennaslee.silverapp.ui.theme.SilverTheme

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
                // 계정 유형에 따라 화면이 나뉜다: 처음 실행 / 어르신 / 보호자
                val session by SessionStore.session.collectAsStateWithLifecycle()
                when (session?.role) {
                    null -> OnboardingScreen()
                    Session.Role.GUARDIAN -> GuardianApp()
                    Session.Role.SENIOR -> if (showKiosk) {
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
