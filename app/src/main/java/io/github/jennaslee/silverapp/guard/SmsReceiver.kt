package io.github.jennaslee.silverapp.guard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 문자가 도착하면 원문을 직접 받는다.
 * Android 15부터는 숫자가 든 문자 알림을 다른 앱이 읽지 못하게 가리기 때문에,
 * 문자는 알림 대신 이 방법으로 내용을 확인한다. 알림을 치우는 일은 SmsGuardService가 한다.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val receivedAt = System.currentTimeMillis()

        // 긴 문자는 여러 조각으로 오므로 보낸 사람별로 이어 붙인다
        val messages = parts
            .groupBy { it.displayOriginatingAddress ?: it.originatingAddress ?: "" }
            .filterKeys { it.isNotEmpty() }
            .mapValues { (_, list) -> list.joinToString("") { it.displayMessageBody ?: "" }.trim() }
            .filterValues { it.isNotEmpty() }
        if (messages.isEmpty()) return

        val pending = goAsync()
        scope.launch {
            try {
                for ((sender, body) in messages) {
                    Log.d(TAG, "문자 수신: from=$sender")
                    val msg = IncomingMessage(
                        source = IncomingMessage.Source.SMS,
                        sender = sender,
                        body = body,
                        senderInContacts = SmsGuardService.lookupContact(context, sender),
                    )
                    MessageGuard.process(context, msg) {
                        SmsGuardService.hideSmsNotification(sender, body, receivedAt)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SmsGuard"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
