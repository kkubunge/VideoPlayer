package com.aiutil.videoplayer.files

import com.aiutil.videoplayer.model.ClipboardOp
import com.aiutil.videoplayer.model.SortOrder
import com.aiutil.videoplayer.model.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileExplorerManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `createFolder renameEntry copy move and delete work with companion subtitles`() {
        val root = tempFolder.root
        val srcDir = FileExplorerManager.createFolder(root, "Source").getOrThrow()
        val dstDir = FileExplorerManager.createFolder(root, "Dest").getOrThrow()

        val videoFile = File(srcDir, "movie.mp4").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val subFile = File(srcDir, "movie.srt").apply { writeText("1\n00:00:01,000 --> 00:00:02,000\nHi") }
        assertTrue(videoFile.exists() && subFile.exists())

        // 이름 변경 시 동반 자막도 함께 변경되는지 검증
        val renamed = FileExplorerManager.renameEntry(videoFile, "film.mp4").getOrThrow()
        assertEquals("film.mp4", renamed.name)
        assertTrue(File(srcDir, "film.srt").exists())
        assertFalse(subFile.exists())

        // 복사 검증
        val copyRes = FileExplorerManager.copyOrMove(
            sources = listOf(renamed),
            destDir = dstDir,
            operation = ClipboardOp.COPY,
        ).getOrThrow()
        assertEquals(1, copyRes)
        assertTrue(File(srcDir, "film.mp4").exists())
        assertTrue(File(dstDir, "film.mp4").exists())
        assertTrue(File(dstDir, "film.srt").exists())

        // 이동 검증 (중복 파일명 자동 회피 포함)
        val moveRes = FileExplorerManager.copyOrMove(
            sources = listOf(renamed),
            destDir = dstDir,
            operation = ClipboardOp.MOVE,
        ).getOrThrow()
        assertEquals(1, moveRes)
        assertFalse(File(srcDir, "film.mp4").exists())
        assertTrue(File(dstDir, "film (1).mp4").exists())

        // 목록 조회 및 삭제 검증
        val entries = FileExplorerManager.listDirectory(dstDir, SortOrder.NAME_ASC)
        assertTrue(entries.any { it.name == "film.mp4" && it.isVideo })

        val deleted = FileExplorerManager.deleteEntries(listOf(File(dstDir, "film.mp4"))).getOrThrow()
        assertEquals(1, deleted)
        assertFalse(File(dstDir, "film.mp4").exists())
    }

    @Test
    fun `listDirectory and VideoScanner support all 8 ascending and descending sort orders`() {
        val root = tempFolder.root
        val dir = FileExplorerManager.createFolder(root, "SortDir").getOrThrow()

        val alphaFile = File(dir, "alpha.mp4").apply {
            writeBytes(ByteArray(300))
            setLastModified(1_000_000L)
        }
        val betaFile = File(dir, "beta.mp4").apply {
            writeBytes(ByteArray(100))
            setLastModified(3_000_000L)
        }
        val gammaFile = File(dir, "gamma.mp4").apply {
            writeBytes(ByteArray(200))
            setLastModified(2_000_000L)
        }
        // 재생시간 정렬 시 비-비디오 파일은 오름/내림차순 모두 동영상 뒤로 배치되어야 함
        File(dir, "aaa_note.srt").apply {
            writeText("note")
            setLastModified(1_500_000L)
        }

        val alphaVideo = VideoItem(
            id = alphaFile.absolutePath,
            title = alphaFile.name,
            uri = alphaFile.toURI().toString(),
            path = alphaFile.absolutePath,
            folderPath = dir.absolutePath,
            folderName = dir.name,
            sizeBytes = 300L,
            durationMs = 20_000L,
            dateModifiedSec = 1_000L,
        )
        val betaVideo = VideoItem(
            id = betaFile.absolutePath,
            title = betaFile.name,
            uri = betaFile.toURI().toString(),
            path = betaFile.absolutePath,
            folderPath = dir.absolutePath,
            folderName = dir.name,
            sizeBytes = 100L,
            durationMs = 10_000L,
            dateModifiedSec = 3_000L,
        )
        val gammaVideo = VideoItem(
            id = gammaFile.absolutePath,
            title = gammaFile.name,
            uri = gammaFile.toURI().toString(),
            path = gammaFile.absolutePath,
            folderPath = dir.absolutePath,
            folderName = dir.name,
            sizeBytes = 200L,
            durationMs = 30_000L,
            dateModifiedSec = 2_000L,
        )

        val videoIndex = mapOf(
            alphaVideo.path to alphaVideo,
            betaVideo.path to betaVideo,
            gammaVideo.path to gammaVideo,
        )
        val videos = listOf(alphaVideo, betaVideo, gammaVideo)

        // 탐색기 파일 목록: 이름/날짜/용량/재생시간 오름·내림차순 검증
        assertEquals(
            listOf("aaa_note.srt", "alpha.mp4", "beta.mp4", "gamma.mp4"),
            FileExplorerManager.listDirectory(dir, SortOrder.NAME_ASC, videoIndex).map { it.name },
        )
        assertEquals(
            listOf("gamma.mp4", "beta.mp4", "alpha.mp4", "aaa_note.srt"),
            FileExplorerManager.listDirectory(dir, SortOrder.NAME_DESC, videoIndex).map { it.name },
        )
        assertEquals(
            listOf("beta.mp4", "gamma.mp4", "aaa_note.srt", "alpha.mp4"),
            FileExplorerManager.listDirectory(dir, SortOrder.DATE_DESC, videoIndex).map { it.name },
        )
        assertEquals(
            listOf("alpha.mp4", "aaa_note.srt", "gamma.mp4", "beta.mp4"),
            FileExplorerManager.listDirectory(dir, SortOrder.DATE_ASC, videoIndex).map { it.name },
        )
        assertEquals(
            listOf("alpha.mp4", "gamma.mp4", "beta.mp4", "aaa_note.srt"),
            FileExplorerManager.listDirectory(dir, SortOrder.SIZE_DESC, videoIndex).map { it.name },
        )
        assertEquals(
            listOf("aaa_note.srt", "beta.mp4", "gamma.mp4", "alpha.mp4"),
            FileExplorerManager.listDirectory(dir, SortOrder.SIZE_ASC, videoIndex).map { it.name },
        )
        assertEquals(
            listOf("gamma.mp4", "alpha.mp4", "beta.mp4", "aaa_note.srt"),
            FileExplorerManager.listDirectory(dir, SortOrder.DURATION_DESC, videoIndex).map { it.name },
        )
        assertEquals(
            listOf("beta.mp4", "alpha.mp4", "gamma.mp4", "aaa_note.srt"),
            FileExplorerManager.listDirectory(dir, SortOrder.DURATION_ASC, videoIndex).map { it.name },
        )

        // 동영상 목록 sortVideos 검증
        assertEquals(
            listOf("alpha.mp4", "gamma.mp4", "beta.mp4"),
            VideoScanner.sortVideos(videos, SortOrder.DATE_ASC).map { it.title },
        )
        assertEquals(
            listOf("beta.mp4", "gamma.mp4", "alpha.mp4"),
            VideoScanner.sortVideos(videos, SortOrder.SIZE_ASC).map { it.title },
        )
        assertEquals(
            listOf("beta.mp4", "alpha.mp4", "gamma.mp4"),
            VideoScanner.sortVideos(videos, SortOrder.DURATION_ASC).map { it.title },
        )

        // 폴더 단위 groupByFolder 오름/내림차순 검증
        val folderA = VideoItem(
            id = "fA",
            title = "a.mp4",
            uri = "file:///fA/a.mp4",
            path = "/fA/a.mp4",
            folderPath = "/fA",
            folderName = "FolderA",
            sizeBytes = 500L,
            durationMs = 5_000L,
            dateModifiedSec = 200L,
        )
        val folderB = VideoItem(
            id = "fB",
            title = "b.mp4",
            uri = "file:///fB/b.mp4",
            path = "/fB/b.mp4",
            folderPath = "/fB",
            folderName = "FolderB",
            sizeBytes = 100L,
            durationMs = 50_000L,
            dateModifiedSec = 100L,
        )
        val allFolderItems = listOf(folderA, folderB)
        assertEquals(
            listOf("FolderB", "FolderA"),
            VideoScanner.groupByFolder(allFolderItems, SortOrder.DATE_ASC).map { it.name },
        )
        assertEquals(
            listOf("FolderB", "FolderA"),
            VideoScanner.groupByFolder(allFolderItems, SortOrder.SIZE_ASC).map { it.name },
        )
        assertEquals(
            listOf("FolderA", "FolderB"),
            VideoScanner.groupByFolder(allFolderItems, SortOrder.DURATION_ASC).map { it.name },
        )
    }
}
