package com.aiutil.videoplayer.files

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.aiutil.videoplayer.model.SortOrder
import com.aiutil.videoplayer.model.VideoFolder
import com.aiutil.videoplayer.model.VideoItem
import java.io.File
import java.util.Locale

/**
 * 기기 저장소의 동영상 파일 및 폴더 목록을 스캔하고 정렬한다.
 */
object VideoScanner {

    val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp", "ts", "mts", "m2ts", "mpg", "mpeg"
    )

    val SUBTITLE_EXTENSIONS = setOf("srt", "vtt", "ass", "ssa")

    fun isVideoFile(file: File): Boolean =
        file.isFile && file.extension.lowercase(Locale.US) in VIDEO_EXTENSIONS

    fun isSubtitleFile(file: File): Boolean =
        file.isFile && file.extension.lowercase(Locale.US) in SUBTITLE_EXTENSIONS

    fun scanVideos(context: Context): List<VideoItem> {
        val byKey = scanMediaStoreOnly(context)
        val rootDir = Environment.getExternalStorageDirectory()
        if (rootDir != null && rootDir.canRead()) {
            scanDirectoryShallowAndSafe(rootDir, byKey, maxDepth = 5)
        }
        return byKey.values.toList()
    }

    /** MediaStore 인덱스만 읽어 즉시 반환한다 (초기 화면 및 폴더 진입 속도 최적화). */
    fun scanVideosFast(context: Context): List<VideoItem> =
        scanMediaStoreOnly(context).values.toList()

    /** MediaStore에 아직 등록되지 않은 파일만 추가 탐색한다. */
    fun scanStorageExtras(context: Context, existing: List<VideoItem>): List<VideoItem> {
        val byKey = LinkedHashMap<String, VideoItem>(existing.size)
        existing.forEach { byKey[it.id] = it }
        val rootDir = Environment.getExternalStorageDirectory()
        if (rootDir != null && rootDir.canRead()) {
            scanDirectoryShallowAndSafe(rootDir, byKey, maxDepth = 5)
        }
        return byKey.values.toList()
    }

    private fun scanMediaStoreOnly(context: Context): LinkedHashMap<String, VideoItem> {
        val byKey = LinkedHashMap<String, VideoItem>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        )

        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val modCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, mediaId).toString()
                    val filePath = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""

                    val file = if (filePath.isNotBlank()) File(filePath) else null
                    val title = cursor.getString(nameCol)?.takeIf { it.isNotBlank() }
                        ?: file?.name
                        ?: "video_$mediaId"
                    val folderPath = file?.parent ?: "/MediaStore/${cursor.getString(bucketCol).orEmpty()}"
                    val folderName = file?.parentFile?.name?.takeIf { it.isNotBlank() }
                        ?: (if (bucketCol >= 0) cursor.getString(bucketCol) else null)
                        ?: "내부 저장소"
                    val key = filePath.ifBlank { contentUri }

                    byKey[key] = VideoItem(
                        id = key,
                        title = title,
                        uri = contentUri,
                        path = filePath,
                        folderPath = folderPath,
                        folderName = folderName,
                        sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L),
                        durationMs = cursor.getLong(durationCol).coerceAtLeast(0L),
                        dateModifiedSec = cursor.getLong(modCol).coerceAtLeast(0L),
                        width = if (widthCol >= 0) cursor.getInt(widthCol) else 0,
                        height = if (heightCol >= 0) cursor.getInt(heightCol) else 0,
                        mimeType = (if (mimeCol >= 0) cursor.getString(mimeCol) else null) ?: "video/*",
                    )
                }
            }
        }
        return byKey
    }

    private fun scanDirectoryShallowAndSafe(
        dir: File,
        out: MutableMap<String, VideoItem>,
        maxDepth: Int,
        depth: Int = 0,
    ) {
        if (depth > maxDepth) return
        val name = dir.name
        if (name.startsWith(".") || name == "Android") return
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                scanDirectoryShallowAndSafe(child, out, maxDepth, depth + 1)
            } else if (isVideoFile(child) && !out.containsKey(child.absolutePath)) {
                out[child.absolutePath] = videoItemFromFile(child)
            }
        }
    }

    fun videoItemFromFile(file: File, extractMetadata: Boolean = false): VideoItem {
        var duration = 0L
        var width = 0
        var height = 0
        if (extractMetadata) {
            val retriever = MediaMetadataRetriever()
            runCatching {
                retriever.setDataSource(file.absolutePath)
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 0
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 0
            }
            runCatching { retriever.release() }
        }
        return VideoItem(
            id = file.absolutePath,
            title = file.name,
            uri = runCatching { Uri.fromFile(file).toString() }.getOrElse { file.toURI().toString() },
            path = file.absolutePath,
            folderPath = file.parent ?: "/",
            folderName = file.parentFile?.name?.takeIf { it.isNotBlank() } ?: "내부 저장소",
            sizeBytes = file.length(),
            durationMs = duration,
            dateModifiedSec = file.lastModified() / 1000L,
            width = width,
            height = height,
        )
    }

    fun groupByFolder(videos: List<VideoItem>, sortOrder: SortOrder): List<VideoFolder> {
        val grouped = videos.groupBy { it.folderPath }
        val folders = grouped.map { (folderPath, items) ->
            val sortedItems = sortVideos(items, sortOrder)
            VideoFolder(
                path = folderPath,
                name = sortedItems.firstOrNull()?.folderName ?: File(folderPath).name.ifBlank { folderPath },
                videos = sortedItems,
                totalSizeBytes = sortedItems.sumOf { it.sizeBytes },
                lastModifiedSec = sortedItems.maxOfOrNull { it.dateModifiedSec } ?: 0L,
            )
        }
        return when (sortOrder) {
            SortOrder.NAME_ASC -> folders.sortedBy { it.name.lowercase(Locale.getDefault()) }
            SortOrder.NAME_DESC -> folders.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
            SortOrder.DATE_DESC -> folders.sortedByDescending { it.lastModifiedSec }
            SortOrder.SIZE_DESC -> folders.sortedByDescending { it.totalSizeBytes }
            SortOrder.DURATION_DESC -> folders.sortedByDescending { it.videos.sumOf { v -> v.durationMs } }
        }
    }

    fun sortVideos(videos: List<VideoItem>, sortOrder: SortOrder): List<VideoItem> {
        return when (sortOrder) {
            SortOrder.NAME_ASC -> videos.sortedBy { it.title.lowercase(Locale.getDefault()) }
            SortOrder.NAME_DESC -> videos.sortedByDescending { it.title.lowercase(Locale.getDefault()) }
            SortOrder.DATE_DESC -> videos.sortedByDescending { it.dateModifiedSec }
            SortOrder.SIZE_DESC -> videos.sortedByDescending { it.sizeBytes }
            SortOrder.DURATION_DESC -> videos.sortedByDescending { it.durationMs }
        }
    }

    /** 특정 폴더 내 모든 자막 파일의 기본 이름(소문자)을 한 번의 I/O로 수집한다. */
    fun scanSubtitleBaseNamesInFolder(folderPath: String): Set<String> {
        if (folderPath.isBlank()) return emptySet()
        val dir = File(folderPath)
        if (!dir.isDirectory) return emptySet()
        val files = dir.listFiles() ?: return emptySet()
        val bases = HashSet<String>()
        for (f in files) {
            if (isSubtitleFile(f)) {
                val noExt = f.nameWithoutExtension.lowercase(Locale.US)
                bases.add(noExt)
                val dot = noExt.indexOf('.')
                if (dot > 0) bases.add(noExt.substring(0, dot))
            }
        }
        return bases
    }

    /** 동영상 파일과 같은 폴더에 있는 동반 자막 파일(.srt/.vtt/.ass) 목록을 우선순위대로 반환한다. */
    fun findCompanionSubtitles(videoPath: String): List<File> {
        if (videoPath.isBlank()) return emptyList()
        val videoFile = File(videoPath)
        val dir = videoFile.parentFile ?: return emptyList()
        if (!dir.isDirectory) return emptyList()
        val base = videoFile.nameWithoutExtension
        val files = dir.listFiles().orEmpty().filter { isSubtitleFile(it) }
        val exactOrLang = files.filter { sub ->
            val subName = sub.nameWithoutExtension
            subName.equals(base, ignoreCase = true) ||
                subName.startsWith("$base.", ignoreCase = true)
        }
        return exactOrLang.sortedWith(
            compareBy<File>(
                { !it.nameWithoutExtension.equals("$base.ko", ignoreCase = true) },
                { !it.nameWithoutExtension.equals(base, ignoreCase = true) },
                { it.name }
            )
        )
    }
}
