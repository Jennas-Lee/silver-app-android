package io.github.jennaslee.silverapp.ui.guardian

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.jennaslee.silverapp.data.remote.ApiClient
import io.github.jennaslee.silverapp.data.remote.SetPlanRequest
import io.github.jennaslee.silverapp.data.remote.SharedPlanDto
import io.github.jennaslee.silverapp.ui.BigButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** 구독 플랜 (서버 값: none | standard | premium) */
enum class Plan(val id: String, val label: String, val features: String) {
    NONE("none", "없음", "문자 지킴이, 링크 지킴이"),
    STANDARD("standard", "Standard", "문자 지킴이, 링크 지킴이, 키오스크 도우미"),
    PREMIUM("premium", "Premium", "Standard의 모든 기능"),
    ;

    companion object {
        fun of(id: String?): Plan = entries.firstOrNull { it.id == id } ?: NONE
    }
}

/** 내 플랜과, 다른 보호자가 구독해서 함께 쓰는 플랜 */
data class PlanState(val mine: Plan, val shared: SharedPlanDto?) {
    /** 실제로 쓰는 플랜 */
    val effective: Plan get() = shared?.let { Plan.of(it.plan) }?.takeIf { it.ordinal > mine.ordinal } ?: mine
}

class SubscriptionViewModel : ViewModel() {
    private val _plan = MutableStateFlow<Loadable<PlanState>>(Loadable.Loading)
    val plan: StateFlow<Loadable<PlanState>> = _plan

    /** 바꾸는 중인 플랜 */
    private val _saving = MutableStateFlow<Plan?>(null)
    val saving: StateFlow<Plan?> = _saving

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun load() {
        viewModelScope.launch {
            if (_plan.value !is Loadable.Ready) _plan.value = Loadable.Loading
            runCatching { ApiClient.api.subscription() }
                .onSuccess { _plan.value = Loadable.Ready(PlanState(Plan.of(it.plan), it.sharedFrom)) }
                .onFailure { _plan.value = Loadable.Failed(ApiClient.errorMessage(it)) }
        }
    }

    fun choose(plan: Plan) {
        if (_saving.value != null) return
        _saving.value = plan
        _message.value = null
        viewModelScope.launch {
            runCatching { ApiClient.api.setSubscription(SetPlanRequest(plan.id)) }
                .onSuccess {
                    val now = Plan.of(it.plan)
                    _plan.value = Loadable.Ready(PlanState(now, it.sharedFrom))
                    _message.value = if (now == Plan.NONE) "구독을 해지했어요." else "${now.label} 플랜으로 바꿨어요."
                }
                .onFailure { _message.value = ApiClient.errorMessage(it) }
            _saving.value = null
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

/** 보호자 메뉴: 구독 플랜 고르기. 아직 결제가 없어서 누르면 바로 바뀐다 */
@Composable
fun SubscriptionScreen(vm: SubscriptionViewModel, onBack: () -> Unit) {
    val plan by vm.plan.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("구독", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("뒤로") }
            }
            Text("아직 결제 기능이 없어서, 고르면 바로 바뀌어요.", style = MaterialTheme.typography.bodyMedium)

            when (val p = plan) {
                Loadable.Loading -> CircularProgressIndicator()
                is Loadable.Failed -> {
                    Text(p.message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = vm::load) { Text("다시 불러오기") }
                }
                is Loadable.Ready -> {
                    val state = p.value
                    Text("지금 플랜: ${state.effective.label}", style = MaterialTheme.typography.titleLarge)
                    val shared = state.shared
                    if (shared != null) {
                        Text(
                            "${shared.name} 님이 구독한 ${Plan.of(shared.plan).label} 플랜을 함께 쓰고 있어요.\n" +
                                "한 가족은 한 분만 구독하면 돼요.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    } else {
                        Text("한 분이 구독하면 같은 어르신에 연결된 가족 모두 함께 써요.", style = MaterialTheme.typography.bodyMedium)
                    }
                    Plan.entries.forEach { option ->
                        PlanCard(
                            plan = option,
                            current = option == state.mine,
                            // 다른 가족이 구독 중이면 따로 구독할 수 없다 (해지는 가능)
                            locked = shared != null && option != Plan.NONE,
                            saving = saving == option,
                            onChoose = { vm.choose(option) },
                        )
                    }
                }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            Spacer(Modifier.heightIn(min = 24.dp))
        }
    }
}

@Composable
private fun PlanCard(plan: Plan, current: Boolean, locked: Boolean, saving: Boolean, onChoose: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(plan.label, style = MaterialTheme.typography.titleLarge)
            Text(plan.features, style = MaterialTheme.typography.bodyMedium)
            if (current) {
                Text("지금 내 플랜이에요", style = MaterialTheme.typography.bodyLarge)
            } else if (locked) {
                Text("다른 가족이 구독 중이라 고를 수 없어요", style = MaterialTheme.typography.bodyMedium)
            } else {
                val label = when {
                    saving -> "바꾸는 중…"
                    plan == Plan.NONE -> "구독 해지하기"
                    else -> "${plan.label} 고르기"
                }
                BigButton(label) { if (!saving) onChoose() }
            }
        }
    }
}
