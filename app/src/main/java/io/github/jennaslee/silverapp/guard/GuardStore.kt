package io.github.jennaslee.silverapp.guard

import android.content.Context
import android.util.Log
import io.github.jennaslee.silverapp.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import java.io.File
import java.util.UUID

/** 지킴이가 치우거나 경고한 메시지 한 건 */
@Serializable
data class GuardRecord(
    val id: String = UUID.randomUUID().toString(),
    val receivedAt: Long,
    val source: String,
    val sender: String,
    /** 원문은 휴대폰 안에만 저장한다 (서버에는 가린 글만 보냄) */
    val body: String,
    val action: Action,
    val category: String,
    val reason: String,
) {
    enum class Action {
        /** 서버가 사기로 판정해 알림을 치웠음 */
        HIDDEN,

        /** 의심스러워 경고만 했음 */
        WARNED,
    }
}

/**
 * 격리함. PoC 단계라 앱 전용 폴더의 JSON 파일 하나에 저장한다.
 * 기록이 많아지면 Room으로 바꾼다.
 */
object GuardStore {
    private const val MAX_RECORDS = 200
    private const val TAG = "GuardStore"

    private val _records = MutableStateFlow<List<GuardRecord>>(emptyList())
    val records: StateFlow<List<GuardRecord>> = _records

    private var file: File? = null

    @Synchronized
    fun init(context: Context) {
        if (file != null) return
        val f = File(context.applicationContext.filesDir, "guard_records.json")
        file = f
        _records.value = runCatching {
            if (f.exists()) ApiClient.json.decodeFromString<List<GuardRecord>>(f.readText()) else emptyList()
        }.onFailure { Log.w(TAG, "격리함 읽기 실패", it) }.getOrDefault(emptyList())
    }

    @Synchronized
    fun add(record: GuardRecord) {
        save((listOf(record) + _records.value).take(MAX_RECORDS))
    }

    @Synchronized
    fun remove(id: String) {
        save(_records.value.filterNot { it.id == id })
    }

    fun find(id: String): GuardRecord? = _records.value.firstOrNull { it.id == id }

    private fun save(list: List<GuardRecord>) {
        _records.value = list
        runCatching { file?.writeText(ApiClient.json.encodeToString(list)) }
            .onFailure { Log.w(TAG, "격리함 저장 실패", it) }
    }
}
