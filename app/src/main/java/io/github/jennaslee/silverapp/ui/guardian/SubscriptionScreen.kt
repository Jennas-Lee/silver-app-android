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
import io.github.jennaslee.silverapp.ui.BigButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** 구독 플랜 (서버 값: none | standard | premium) */
enum class Plan(val id: String, val label: String) {
    NONE("none", "없음"),
    STANDARD("standard", "Standard"),
    PREMIUM("premium", "Premium"),
    ;

    companion object {
        fun of(id: String?): Plan = entries.firstOrNull { it.id == id } ?: NONE
    }
}

class SubscriptionViewModel : ViewModel() {
    private val _plan = MutableStateFlow<Loadable<Plan>>(Loadable.Loading)
    val plan: StateFlow<Loadable<Plan>> = _plan

    /** 바꾸는 중인 플랜 */
    private val _saving = MutableStateFlow<Plan?>(null)
    val saving: StateFlow<Plan?> = _saving

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun load() {
        viewModelScope.launch {
            if (_plan.value !is Loadable.Ready) _plan.value = Loadable.Loading
            runCatching { ApiClient.api.subscription() }
                .onSuccess { _plan.value = Loadable.Ready(Plan.of(it.plan)) }
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
                    _plan.value = Loadable.Ready(now)
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
                    Text("지금 플랜: ${p.value.label}", style = MaterialTheme.typography.titleLarge)
                    Plan.entries.forEach { option ->
                        PlanCard(
                            plan = option,
                            current = option == p.value,
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
private fun PlanCard(plan: Plan, current: Boolean, saving: Boolean, onChoose: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(plan.label, style = MaterialTheme.typography.titleLarge)
            if (current) {
                Text("지금 쓰는 플랜이에요", style = MaterialTheme.typography.bodyLarge)
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
