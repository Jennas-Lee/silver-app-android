package io.github.jennaslee.silverapp.guard

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.github.jennaslee.silverapp.data.remote.ApiClient
import io.github.jennaslee.silverapp.data.remote.ClassifyRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * 문자 앱과 카카오톡 알림을 보고 사기 메시지를 걸러낸다.
 *
 * 흐름: 알림 → 1차 규칙(MessageRules) → 의심이면 개인정보를 가려 서버 판정
 *   → malicious: 알림을 치우고 격리함 + 경고 / suspicious: 경고 / safe: 그대로
 * 서버가 응답하지 못하면 규칙이 고위험일 때만 경고한다 (알림은 치우지 않음).
 *
 * 주의: 알림만 치울 수 있고, 문자 앱 안의 원본 메시지는 남아 있다.
 */
class SmsGuardService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 같은 알림이 여러 번 갱신되어 들어오므로 최근 처리한 메시지는 건너뛴다
    private val recent = object : LinkedHashMap<Int, Unit>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Unit>?) = size > 100
    }

    override fun onCreate() {
        super.onCreate()
        GuardStore.init(this)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val source = sourceOf(sbn.packageName) ?: return
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val (sender, body) = extract(n) ?: return
        val dedupKey = (sbn.packageName + sender + body).hashCode()
        synchronized(recent) {
            if (recent.containsKey(dedupKey)) return
            recent[dedupKey] = Unit
        }

        val msg = IncomingMessage(source, sender, body, senderInContacts = isInContacts(source, sender))
        scope.launch { handle(sbn, msg) }
    }

    private suspend fun handle(sbn: StatusBarNotification, msg: IncomingMessage) {
        val rule = MessageRules.check(msg)
        Log.d(TAG, "규칙 결과 ${rule.level} score=${rule.score} hits=${rule.hits}")
        if (rule.level == RuleResult.Level.SAFE) return

        val verdict = runCatching {
            withTimeout(SERVER_TIMEOUT_MS) {
                ApiClient.api.classifyMessage(
                    ClassifyRequest(
                        source = msg.source.apiValue,
                        sender = msg.sender,
                        senderInContacts = msg.senderInContacts,
                        body = PiiMasker.mask(msg.body),
                        ruleHits = rule.hits,
                    ),
                )
            }
        }.onFailure { Log.w(TAG, "서버 판정 실패, 규칙 결과로 처리", it) }.getOrNull()

        val record = when {
            verdict?.verdict == "malicious" -> {
                cancelNotification(sbn.key)
                newRecord(msg, GuardRecord.Action.HIDDEN, verdict.category, verdict.reason)
            }
            verdict?.verdict == "suspicious" ->
                newRecord(msg, GuardRecord.Action.WARNED, verdict.category, verdict.reason)
            verdict == null && rule.level == RuleResult.Level.HIGH_RISK ->
                newRecord(msg, GuardRecord.Action.WARNED, "rule", RULE_ONLY_REASON)
            else -> null
        } ?: return

        GuardStore.add(record)
        GuardNotifier.notify(this, record)
    }

    private fun newRecord(msg: IncomingMessage, action: GuardRecord.Action, category: String, reason: String) =
        GuardRecord(
            receivedAt = System.currentTimeMillis(),
            source = msg.source.apiValue,
            sender = msg.sender,
            body = msg.body,
            action = action,
            category = category,
            reason = reason,
        )

    private fun sourceOf(pkg: String): IncomingMessage.Source? = when {
        pkg == packageName -> null
        pkg == KAKAO_PACKAGE -> IncomingMessage.Source.KAKAO
        pkg in SMS_PACKAGES || pkg == Telephony.Sms.getDefaultSmsPackage(this) -> IncomingMessage.Source.SMS
        else -> null
    }

    /** 알림에서 보낸 사람과 본문을 꺼낸다 */
    private fun extract(n: Notification): Pair<String, String>? {
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
        val last = style?.messages?.lastOrNull()
        val extras = n.extras
        val sender = (last?.person?.name ?: extras.getCharSequence(Notification.EXTRA_TITLE))?.toString()?.trim()
        val body = (
            last?.text
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
            )?.toString()?.trim()
        if (sender.isNullOrEmpty() || body.isNullOrEmpty()) return null
        return sender to body
    }

    /**
     * 문자 앱은 연락처에 있는 사람이면 번호 대신 이름을 보여준다.
     * 번호로 보이면 연락처 권한이 있을 때 직접 찾아본다.
     */
    private fun isInContacts(source: IncomingMessage.Source, sender: String): Boolean {
        if (source != IncomingMessage.Source.SMS) return false
        val digits = sender.filter { it.isDigit() }
        val looksLikeNumber = digits.length >= 7 && digits.length >= sender.count { !it.isWhitespace() } - 3
        if (!looksLikeNumber) return true
        return lookupContact(this, sender)
    }

    companion object {
        private const val TAG = "SmsGuard"
        private const val SERVER_TIMEOUT_MS = 15_000L
        private const val KAKAO_PACKAGE = "com.kakao.talk"
        private val SMS_PACKAGES = setOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms",
            "com.android.messaging",
        )
        private const val RULE_ONLY_REASON =
            "모르는 사람이 보낸 주소나 돈 이야기가 있어요. 누르지 마시고 가족에게 먼저 물어보세요."

        fun lookupContact(context: Context, number: String): Boolean {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            return runCatching {
                context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
                    ?.use { it.count > 0 } ?: false
            }.getOrDefault(false)
        }
    }
}
