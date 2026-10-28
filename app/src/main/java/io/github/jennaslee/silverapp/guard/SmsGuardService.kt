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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 알림 감시.
 * - 카카오톡: 알림 내용으로 판정한다.
 * - 문자: 문자 수신 권한이 있으면 SmsReceiver가 원문으로 판정하고, 여기서는 사기 문자 알림을 치우기만 한다.
 *   권한이 없으면 알림 내용으로 판정한다 (Android 15+에서는 숫자가 든 문자 알림이 가려져 놓칠 수 있음).
 *
 * 주의: 알림만 치울 수 있고, 문자함·카톡방 안의 원본 메시지는 남아 있다.
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

    override fun onListenerConnected() {
        instance = this
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val source = sourceOf(this, sbn.packageName) ?: return
        if (source == IncomingMessage.Source.SMS && hasReceiveSms(this)) return // SmsReceiver가 처리

        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val (sender, body) = extract(n) ?: run {
            Log.d(TAG, "알림 내용을 읽지 못함 (가려졌을 수 있음): ${sbn.packageName}")
            return
        }
        val dedupKey = (sbn.packageName + sender + body).hashCode()
        synchronized(recent) {
            if (recent.containsKey(dedupKey)) return
            recent[dedupKey] = Unit
        }
        Log.d(TAG, "알림 수신: $source from=$sender")

        val msg = IncomingMessage(source, sender, body, senderInContacts = isInContacts(source, sender))
        scope.launch {
            MessageGuard.process(this@SmsGuardService, msg) { cancelNotification(sbn.key) }
        }
    }

    /** SmsReceiver가 사기로 판정한 문자의 알림을 찾아 치운다 */
    private suspend fun hideMatching(sender: String, body: String, receivedAt: Long): Boolean {
        val senderDigits = sender.filter { it.isDigit() }.takeLast(8)
        val bodyHead = body.take(20)
        // 문자 앱이 알림을 늦게 올릴 수 있어 몇 번 다시 찾아본다
        repeat(3) { attempt ->
            val targets = activeNotifications.orEmpty().filter { sbn ->
                sourceOf(this, sbn.packageName) == IncomingMessage.Source.SMS &&
                    sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 &&
                    sbn.postTime >= receivedAt - 60_000 &&
                    matches(sbn, senderDigits, bodyHead, receivedAt)
            }
            if (targets.isNotEmpty()) {
                targets.forEach { cancelNotification(it.key) }
                Log.d(TAG, "사기 문자 알림 ${targets.size}개를 치움")
                return true
            }
            if (attempt < 2) delay(2_000)
        }
        Log.d(TAG, "치울 문자 알림을 찾지 못함")
        return false
    }

    /** 내용이 보이면 본문으로, 가려졌으면 보낸 사람 번호로 같은 문자인지 확인한다 */
    private fun matches(sbn: StatusBarNotification, senderDigits: String, bodyHead: String, receivedAt: Long): Boolean {
        val extracted = extract(sbn.notification)
        if (extracted != null) {
            val (s, b) = extracted
            return b.contains(bodyHead) || (senderDigits.isNotEmpty() && s.filter { it.isDigit() }.endsWith(senderDigits))
        }
        // 내용이 가려진 알림: 알림에 적힌 문자 도착 시각이 15초 안인 것만 같은 문자로 본다.
        // (postTime은 문자 앱이 예전 알림을 다시 올릴 때마다 바뀌므로 쓰지 않는다)
        return kotlin.math.abs(sbn.notification.`when` - receivedAt) < 15_000
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
        private const val KAKAO_PACKAGE = "com.kakao.talk"
        private val SMS_PACKAGES = setOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms",
            "com.android.messaging",
        )

        @Volatile
        private var instance: SmsGuardService? = null

        suspend fun hideSmsNotification(sender: String, body: String, receivedAt: Long) {
            val service = instance ?: run {
                Log.d(TAG, "알림 감시가 꺼져 있어 알림을 치우지 못함")
                return
            }
            service.hideMatching(sender, body, receivedAt)
        }

        fun sourceOf(context: Context, pkg: String): IncomingMessage.Source? = when {
            pkg == context.packageName -> null
            pkg == KAKAO_PACKAGE -> IncomingMessage.Source.KAKAO
            pkg in SMS_PACKAGES || pkg == Telephony.Sms.getDefaultSmsPackage(context) -> IncomingMessage.Source.SMS
            else -> null
        }

        fun hasReceiveSms(context: Context) =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

        /** 알림에서 보낸 사람과 본문을 꺼낸다. 내용이 가려졌으면 null */
        fun extract(n: Notification): Pair<String, String>? {
            val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
            val last = style?.messages?.lastOrNull()
            val extras = n.extras
            // 모르는 번호는 이름이 빈 글자로 오기도 하므로 빈 값은 건너뛰고 다음 후보를 쓴다
            fun CharSequence?.clean() = this?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val sender = last?.person?.name.clean()
                ?: style?.conversationTitle.clean()
                ?: extras.getCharSequence(Notification.EXTRA_TITLE).clean()
            val body = last?.text.clean()
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT).clean()
                ?: extras.getCharSequence(Notification.EXTRA_TEXT).clean()
            if (sender == null || body == null) return null
            return sender to body
        }

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
