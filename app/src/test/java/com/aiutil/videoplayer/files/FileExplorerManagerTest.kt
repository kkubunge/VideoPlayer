package com.aiutil.videoplayer.files

import com.aiutil.videoplayer.model.ClipboardOp
import com.aiutil.videoplayer.model.SortOrder
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
}
