package io.github.jennaslee.silverapp.guard

import android.util.Log
import io.github.jennaslee.silverapp.data.remote.ApiClient
import io.github.jennaslee.silverapp.data.remote.RecordDto
import io.github.jennaslee.silverapp.data.remote.UploadRecordsRequest
import io.github.jennaslee.silverapp.data.remote.UploadUrlRecordsRequest
import io.github.jennaslee.silverapp.data.remote.UrlRecordDto
import io.github.jennaslee.silverapp.link.LinkRecord
import io.github.jennaslee.silverapp.link.LinkStore
import io.github.jennaslee.silverapp.session.Session
import io.github.jennaslee.silverapp.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/**
 * 어르신 휴대폰의 차단·경고 기록(문자, 막은 주소)을 서버에 올려 보호자가 볼 수 있게 한다.
 * 새 기록이 생길 때와 앱이 켜질 때 아직 안 올린 기록을 올린다. 실패하면 다음 기회에 다시 올린다.
 * 본문은 개인정보를 가려서 보낸다 (서버에서도 한 번 더 가림).
 */
object RecordSync {
    private const val TAG = "RecordSync"
    private const val BATCH = 100

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val isSenior: Boolean get() = SessionStore.current?.role == Session.Role.SENIOR

    fun syncSoon() {
        if (!isSenior) return
        scope.launch { sync() }
    }

    suspend fun sync() = mutex.withLock {
        if (!isSenior) return@withLock
        syncLinks()
        val pending = GuardStore.pendingUploads()
        if (pending.isEmpty()) return@withLock
        for (chunk in pending.chunked(BATCH)) {
            runCatching {
                ApiClient.api.uploadRecords(UploadRecordsRequest(chunk.map { it.toDto() }))
            }.onSuccess {
                GuardStore.markUploaded(chunk.map { it.id }.toSet())
                Log.d(TAG, "기록 ${chunk.size}건 올림 (새로 저장 ${it.saved})")
            }.onFailure {
                Log.w(TAG, "기록 올리기 실패, 다음에 다시 시도: ${it.message}")
                return@withLock
            }
        }
    }

    private suspend fun syncLinks() {
        for (chunk in LinkStore.pendingUploads().chunked(BATCH)) {
            runCatching {
                ApiClient.api.uploadUrlRecords(UploadUrlRecordsRequest(chunk.map { it.toDto() }))
            }.onSuccess {
                LinkStore.markUploaded(chunk.map { it.id }.toSet())
                Log.d(TAG, "막은 주소 ${chunk.size}건 올림 (새로 저장 ${it.saved})")
            }.onFailure {
                Log.w(TAG, "막은 주소 올리기 실패, 다음에 다시 시도: ${it.message}")
                return
            }
        }
    }

    /** 어르신이 목록에서 지운 주소 기록은 서버에서도 지운다 */
    fun deleteLinkSoon(id: String) {
        if (!isSenior) return
        scope.launch {
            runCatching { ApiClient.api.deleteUrlRecord(id) }
                .onFailure { Log.w(TAG, "서버 주소 기록 삭제 실패: ${it.message}") }
        }
    }

    /** 어르신이 목록에서 지운 기록은 서버에서도 지운다 (실패해도 앱 동작에는 영향 없음) */
    fun deleteSoon(id: String) {
        if (!isSenior) return
        scope.launch {
            runCatching { ApiClient.api.deleteRecord(id) }
                .onFailure { Log.w(TAG, "서버 기록 삭제 실패: ${it.message}") }
        }
    }

    private fun LinkRecord.toDto() = UrlRecordDto(
        id = id,
        visitedAt = Instant.ofEpochMilli(visitedAt).toString(),
        host = host,
        url = url,
        app = app,
        action = action.name,
        category = category,
        reason = reason,
    )

    private fun GuardRecord.toDto() = RecordDto(
        id = id,
        receivedAt = Instant.ofEpochMilli(receivedAt).toString(),
        source = source,
        sender = sender,
        body = PiiMasker.mask(body),
        action = action.name,
        category = category,
        reason = reason,
    )
}
