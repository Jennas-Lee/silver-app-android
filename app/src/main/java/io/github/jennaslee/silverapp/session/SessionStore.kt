package io.github.jennaslee.silverapp.session

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.jennaslee.silverapp.data.remote.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 로그인한 계정 */
data class Session(
    val token: String,
    val userId: String,
    val role: Role,
    val name: String,
    val email: String?,
) {
    enum class Role { SENIOR, GUARDIAN }
}

/**
 * 로그인 토큰과 계정 정보를 앱 전용 저장소에 보관한다.
 * PoC 단계라 일반 SharedPreferences(앱 밖에서 읽을 수 없는 영역)를 쓴다. 정식 버전에서는 Android Keystore로 암호화한다.
 */
object SessionStore {
    private const val PREFS = "session"

    private var prefs: SharedPreferences? = null
    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session

    val current: Session? get() = _session.value
    val isGuardian: Boolean get() = _session.value?.role == Session.Role.GUARDIAN

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        val token = p.getString("token", null)
        val userId = p.getString("userId", null)
        val role = p.getString("role", null)?.let { runCatching { Session.Role.valueOf(it) }.getOrNull() }
        if (token != null && userId != null && role != null) {
            _session.value = Session(token, userId, role, p.getString("name", "").orEmpty(), p.getString("email", null))
        }
    }

    fun save(token: String, user: UserDto) {
        val session = Session(
            token = token,
            userId = user.id,
            role = if (user.role == "guardian") Session.Role.GUARDIAN else Session.Role.SENIOR,
            name = user.name,
            email = user.email,
        )
        prefs?.edit {
            putString("token", session.token)
            putString("userId", session.userId)
            putString("role", session.role.name)
            putString("name", session.name)
            putString("email", session.email)
        }
        _session.value = session
    }

    fun clear() {
        prefs?.edit { clear() }
        _session.value = null
    }
}
