package io.github.jennaslee.silverapp.guard

import android.content.Context
import android.util.Log
import io.github.jennaslee.silverapp.data.remote.ApiClient
import io.github.jennaslee.silverapp.data.remote.ClassifyRequest
import io.github.jennaslee.silverapp.session.SessionStore
import kotlinx.coroutines.withTimeout

/**
 * 메시지 한 건을 판정하고 처리한다. 문자 수신(SmsReceiver)과 알림 감시(SmsGuardService)가 함께 쓴다.
 *
 * 흐름: 1차 규칙 → 의심이면 개인정보를 가려 서버 판정
 *   → malicious: 알림을 치우고 격리함 + 경고 / suspicious: 경고 / safe: 그대로
 * 서버가 응답하지 못하면 규칙이 고위험일 때만 경고한다 (알림은 치우지 않음).
 */
object MessageGuard {
    private const val TAG = "SmsGuard"
    private const val SERVER_TIMEOUT_MS = 15_000L
    private const val RULE_ONLY_REASON =
        "모르는 사람이 보낸 주소나 돈 이야기가 있어요. 누르지 마시고 가족에게 먼저 물어보세요."

    /**
     * @param hideNotification malicious 판정 시 원래 알림을 치우는 방법
     */
    suspend fun process(context: Context, msg: IncomingMessage, hideNotification: suspend () -> Unit) {
        // 보호자 휴대폰, 보호자와 연결되지 않은 어르신 휴대폰에서는 쓰지 않는다
        if (!SessionStore.canUseFeatures) return
        GuardStore.init(context)
        if (GuardStore.contains(msg.sender, msg.body)) return

        val rule = MessageRules.check(msg)
        Log.d(TAG, "규칙 결과 ${msg.source} ${rule.level} score=${rule.score} hits=${rule.hits}")
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
        }.onFailure { Log.w(TAG, "서버 판정 실패, 규칙 결과로 처리: ${it.message}") }.getOrNull()

        val record = when {
            verdict?.verdict == "malicious" -> {
                hideNotification()
                newRecord(msg, GuardRecord.Action.HIDDEN, verdict.category, verdict.reason)
            }
            verdict?.verdict == "suspicious" ->
                newRecord(msg, GuardRecord.Action.WARNED, verdict.category, verdict.reason)
            verdict == null && rule.level == RuleResult.Level.HIGH_RISK ->
                newRecord(msg, GuardRecord.Action.WARNED, "rule", RULE_ONLY_REASON)
            else -> null
        } ?: return

        GuardStore.add(record)
        GuardNotifier.notify(context, record)
        RecordSync.syncSoon()
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
}
