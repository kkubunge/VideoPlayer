package com.aiutil.videoplayer.model

import kotlinx.serialization.Serializable

enum class SeekUnit(val label: String) {
    SECONDS("초 단위"),
    FRAMES("프레임 단위"),
}

enum class ScreenOrientationMode(val label: String) {
    AUTO("자동 회전"),
    LANDSCAPE("가로 고정"),
    PORTRAIT("세로 고정"),
}

enum class VideoScaleMode(val label: String) {
    FIT("화면 맞춤"),
    FILL("화면 채우기"),
    ZOOM("확대 자르기"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3"),
}

enum class TranslationEngineMode(val label: String) {
    AUTO("자동 (온디바이스 + 웹/Gemini)"),
    ML_KIT("온디바이스 AI (ML Kit)"),
    WEB("온라인 자동 번역"),
    GEMINI("Gemini AI 정밀 번역"),
}

enum class SortOrder(val label: String) {
    NAME_ASC("이름 오름차순"),
    NAME_DESC("이름 내림차순"),
    DATE_DESC("최신순"),
    SIZE_DESC("용량 큰 순"),
    DURATION_DESC("재생시간 긴 순"),
}

enum class ExplorerTab(val label: String) {
    FOLDERS("동영상 폴더"),
    FILES("파일 탐색기"),
    FAVORITES("즐겨찾기"),
    RECENT("최근 재생"),
}

enum class ClipboardOp(val label: String) {
    COPY("복사"),
    MOVE("이동"),
}

data class VideoItem(
    val id: String,
    val title: String,
    val uri: String,
    val path: String,
    val folderPath: String,
    val folderName: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val dateModifiedSec: Long,
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String = "video/*",
    val frameRate: Float = 30f,
)

data class VideoFolder(
    val path: String,
    val name: String,
    val videos: List<VideoItem>,
    val totalSizeBytes: Long,
    val lastModifiedSec: Long,
)

data class ExplorerEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModifiedMs: Long,
    val childCount: Int = 0,
    val isVideo: Boolean = false,
    val isSubtitle: Boolean = false,
    val videoItem: VideoItem? = null,
)

data class FileClipboard(
    val operation: ClipboardOp,
    val sourcePaths: List<String>,
)

data class SubtitleCue(
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val originalText: String,
    val translatedText: String? = null,
)

data class TrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val selected: Boolean,
)

@Serializable
data class PlayerSettings(
    val doubleTapUnit: SeekUnit = SeekUnit.SECONDS,
    val doubleTapSeconds: Int = 10,
    val doubleTapFrames: Int = 10,
    val defaultFps: Float = 30f,
    val resetSpeedOnLongPressRelease: Boolean = false,
    val resumePlayback: Boolean = true,
    val aiTranslateEnabled: Boolean = false,
    val targetLangCode: String = "ko",
    val dualSubtitles: Boolean = true,
    val subtitleFontSizeSp: Int = 18,
    val translationEngine: TranslationEngineMode = TranslationEngineMode.AUTO,
    val geminiApiKey: String = "",
    val autoTranscribeNoSubtitle: Boolean = true,
    val sortOrder: SortOrder = SortOrder.NAME_ASC,
)

@Serializable
data class PlaybackHistoryItem(
    val id: String,
    val title: String,
    val uri: String,
    val path: String,
    val folderPath: String,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedAtMs: Long,
)

@Serializable
data class FavoriteVideoItem(
    val id: String,
    val title: String,
    val uri: String,
    val path: String,
    val folderPath: String,
    val folderName: String,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val addedAtMs: Long = System.currentTimeMillis(),
)

@Serializable
data class FavoriteFolderItem(
    val path: String,
    val name: String,
    val addedAtMs: Long = System.currentTimeMillis(),
)

@Serializable
data class VideoBookmarkItem(
    val bookmarkId: String,
    val videoId: String,
    val videoTitle: String,
    val uri: String,
    val path: String,
    val folderPath: String,
    val positionMs: Long,
    val note: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
)

object SupportedLanguages {
    val items: List<Pair<String, String>> = listOf(
        "ko" to "한국어",
        "en" to "영어 (English)",
        "ja" to "일본어 (日本語)",
        "zh" to "중국어 (中文)",
        "es" to "스페인어 (Español)",
        "fr" to "프랑스어 (Français)",
        "de" to "독일어 (Deutsch)",
        "vi" to "베트남어 (Tiếng Việt)",
        "th" to "태국어 (ไทย)",
    )

    fun displayName(code: String): String =
        items.firstOrNull { it.first == code }?.second ?: code
}
