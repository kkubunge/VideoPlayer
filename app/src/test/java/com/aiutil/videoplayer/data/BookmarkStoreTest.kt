package com.aiutil.videoplayer.data

import com.aiutil.videoplayer.model.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BookmarkStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `favorites and video timestamps persist across reloads`() {
        val storage = File(tempFolder.root, "bookmarks.json")
        val store = BookmarkStore(storage)

        val video = VideoItem(
            id = "/movies/sample.mp4",
            title = "sample.mp4",
            uri = "file:///movies/sample.mp4",
            path = "/movies/sample.mp4",
            folderPath = "/movies",
            folderName = "movies",
            sizeBytes = 1024L,
            durationMs = 120_000L,
            dateModifiedSec = 100L,
        )

        // 동영상 즐겨찾기 추가 및 재로드 확인
        assertTrue(store.toggleFavoriteVideo(video))
        assertTrue(store.isFavoriteVideo(video.id, video.path))

        // 폴더 즐겨찾기 추가
        assertTrue(store.toggleFavoriteFolder("/movies", "movies"))
        assertTrue(store.isFavoriteFolder("/movies"))

        // 재생 위치 북마크 추가 (역순으로 넣어도 시간순 조회)
        store.addVideoBookmark(video, positionMs = 45_000L, note = "하이라이트")
        store.addVideoBookmark(video, positionMs = 12_000L, note = "오프닝")

        val reloaded = BookmarkStore(storage)
        assertTrue(reloaded.isFavoriteVideo(video.id, video.path))
        assertTrue(reloaded.isFavoriteFolder("/movies"))

        val list = reloaded.bookmarksForVideo(video.id, video.path)
        assertEquals(2, list.size)
        assertEquals(12_000L, list[0].positionMs)
        assertEquals("오프닝", list[0].note)
        assertEquals(45_000L, list[1].positionMs)

        // 삭제 시 연관 즐겨찾기/북마크 정리 및 해제 검증
        assertFalse(reloaded.toggleFavoriteFolder("/movies", "movies"))
        assertFalse(reloaded.isFavoriteFolder("/movies"))

        reloaded.removeForDeletedPath(video.id)
        assertFalse(reloaded.isFavoriteVideo(video.id, video.path))
        assertTrue(reloaded.bookmarksForVideo(video.id, video.path).isEmpty())
    }
}
