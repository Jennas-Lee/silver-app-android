package io.github.jennaslee.silverapp.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.jennaslee.silverapp.data.remote.ApiClient
import io.github.jennaslee.silverapp.data.remote.AuthResponse
import io.github.jennaslee.silverapp.data.remote.GuardianLoginRequest
import io.github.jennaslee.silverapp.data.remote.GuardianSignupRequest
import io.github.jennaslee.silverapp.data.remote.SeniorStartRequest
import io.github.jennaslee.silverapp.guard.RecordSync
import io.github.jennaslee.silverapp.session.SessionStore
import io.github.jennaslee.silverapp.ui.BigButton
import kotlinx.coroutines.launch

private enum class Step { CHOOSE, SENIOR, GUARDIAN_LOGIN, GUARDIAN_SIGNUP }

/** 처음 실행: 어르신 / 보호자 고르기 → 시작 */
@Composable
fun OnboardingScreen() {
    var step by remember { mutableStateOf(Step.CHOOSE) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (step != Step.CHOOSE) BackHandler { step = Step.CHOOSE; error = null }

    fun submit(call: suspend () -> AuthResponse) {
        busy = true
        error = null
        scope.launch {
            runCatching { call() }
                .onSuccess {
                    SessionStore.save(it.token, it.user)
                    RecordSync.syncSoon()
                }
                .onFailure { error = ApiClient.errorMessage(it) }
            busy = false
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("실버 도우미", style = MaterialTheme.typography.headlineLarge)

            when (step) {
                Step.CHOOSE -> ChooseRole(onSenior = { step = Step.SENIOR }, onGuardian = { step = Step.GUARDIAN_LOGIN })
                Step.SENIOR -> SeniorStart(busy) { name -> submit { ApiClient.api.startSenior(SeniorStartRequest(name)) } }
                Step.GUARDIAN_LOGIN -> GuardianLogin(
                    busy = busy,
                    onLogin = { email, pw -> submit { ApiClient.api.guardianLogin(GuardianLoginRequest(email, pw)) } },
                    onSignup = { step = Step.GUARDIAN_SIGNUP; error = null },
                )
                Step.GUARDIAN_SIGNUP -> GuardianSignup(
                    busy = busy,
                    onSignup = { name, email, pw -> submit { ApiClient.api.guardianSignup(GuardianSignupRequest(name, email, pw)) } },
                    onLogin = { step = Step.GUARDIAN_LOGIN; error = null },
                )
            }

            error?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error) }
            if (step != Step.CHOOSE) {
                TextButton(onClick = { step = Step.CHOOSE; error = null }, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text("처음으로", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun ChooseRole(onSenior: () -> Unit, onGuardian: () -> Unit) {
    Text("이 휴대폰은 누가 쓰시나요?", style = MaterialTheme.typography.headlineMedium)
    RoleCard(
        title = "제가 쓸게요",
        description = "사기 문자를 걸러 드리고, 주문하는 화면 쓰는 법을 알려 드려요.",
        button = "어르신으로 시작하기",
        onClick = onSenior,
    )
    RoleCard(
        title = "가족을 돌볼게요",
        description = "부모님 휴대폰에 온 사기 문자와 전화 내역을 확인할 수 있어요.",
        button = "보호자로 시작하기",
        onClick = onGuardian,
    )
}

@Composable
private fun RoleCard(title: String, description: String, button: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, style = MaterialTheme.typography.bodyLarge)
            BigButton(button, onClick)
        }
    }
}

@Composable
private fun SeniorStart(busy: Boolean, onStart: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Text("성함을 알려 주세요", style = MaterialTheme.typography.headlineMedium)
    BigTextField(value = name, onValueChange = { name = it }, label = "성함")
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Text(
            "가족을 보호자로 연결하면, 걸러낸 사기 문자를 가족도 볼 수 있어요. 계좌번호나 주민번호는 가려서 보여 드려요.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp),
        )
    }
    BigButton(if (busy) "준비하는 중…" else "시작하기") { if (!busy && name.isNotBlank()) onStart(name.trim()) }
}

@Composable
private fun GuardianLogin(busy: Boolean, onLogin: (String, String) -> Unit, onSignup: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Text("보호자 로그인", style = MaterialTheme.typography.headlineMedium)
    BigTextField(email, { email = it }, "이메일", KeyboardType.Email)
    BigTextField(password, { password = it }, "비밀번호", KeyboardType.Password, isPassword = true)
    BigButton(if (busy) "로그인하는 중…" else "로그인") {
        if (!busy && email.isNotBlank() && password.isNotEmpty()) onLogin(email.trim(), password)
    }
    OutlinedButton(onClick = onSignup, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
        Text("처음이에요, 가입할게요", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun GuardianSignup(busy: Boolean, onSignup: (String, String, String) -> Unit, onLogin: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Text("보호자 가입", style = MaterialTheme.typography.headlineMedium)
    BigTextField(name, { name = it }, "이름")
    BigTextField(email, { email = it }, "이메일", KeyboardType.Email)
    BigTextField(password, { password = it }, "비밀번호 (8자 이상)", KeyboardType.Password, isPassword = true)
    BigButton(if (busy) "가입하는 중…" else "가입하기") {
        if (!busy && name.isNotBlank() && email.isNotBlank() && password.isNotEmpty()) onSignup(name.trim(), email.trim(), password)
    }
    OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
        Text("이미 가입했어요, 로그인할게요", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun BigTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        textStyle = MaterialTheme.typography.bodyLarge,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
    )
}
