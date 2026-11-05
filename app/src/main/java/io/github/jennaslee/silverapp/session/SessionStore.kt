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
    /** 어르신: 보호자와 연결되어 있는지. 연결 전에는 연결 화면만 쓸 수 있다 */
    val hasGuardian: Boolean = false,
    /** 어르신: 연결된 보호자들이 함께 쓰는 구독 플랜 (none | standard | premium) */
    val plan: String = "none",
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

    /** 어르신 기능(문자 지킴이, 링크 지킴이, 키오스크)을 써도 되는지: 보호자와 연결된 어르신만 */
    val canUseFeatures: Boolean get() = _session.value?.let { it.role == Session.Role.SENIOR && it.hasGuardian } == true

    /** 키오스크 도우미: 가족이 Standard 이상 구독했을 때 */
    val canUseKiosk: Boolean get() = canUseFeatures && _session.value?.plan in setOf("standard", "premium")

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        val token = p.getString("token", null)
        val userId = p.getString("userId", null)
        val role = p.getString("role", null)?.let { runCatching { Session.Role.valueOf(it) }.getOrNull() }
        if (token != null && userId != null && role != null) {
            _session.value = Session(
                token, userId, role, p.getString("name", "").orEmpty(), p.getString("email", null),
                hasGuardian = p.getBoolean("hasGuardian", false),
                plan = p.getString("plan", "none") ?: "none",
            )
        }
    }

    fun save(token: String, user: UserDto) {
        val session = Session(
            token = token,
            userId = user.id,
            role = if (user.role == "guardian") Session.Role.GUARDIAN else Session.Role.SENIOR,
            name = user.name,
            email = user.email,
            hasGuardian = user.hasGuardian ?: false,
            plan = user.plan ?: "none",
        )
        prefs?.edit {
            putString("token", session.token)
            putString("userId", session.userId)
            putString("role", session.role.name)
            putString("name", session.name)
            putString("email", session.email)
            putBoolean("hasGuardian", session.hasGuardian)
            putString("plan", session.plan)
        }
        _session.value = session
    }

    /** 서버에서 확인한 보호자 연결 여부를 반영한다 (연결되면 첫 화면이 열리고, 모두 끊기면 다시 잠긴다) */
    fun setHasGuardian(value: Boolean) {
        val s = _session.value ?: return
        if (s.role != Session.Role.SENIOR || s.hasGuardian == value) return
        prefs?.edit { putBoolean("hasGuardian", value) }
        _session.value = s.copy(hasGuardian = value)
    }

    /** 서버에서 확인한 어르신의 플랜을 반영한다 */
    fun setPlan(value: String) {
        val s = _session.value ?: return
        if (s.role != Session.Role.SENIOR || s.plan == value) return
        prefs?.edit { putString("plan", value) }
        _session.value = s.copy(plan = value)
    }

    fun clear() {
        prefs?.edit { clear() }
        _session.value = null
    }
}
