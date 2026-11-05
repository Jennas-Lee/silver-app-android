package io.github.jennaslee.silverapp.link

import android.content.Context
import android.util.Log
import io.github.jennaslee.silverapp.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import java.io.File
import java.util.UUID

/** 링크 지킴이가 막거나 경고한 주소 한 건 */
@Serializable
data class LinkRecord(
    val id: String = UUID.randomUUID().toString(),
    val visitedAt: Long,
    /** ? 뒤를 버린 주소 (도메인 + 경로) */
    val url: String,
    val host: String,
    /** 주소를 연 곳 (크롬, 카카오톡 등) */
    val app: String,
    val action: Action,
    val category: String,
    val reason: String,
    val uploaded: Boolean = false,
) {
    enum class Action {
        /** 위험해서 뒤로 돌아갔음 */
        BLOCKED,

        /** 의심스러워 경고만 했음 */
        WARNED,
    }
}

/** 막은 주소 목록. 문자 격리함(GuardStore)처럼 앱 전용 폴더의 JSON 파일 하나에 저장한다. */
object LinkStore {
    private const val MAX_RECORDS = 200
    private const val TAG = "LinkStore"

    private val _records = MutableStateFlow<List<LinkRecord>>(emptyList())
    val records: StateFlow<List<LinkRecord>> = _records

    private var file: File? = null

    @Synchronized
    fun init(context: Context) {
        if (file != null) return
        val f = File(context.applicationContext.filesDir, "link_records.json")
        file = f
        _records.value = runCatching {
            if (f.exists()) ApiClient.json.decodeFromString<List<LinkRecord>>(f.readText()) else emptyList()
        }.onFailure { Log.w(TAG, "막은 주소 읽기 실패", it) }.getOrDefault(emptyList())
    }

    @Synchronized
    fun add(record: LinkRecord) {
        save((listOf(record) + _records.value).take(MAX_RECORDS))
    }

    @Synchronized
    fun remove(id: String) {
        save(_records.value.filterNot { it.id == id })
    }

    @Synchronized
    fun markUploaded(ids: Set<String>) {
        if (ids.isEmpty()) return
        save(_records.value.map { if (it.id in ids) it.copy(uploaded = true) else it })
    }

    fun pendingUploads(): List<LinkRecord> = _records.value.filterNot { it.uploaded }

    /** 같은 주소를 최근에 이미 기록했는지 (경고를 너무 자주 남기지 않게) */
    fun recentlyRecorded(url: String, withinMillis: Long): Boolean {
        val since = System.currentTimeMillis() - withinMillis
        return _records.value.any { it.url == url && it.visitedAt > since }
    }

    private fun save(list: List<LinkRecord>) {
        _records.value = list
        runCatching { file?.writeText(ApiClient.json.encodeToString(list)) }
            .onFailure { Log.w(TAG, "막은 주소 저장 실패", it) }
    }
}
