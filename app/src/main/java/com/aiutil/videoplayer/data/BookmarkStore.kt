package com.aiutil.videoplayer.data

import com.aiutil.videoplayer.model.FavoriteFolderItem
import com.aiutil.videoplayer.model.FavoriteVideoItem
import com.aiutil.videoplayer.model.VideoBookmarkItem
import com.aiutil.videoplayer.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
private data class BookmarkSnapshot(
    val favoriteVideos: List<FavoriteVideoItem> = emptyList(),
    val favoriteFolders: List<FavoriteFolderItem> = emptyList(),
    val videoBookmarks: List<VideoBookmarkItem> = emptyList(),
)

/**
 * 동영상/폴더 즐겨찾기 및 동영상 특정 재생 시점(타임스탬프) 북마크를 JSON 파일로 영속화한다.
 */
class BookmarkStore(private val storageFile: File) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val lock = Any()

    private val initial = load()
    private val _favoriteVideos = MutableStateFlow(initial.favoriteVideos)
    val favoriteVideos: StateFlow<List<FavoriteVideoItem>> = _favoriteVideos.asStateFlow()

    private val _favoriteFolders = MutableStateFlow(initial.favoriteFolders)
    val favoriteFolders: StateFlow<List<FavoriteFolderItem>> = _favoriteFolders.asStateFlow()

    private val _videoBookmarks = MutableStateFlow(initial.videoBookmarks)
    val videoBookmarks: StateFlow<List<VideoBookmarkItem>> = _videoBookmarks.asStateFlow()

    private fun load(): BookmarkSnapshot {
        if (!storageFile.exists()) return BookmarkSnapshot()
        return runCatching {
            json.decodeFromString<BookmarkSnapshot>(storageFile.readText(Charsets.UTF_8))
        }.getOrDefault(BookmarkSnapshot())
    }

    private fun persistLocked() {
        runCatching {
            storageFile.parentFile?.mkdirs()
            val snapshot = BookmarkSnapshot(
                favoriteVideos = _favoriteVideos.value,
                favoriteFolders = _favoriteFolders.value,
                videoBookmarks = _videoBookmarks.value,
            )
            val tmp = File(storageFile.parentFile, "${storageFile.name}.tmp")
            tmp.writeText(json.encodeToString(snapshot), Charsets.UTF_8)
            tmp.renameTo(storageFile)
        }
    }

    fun isFavoriteVideo(id: String, path: String = ""): Boolean =
        _favoriteVideos.value.any {
            it.id == id || (path.isNotBlank() && it.path == path)
        }

    fun toggleFavoriteVideo(video: VideoItem): Boolean = synchronized(lock) {
        val exists = isFavoriteVideo(video.id, video.path)
        if (exists) {
            _favoriteVideos.value = _favoriteVideos.value.filterNot {
                it.id == video.id || (video.path.isNotBlank() && it.path == video.path)
            }
            persistLocked()
            false
        } else {
            val item = FavoriteVideoItem(
                id = video.id,
                title = video.title,
                uri = video.uri,
                path = video.path,
                folderPath = video.folderPath,
                folderName = video.folderName,
                durationMs = video.durationMs,
                sizeBytes = video.sizeBytes,
                addedAtMs = System.currentTimeMillis(),
            )
            _favoriteVideos.value = listOf(item) + _favoriteVideos.value
            persistLocked()
            true
        }
    }

    fun removeFavoriteVideo(idOrPath: String) = synchronized(lock) {
        _favoriteVideos.value = _favoriteVideos.value.filterNot {
            it.id == idOrPath || it.path == idOrPath || it.uri == idOrPath
        }
        persistLocked()
    }

    fun isFavoriteFolder(path: String): Boolean =
        path.isNotBlank() && _favoriteFolders.value.any { it.path == path }

    fun toggleFavoriteFolder(path: String, name: String): Boolean = synchronized(lock) {
        if (path.isBlank()) return@synchronized false
        val exists = isFavoriteFolder(path)
        if (exists) {
            _favoriteFolders.value = _favoriteFolders.value.filterNot { it.path == path }
            persistLocked()
            false
        } else {
            val item = FavoriteFolderItem(
                path = path,
                name = name.ifBlank { File(path).name.ifBlank { path } },
                addedAtMs = System.currentTimeMillis(),
            )
            _favoriteFolders.value = listOf(item) + _favoriteFolders.value
            persistLocked()
            true
        }
    }

    fun removeFavoriteFolder(path: String) = synchronized(lock) {
        _favoriteFolders.value = _favoriteFolders.value.filterNot { it.path == path }
        persistLocked()
    }

    fun addVideoBookmark(
        video: VideoItem,
        positionMs: Long,
        note: String = "",
    ): VideoBookmarkItem = synchronized(lock) {
        val entry = VideoBookmarkItem(
            bookmarkId = UUID.randomUUID().toString(),
            videoId = video.id,
            videoTitle = video.title,
            uri = video.uri,
            path = video.path,
            folderPath = video.folderPath,
            positionMs = positionMs.coerceAtLeast(0L),
            note = note.trim(),
            createdAtMs = System.currentTimeMillis(),
        )
        _videoBookmarks.value = listOf(entry) + _videoBookmarks.value
        persistLocked()
        entry
    }

    fun removeVideoBookmark(bookmarkId: String) = synchronized(lock) {
        _videoBookmarks.value = _videoBookmarks.value.filterNot { it.bookmarkId == bookmarkId }
        persistLocked()
    }

    fun bookmarksForVideo(videoId: String, videoPath: String): List<VideoBookmarkItem> =
        _videoBookmarks.value
            .filter { it.videoId == videoId || (videoPath.isNotBlank() && it.path == videoPath) }
            .sortedBy { it.positionMs }

    fun removeForDeletedPath(idOrPath: String) = synchronized(lock) {
        if (idOrPath.isBlank()) return@synchronized
        _favoriteVideos.value = _favoriteVideos.value.filterNot {
            it.id == idOrPath || it.path == idOrPath || it.uri == idOrPath
        }
        _favoriteFolders.value = _favoriteFolders.value.filterNot {
            it.path == idOrPath || it.path.startsWith("$idOrPath/")
        }
        _videoBookmarks.value = _videoBookmarks.value.filterNot {
            it.videoId == idOrPath || it.path == idOrPath || it.uri == idOrPath
        }
        persistLocked()
    }
}
