package com.aiutil.videoplayer.files

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import com.aiutil.videoplayer.model.ClipboardOp
import com.aiutil.videoplayer.model.ExplorerEntry
import com.aiutil.videoplayer.model.SortOrder
import com.aiutil.videoplayer.model.VideoItem
import java.io.File
import java.util.Locale

/**
 * 폴더 탐색기 파일/폴더 관리(목록 조회, 새 폴더, 이름 변경, 복사, 이동, 삭제) 엔진.
 * 동영상을 이동·복사·이름변경·삭제할 때 동반 자막 파일도 함께 처리할 수 있다.
 */
object FileExplorerManager {

    fun listDirectory(
        dir: File,
        sortOrder: SortOrder,
        videoIndex: Map<String, VideoItem> = emptyMap(),
    ): List<ExplorerEntry> {
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        val raw = dir.listFiles() ?: return emptyList()
        val entries = raw.mapNotNull { file ->
            if (file.name.startsWith(".")) return@mapNotNull null
            if (file.isDirectory) {
                val childCount = file.listFiles()?.count { !it.name.startsWith(".") } ?: 0
                ExplorerEntry(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = true,
                    sizeBytes = 0L,
                    lastModifiedMs = file.lastModified(),
                    childCount = childCount,
                )
            } else {
                val isVideo = VideoScanner.isVideoFile(file)
                val isSubtitle = VideoScanner.isSubtitleFile(file)
                val videoItem = if (isVideo) {
                    videoIndex[file.absolutePath] ?: VideoScanner.videoItemFromFile(file)
                } else {
                    null
                }
                ExplorerEntry(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = false,
                    sizeBytes = file.length(),
                    lastModifiedMs = file.lastModified(),
                    isVideo = isVideo,
                    isSubtitle = isSubtitle,
                    videoItem = videoItem,
                )
            }
        }

        val dirs = entries.filter { it.isDirectory }
        val files = entries.filterNot { it.isDirectory }
        val comparator = entryComparator(sortOrder)
        return dirs.sortedWith(comparator) + files.sortedWith(comparator)
    }

    private fun entryComparator(sortOrder: SortOrder): Comparator<ExplorerEntry> = when (sortOrder) {
        SortOrder.NAME_ASC -> compareBy { it.name.lowercase(Locale.getDefault()) }
        SortOrder.NAME_DESC -> compareByDescending { it.name.lowercase(Locale.getDefault()) }
        SortOrder.DATE_DESC -> compareByDescending { it.lastModifiedMs }
        SortOrder.SIZE_DESC -> compareByDescending { it.sizeBytes }
        SortOrder.DURATION_DESC -> compareByDescending { it.videoItem?.durationMs ?: 0L }
    }

    fun createFolder(parentDir: File, name: String): Result<File> = runCatching {
        val clean = name.trim()
        require(clean.isNotEmpty() && !clean.contains('/') && !clean.contains('\\')) {
            "유효하지 않은 폴더 이름입니다."
        }
        val target = File(parentDir, clean)
        if (target.exists()) error("이미 같은 이름의 폴더 또는 파일이 존재합니다.")
        check(target.mkdirs()) { "폴더를 생성할 수 없습니다. 저장소 접근 권한을 확인해 주세요." }
        target
    }

    fun renameEntry(
        target: File,
        newName: String,
        context: Context? = null,
    ): Result<File> = runCatching {
        val clean = newName.trim()
        require(clean.isNotEmpty() && !clean.contains('/') && !clean.contains('\\')) {
            "유효하지 않은 이름입니다."
        }
        require(target.exists()) { "대상 항목이 존재하지 않습니다." }
        val parent = target.parentFile ?: error("상위 폴더를 찾을 수 없습니다.")
        val dest = File(parent, clean)
        if (dest.exists()) error("이미 같은 이름이 존재합니다.")

        val oldBase = target.nameWithoutExtension
        val newBase = File(clean).nameWithoutExtension
        val companions = if (target.isFile && VideoScanner.isVideoFile(target)) {
            VideoScanner.findCompanionSubtitles(target.absolutePath)
        } else {
            emptyList()
        }

        check(target.renameTo(dest)) { "이름을 변경할 수 없습니다. 저장소 권한을 확인해 주세요." }

        // 동반 자막 파일도 같은 접두어로 함께 변경
        companions.forEach { sub ->
            val suffix = sub.name.removePrefix(oldBase)
            val renamedSub = File(parent, newBase + suffix)
            if (!renamedSub.exists()) {
                sub.renameTo(renamedSub)
            }
        }

        if (context != null) {
            removeFromMediaStore(context, target.absolutePath)
            scanPaths(context, listOf(dest.absolutePath))
        }
        dest
    }

    fun copyOrMove(
        sources: List<File>,
        destDir: File,
        operation: ClipboardOp,
        context: Context? = null,
    ): Result<Int> = runCatching {
        require(destDir.exists() && destDir.isDirectory) { "대상 폴더가 올바르지 않습니다." }
        var processed = 0
        val pathsToScan = ArrayList<String>()
        val pathsToRemoveFromMediaStore = ArrayList<String>()

        for (src in sources) {
            if (!src.exists()) continue
            val srcCanonical = src.canonicalFile
            val destCanonical = destDir.canonicalFile

            if (src.isDirectory) {
                if (destCanonical == srcCanonical ||
                    destCanonical.path.startsWith(srcCanonical.path + File.separator)
                ) {
                    error("폴더를 자기 자신이나 하위 폴더로 ${operation.label}할 수 없습니다.")
                }
            } else if (operation == ClipboardOp.MOVE && src.parentFile?.canonicalFile == destCanonical) {
                // 이미 같은 폴더에 있는 파일 이동은 건너뜀
                continue
            }

            val targetFile = uniqueDestination(destDir, src.name)
            val companions = if (src.isFile && VideoScanner.isVideoFile(src)) {
                VideoScanner.findCompanionSubtitles(src.absolutePath)
            } else {
                emptyList()
            }

            when (operation) {
                ClipboardOp.COPY -> {
                    copyRecursively(src, targetFile)
                    collectFiles(targetFile, pathsToScan)
                }
                ClipboardOp.MOVE -> {
                    val movedDirectly = src.renameTo(targetFile)
                    if (!movedDirectly) {
                        copyRecursively(src, targetFile)
                        check(src.deleteRecursively()) { "원본을 삭제하지 못해 이동이 중단되었습니다." }
                    }
                    collectFiles(targetFile, pathsToScan)
                    pathsToRemoveFromMediaStore.add(src.absolutePath)
                }
            }

            // 동반 자막도 함께 복사/이동
            val oldBase = src.nameWithoutExtension
            val newBase = targetFile.nameWithoutExtension
            for (sub in companions) {
                val subSuffix = sub.name.removePrefix(oldBase)
                val destSub = uniqueDestination(destDir, newBase + subSuffix)
                if (operation == ClipboardOp.COPY) {
                    sub.copyTo(destSub, overwrite = false)
                } else {
                    if (!sub.renameTo(destSub)) {
                        sub.copyTo(destSub, overwrite = false)
                        sub.delete()
                    }
                }
            }
            processed++
        }

        if (context != null) {
            pathsToRemoveFromMediaStore.forEach { removeFromMediaStore(context, it) }
            if (pathsToScan.isNotEmpty()) scanPaths(context, pathsToScan)
        }
        processed
    }

    fun deleteEntries(
        targets: List<File>,
        context: Context? = null,
        fallbackUris: List<String> = emptyList(),
    ): Result<Int> = runCatching {
        var deletedCount = 0
        for (target in targets) {
            if (!target.exists()) continue
            val companions = if (target.isFile && VideoScanner.isVideoFile(target)) {
                VideoScanner.findCompanionSubtitles(target.absolutePath)
            } else {
                emptyList()
            }
            val deleted = target.deleteRecursively() ||
                (context != null && deleteViaMediaStore(context, target.absolutePath))
            if (deleted) {
                deletedCount++
                companions.forEach { runCatching { it.delete() } }
                if (context != null) removeFromMediaStore(context, target.absolutePath)
            }
        }
        if (deletedCount == 0 && context != null) {
            for (uriStr in fallbackUris) {
                val parsed = runCatching { Uri.parse(uriStr) }.getOrNull() ?: continue
                if (parsed.scheme == "content") {
                    val rows = runCatching { context.contentResolver.delete(parsed, null, null) }.getOrDefault(0)
                    if (rows > 0) deletedCount++
                }
            }
        }
        check(deletedCount > 0 || targets.isEmpty()) {
            "파일을 삭제할 수 없습니다. '모든 파일 접근' 권한을 허용해 주세요."
        }
        deletedCount
    }

    fun uniqueDestination(dir: File, desiredName: String): File {
        val candidate = File(dir, desiredName)
        if (!candidate.exists()) return candidate
        val dotIdx = desiredName.lastIndexOf('.')
        val (base, ext) = if (dotIdx > 0) {
            desiredName.substring(0, dotIdx) to desiredName.substring(dotIdx)
        } else {
            desiredName to ""
        }
        var counter = 1
        while (true) {
            val next = File(dir, "$base ($counter)$ext")
            if (!next.exists()) return next
            counter++
        }
    }

    private fun copyRecursively(src: File, dst: File) {
        if (src.isDirectory) {
            check(dst.exists() || dst.mkdirs()) { "폴더를 생성할 수 없습니다: ${dst.name}" }
            src.listFiles().orEmpty().forEach { child ->
                copyRecursively(child, File(dst, child.name))
            }
        } else {
            src.inputStream().use { input ->
                dst.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            }
        }
    }

    private fun collectFiles(root: File, out: MutableList<String>) {
        if (root.isDirectory) {
            root.listFiles().orEmpty().forEach { collectFiles(it, out) }
        } else {
            out.add(root.absolutePath)
        }
    }

    private fun deleteViaMediaStore(context: Context, path: String): Boolean {
        return runCatching {
            val uri = MediaStore.Files.getContentUri("external")
            val deleted = context.contentResolver.delete(
                uri,
                "${MediaStore.MediaColumns.DATA}=?",
                arrayOf(path)
            )
            deleted > 0
        }.getOrDefault(false)
    }

    private fun removeFromMediaStore(context: Context, path: String) {
        runCatching {
            val uri = MediaStore.Files.getContentUri("external")
            context.contentResolver.delete(
                uri,
                "${MediaStore.MediaColumns.DATA}=?",
                arrayOf(path)
            )
        }
    }

    private fun scanPaths(context: Context, paths: List<String>) {
        runCatching {
            MediaScannerConnection.scanFile(context, paths.toTypedArray(), null, null)
        }
    }
}
