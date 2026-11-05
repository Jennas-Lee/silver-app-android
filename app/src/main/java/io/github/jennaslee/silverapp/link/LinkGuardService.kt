package io.github.jennaslee.silverapp.link

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.accessibilityservice.AccessibilityServiceInfo
import io.github.jennaslee.silverapp.data.remote.UrlCheckResult
import io.github.jennaslee.silverapp.guard.GuardNotifier
import io.github.jennaslee.silverapp.guard.RecordSync
import io.github.jennaslee.silverapp.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 링크 지킴이. 인터넷 창(크롬, 삼성 인터넷, 네이버 등)의 주소창만 읽어서
 * 위험한 주소면 바로 뒤로 가고 경고 화면을 띄운다. 의심스러운 주소는 알림으로 경고만 한다.
 * 어떤 앱을 볼지는 res/xml/link_guard_config.xml의 packageNames로 정한다 (다른 앱의 글자는 받지 않음).
 */
class LinkGuardService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val scanRunnable = Runnable { scan() }

    /** 앱별 마지막 화면(Activity) 이름. 카카오톡 같은 앱에서 인터넷 화면인지 구분한다 */
    private val windowClass = mutableMapOf<String, String>()

    /**
     * 앱별로 주소창에서 마지막으로 읽은 주소와 시각. 주소창 글자가 바뀔 때 이벤트로 받아 둔다.
     * 팝업(알림 허용 창, 메뉴, 사이트 경고창)이 위를 덮으면 주소창을 읽을 수 없어서 이 값을 쓴다.
     */
    private val lastBar = mutableMapOf<String, Pair<String, Long>>()
    private var scanScheduled = false

    private var lastKey: String? = null
    private var lastKeyAt = 0L

    /**
     * 최근에 막은 주소. 팝업만 닫히고 페이지가 남았거나, 경고 화면을 닫은 뒤 다시 보이면 또 뒤로 간다.
     */
    private var blockedKey: String? = null
    private var blockedAt = 0L
    private var lastBackAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || !SessionStore.canUseFeatures) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.className?.toString()?.let { windowClass[pkg] = it }
        }
        rememberBar(pkg, event.source)
        // 이벤트가 몰려 오므로 잠깐 모았다가 한 번 읽는다. 이벤트가 계속 와도 미루지 않는다 (팝업이 뜨기 전에 읽도록)
        if (!scanScheduled) {
            scanScheduled = true
            handler.postDelayed(scanRunnable, SCAN_DELAY_MS)
        }
    }

    /** 이벤트가 주소창에서 왔으면 그 주소를 기억한다 */
    private fun rememberBar(pkg: String, source: AccessibilityNodeInfo?) {
        if (source == null) return
        val bar = UrlExtractor.urlBarIds(pkg).firstNotNullOfOrNull { id ->
            if (source.viewIdResourceName == id) source else runCatching { source.findAccessibilityNodeInfosByViewId(id).firstOrNull() }.getOrNull()
        } ?: return
        if (bar.isFocused) return
        val key = bar.text?.toString()?.takeIf(UrlExtractor::looksLikeUrl)?.let(UrlExtractor::cleanKey)
        if (key == null) lastBar.remove(pkg) else lastBar[pkg] = key to System.currentTimeMillis()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(scanRunnable)
        scanScheduled = false
        scope.cancel()
        super.onDestroy()
    }

    /**
     * 지금 화면의 (앱, 줄인 주소). 없으면 null.
     * 알림 허용 창이나 사이트가 띄운 팝업이 맨 위에 있어도 그 아래 인터넷 창의 주소창을 읽도록
     * 맨 위 창만이 아니라 화면에 있는 앱 창을 모두 살펴본다.
     */
    private fun currentKey(): Pair<String, String>? {
        val roots = runCatching {
            windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }.mapNotNull { it.root }
        }.getOrDefault(emptyList()).ifEmpty { listOfNotNull(rootInActiveWindow) }
        val height = resources.displayMetrics.heightPixels
        for (root in roots) {
            val pkg = root.packageName?.toString() ?: continue
            if (pkg == packageName) continue
            val texts = ArrayList<ScreenText>()
            collect(root, texts, 0)
            val url = UrlExtractor.find(pkg, texts, height, UrlExtractor.allowHeuristic(pkg, windowClass[pkg])) ?: continue
            val key = UrlExtractor.cleanKey(url) ?: continue
            return pkg to key
        }
        // 팝업이 덮고 있으면: 같은 앱이 맨 위에 있는 동안은 주소창에서 마지막으로 읽은 주소를 쓴다
        val top = roots.firstOrNull()?.packageName?.toString() ?: return null
        val (key, at) = lastBar[top] ?: return null
        return if (System.currentTimeMillis() - at < BAR_MEMORY_MS) top to key else null
    }

    private fun collect(node: AccessibilityNodeInfo, out: MutableList<ScreenText>, depth: Int) {
        if (out.size >= MAX_NODES || depth > MAX_DEPTH) return
        val text = node.text?.toString()
        if (!text.isNullOrBlank()) {
            val rect = Rect().also { node.getBoundsInScreen(it) }
            out += ScreenText(node.viewIdResourceName, text, node.isFocused, rect.top)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collect(it, out, depth + 1) }
        }
    }

    private fun scan() {
        scanScheduled = false
        val (pkg, key) = currentKey() ?: return
        val now = System.currentTimeMillis()

        if (key == blockedKey && now - blockedAt < BLOCKED_MEMORY_MS) {
            if (now - lastBackAt > BACK_INTERVAL_MS) {
                lastBackAt = now
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            return
        }
        if (key == lastKey && now - lastKeyAt < SAME_URL_SKIP_MS) return
        lastKey = key
        lastKeyAt = now

        scope.launch {
            val result = LinkChecker.check(key) ?: return@launch
            when (result.verdict) {
                "malicious" -> block(pkg, key, result)
                "suspicious" -> warn(pkg, key, result)
            }
        }
    }

    private fun block(pkg: String, key: String, result: UrlCheckResult) {
        // 판정을 기다리는 동안 다른 곳으로 옮겼으면 뒤로 가지 않는다
        if (currentKey()?.second != key) return
        performGlobalAction(GLOBAL_ACTION_BACK)
        blockedKey = key
        blockedAt = System.currentTimeMillis()
        lastBackAt = blockedAt
        Log.i(TAG, "위험한 주소를 막음: ${UrlExtractor.hostOf(key)}")

        val record = record(pkg, key, result, LinkRecord.Action.BLOCKED)
        LinkBlockedActivity.start(this, record)
        GuardNotifier.notifyLink(this, record)
    }

    private fun warn(pkg: String, key: String, result: UrlCheckResult) {
        if (LinkStore.recentlyRecorded(key, WARN_REPEAT_MS)) return
        GuardNotifier.notifyLink(this, record(pkg, key, result, LinkRecord.Action.WARNED))
    }

    private fun record(pkg: String, key: String, result: UrlCheckResult, action: LinkRecord.Action): LinkRecord {
        val record = LinkRecord(
            visitedAt = System.currentTimeMillis(),
            url = key,
            host = result.host ?: UrlExtractor.hostOf(key),
            app = appLabel(this, pkg),
            action = action,
            category = result.category,
            reason = result.reason,
        )
        LinkStore.add(record)
        RecordSync.syncSoon()
        return record
    }

    companion object {
        private const val TAG = "LinkGuard"
        private const val SCAN_DELAY_MS = 350L
        private const val SAME_URL_SKIP_MS = 60_000L
        private const val BAR_MEMORY_MS = 30_000L
        private const val BLOCKED_MEMORY_MS = 5 * 60_000L
        private const val BACK_INTERVAL_MS = 1_000L
        private const val WARN_REPEAT_MS = 60 * 60_000L
        private const val MAX_NODES = 400
        private const val MAX_DEPTH = 40

        /** 링크 지킴이가 켜져 있는지 */
        fun isEnabled(context: Context): Boolean {
            val am = context.getSystemService(AccessibilityManager::class.java) ?: return false
            return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
                it.resolveInfo.serviceInfo.packageName == context.packageName &&
                    it.resolveInfo.serviceInfo.name == LinkGuardService::class.java.name
            } || Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                .orEmpty().contains("${context.packageName}/${LinkGuardService::class.java.name}")
        }

        /** 보호자 화면에 보여줄 앱 이름 */
        fun appLabel(context: Context, pkg: String): String = when (pkg) {
            "com.android.chrome" -> "크롬"
            "com.sec.android.app.sbrowser" -> "삼성 인터넷"
            "com.nhn.android.search" -> "네이버"
            "com.naver.whale" -> "웨일"
            "com.kakao.talk" -> "카카오톡"
            "org.mozilla.firefox" -> "파이어폭스"
            "com.microsoft.emmx" -> "엣지"
            "com.google.android.googlequicksearchbox" -> "구글"
            else -> runCatching {
                val pm = context.packageManager
                @Suppress("DEPRECATION") // 새 방식(ApplicationInfoFlags)은 Android 13부터라 최소 버전(8.0)에서 못 씀
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrDefault(pkg)
        }
    }
}
