package com.aiutil.videoplayer.data

import android.content.Context
import com.aiutil.videoplayer.model.PlaybackHistoryItem
import com.aiutil.videoplayer.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 동영상별 마지막 재생 위치(이어보기) 및 최근 재생 목록을 JSON 파일로 저장한다.
 */
class PlaybackHistoryStore(context: Context) {

    private val file = File(context.filesDir, "playback_history.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val lock = Any()

    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<PlaybackHistoryItem>> = _items.asStateFlow()

    private fun load(): List<PlaybackHistoryItem> {
        if (!file.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<List<PlaybackHistoryItem>>(file.readText())
                .sortedByDescending { it.lastPlayedAtMs }
        }.getOrDefault(emptyList())
    }

    private fun persist(list: List<PlaybackHistoryItem>) {
        runCatching {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(json.encodeToString(list))
            tmp.renameTo(file)
        }
    }

    fun getSavedPosition(idOrUri: String): Long {
        val item = _items.value.firstOrNull { it.id == idOrUri || it.uri == idOrUri || it.path == idOrUri }
            ?: return 0L
        // 영상 끝(98% 이후)에 도달했으면 처음부터 재생
        if (item.durationMs > 10_000L && item.positionMs >= item.durationMs * 0.98) {
            return 0L
        }
        return item.positionMs.coerceAtLeast(0L)
    }

    fun recordProgress(video: VideoItem, positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L && positionMs <= 0L) return
        val safeDuration = maxOf(durationMs, video.durationMs)
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val entry = PlaybackHistoryItem(
                id = video.id,
                title = video.title,
                uri = video.uri,
                path = video.path,
                folderPath = video.folderPath,
                positionMs = positionMs.coerceAtLeast(0L),
                durationMs = safeDuration,
                lastPlayedAtMs = now,
            )
            val updated = (listOf(entry) + _items.value.filterNot {
                it.id == video.id || (video.path.isNotBlank() && it.path == video.path)
            }).take(MAX_ITEMS)
            _items.value = updated
            persist(updated)
        }
    }

    fun removeByPathOrId(idOrPath: String) {
        synchronized(lock) {
            val updated = _items.value.filterNot {
                it.id == idOrPath || it.path == idOrPath || it.uri == idOrPath
            }
            _items.value = updated
            persist(updated)
        }
    }

    fun clearAll() {
        synchronized(lock) {
            _items.value = emptyList()
            persist(emptyList())
        }
    }

    private companion object {
        const val MAX_ITEMS = 200
    }
}
