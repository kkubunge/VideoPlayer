package com.aiutil.videoplayer.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Rational
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aiutil.videoplayer.App
import com.aiutil.videoplayer.ai.AiSubtitleController
import com.aiutil.videoplayer.files.FileExplorerManager
import com.aiutil.videoplayer.files.VideoScanner
import com.aiutil.videoplayer.files.VideoTrimmer
import com.aiutil.videoplayer.model.DoubleTapAction
import com.aiutil.videoplayer.model.PlayerSettings
import com.aiutil.videoplayer.model.ScreenOrientationMode
import com.aiutil.videoplayer.model.SeekUnit
import com.aiutil.videoplayer.model.SupportedLanguages
import com.aiutil.videoplayer.model.TrackOption
import com.aiutil.videoplayer.model.VideoItem
import com.aiutil.videoplayer.model.VideoScaleMode
import com.aiutil.videoplayer.player.BackgroundPlaybackBridge
import com.aiutil.videoplayer.player.BackgroundPlaybackPolicy
import com.aiutil.videoplayer.player.BackgroundPlaybackService
import com.aiutil.videoplayer.util.Format
import com.aiutil.videoplayer.util.GestureMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private enum class ActiveDragMode {
    NONE,
    BRIGHTNESS,
    VOLUME,
    SEEK,
    LONG_PRESS_SPEED,
    TWO_FINGER_ZOOM,
}

private data class HudOverlayState(
    val icon: ImageVector,
    val title: String,
    val subtitle: String = "",
    val progress: Float? = null,
)

private data class DoubleTapFeedback(
    val isRightSide: Boolean,
    val label: String,
    val token: Long,
    val isFileSwitch: Boolean = false,
)

private data class CornerNotice(
    val message: String,
    val token: Long,
    val durationMs: Long = 1_800L,
)

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    initialVideo: VideoItem,
    initialPlaylist: List<VideoItem>,
    initialSeekPositionMs: Long? = null,
    isInPipMode: Boolean,
    onOpenSettings: () -> Unit,
    onDeletedVideo: (String) -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val viewConfig = LocalViewConfiguration.current
    val scope = rememberCoroutineScope()
    val settings by App.settings.settings.collectAsState()
    val latestSettings by rememberUpdatedState(settings)

    var playlist by remember(initialPlaylist) {
        mutableStateOf(if (initialPlaylist.isNotEmpty()) initialPlaylist else listOf(initialVideo))
    }
    var currentIndex by remember(initialVideo, initialPlaylist) {
        val idx = playlist.indexOfFirst { it.id == initialVideo.id || it.uri == initialVideo.uri }
        mutableIntStateOf(if (idx >= 0) idx else 0)
    }
    val currentVideo = playlist.getOrElse(currentIndex) { initialVideo }

    // 플레이어 인스턴스 & AI 자막 컨트롤러
    val exoPlayer = remember {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MOVIE)
            .build()
        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build().apply {
                setSeekParameters(SeekParameters.EXACT)
                playWhenReady = true
            }
    }
    val subtitleController = remember { AiSubtitleController(context, scope) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestIsInPipMode by rememberUpdatedState(isInPipMode)
    var hasStartedBackgroundSession by remember { mutableStateOf(false) }
    var lastLoadedVideoKey by remember { mutableStateOf<String?>(null) }

    // 재생 상태
    var isPlaying by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(currentVideo.durationMs.coerceAtLeast(0L)) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var videoFps by remember { mutableFloatStateOf(settings.defaultFps) }
    var isScrubbingBar by remember { mutableStateOf(false) }
    var scrubBarPositionMs by remember { mutableLongStateOf(0L) }
    var wasPlayingBeforeBarScrub by remember { mutableStateOf(false) }
    var lastBarSeekAt by remember { mutableLongStateOf(0L) }

    // UI 및 제어 상태
    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var scaleMode by remember { mutableStateOf(VideoScaleMode.FIT) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var orientationMode by remember { mutableStateOf(ScreenOrientationMode.AUTO) }
    var subtitlesVisible by remember { mutableStateOf(true) }

    // 밝기 / 볼륨 상태
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var brightness by remember {
        val attr = activity?.window?.attributes?.screenBrightness ?: -1f
        mutableFloatStateOf(if (attr in 0f..1f) attr else 0.5f)
    }
    var volumeFraction by remember {
        mutableFloatStateOf(
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume.toFloat()
        )
    }

    // HUD / 더블터치 피드백 / 우측 상단 구석 안내 배지
    var hudState by remember { mutableStateOf<HudOverlayState?>(null) }
    var doubleTapFeedback by remember { mutableStateOf<DoubleTapFeedback?>(null) }
    var cornerNotice by remember { mutableStateOf<CornerNotice?>(null) }
    var containerSize by remember { mutableStateOf(IntSize(1080, 1920)) }

    // A-B 구간 반복
    var repeatA by remember { mutableStateOf<Long?>(null) }
    var repeatB by remember { mutableStateOf<Long?>(null) }

    // 취침 타이머 (남은 초, null이면 꺼짐)
    var sleepRemainingSec by remember { mutableStateOf<Int?>(null) }

    // 트랙 목록 및 시트/다이얼로그 상태
    var audioTracks by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var textTracks by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAiSheet by remember { mutableStateOf(false) }
    var showTrackSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showSubtitleFileDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showBookmarkSheet by remember { mutableStateOf(false) }
    var showTrimSheet by remember { mutableStateOf(false) }
    var pendingInitialSeek by remember(initialVideo, initialSeekPositionMs) {
        mutableStateOf(initialSeekPositionMs)
    }

    // 즐겨찾기 및 북마크 상태
    val favoriteVideos by App.bookmarks.favoriteVideos.collectAsState()
    val allBookmarks by App.bookmarks.videoBookmarks.collectAsState()
    val allCues by subtitleController.cues.collectAsState()
    val isCurrentFavorite = remember(favoriteVideos, currentVideo) {
        App.bookmarks.isFavoriteVideo(currentVideo.id, currentVideo.path)
    }
    val currentVideoBookmarks = remember(allBookmarks, currentVideo) {
        App.bookmarks.bookmarksForVideo(currentVideo.id, currentVideo.path)
    }

    // AI 자막 상태 수집
    val activeCue by subtitleController.activeCue.collectAsState()
    val embeddedOrig by subtitleController.liveEmbeddedOriginal.collectAsState()
    val embeddedTrans by subtitleController.liveEmbeddedTranslated.collectAsState()
    val subtitleStatus by subtitleController.statusText.collectAsState()
    val loadedSubName by subtitleController.loadedSubtitleName.collectAsState()
    val syncOffsetMs by subtitleController.syncOffsetMs.collectAsState()

    fun applyBrightness(value: Float) {
        val clamped = value.coerceIn(GestureMath.MIN_BRIGHTNESS, GestureMath.MAX_BRIGHTNESS)
        brightness = clamped
        activity?.window?.let { w ->
            val lp = w.attributes
            lp.screenBrightness = clamped
            w.attributes = lp
        }
    }

    fun applyVolumeFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        volumeFraction = clamped
        val targetStreamVol = (clamped * maxVolume).roundToInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetStreamVol, 0)
    }

    fun applySpeed(speed: Float) {
        val clamped = speed.coerceIn(GestureMath.MIN_SPEED, GestureMath.MAX_SPEED)
        playbackSpeed = clamped
        exoPlayer.setPlaybackSpeed(clamped)
    }

    fun showNotice(message: String, longDuration: Boolean = false) {
        cornerNotice = CornerNotice(
            message = message,
            token = SystemClock.uptimeMillis(),
            durationMs = if (longDuration) 2_800L else 1_800L,
        )
    }

    fun saveCurrentProgress() {
        val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
        val dur = exoPlayer.duration.takeIf { it > 0L } ?: currentVideo.durationMs
        App.history.recordProgress(currentVideo, pos, dur)
    }

    fun loadVideo(video: VideoItem, index: Int) {
        val key = "${video.id}|${video.uri}"
        if (lastLoadedVideoKey == key) return
        lastLoadedVideoKey = key

        val mediaUri = if (video.path.isNotBlank() && File(video.path).canRead()) {
            Uri.fromFile(File(video.path))
        } else {
            Uri.parse(video.uri)
        }
        val mediaItem = MediaItem.Builder()
            .setUri(mediaUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(video.title)
                    .setArtist(video.folderName)
                    .build()
            )
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        val bookmarkTarget = pendingInitialSeek
        if (bookmarkTarget != null && bookmarkTarget > 0L) {
            pendingInitialSeek = null
            exoPlayer.seekTo(bookmarkTarget)
            showNotice("북마크 위치 (${Format.duration(bookmarkTarget)})")
        } else if (latestSettings.resumePlayback) {
            val savedPos = App.history.getSavedPosition(video.id)
            if (savedPos > 1_500L) {
                exoPlayer.seekTo(savedPos)
                showNotice("이어보기 (${Format.duration(savedPos)})")
            }
        }
        exoPlayer.setPlaybackSpeed(playbackSpeed)
        exoPlayer.play()
        subtitleController.loadForVideo(video, latestSettings)

        val metaSubtitle = buildString {
            if (video.folderName.isNotBlank()) append(video.folderName)
            if (playlist.size > 1) {
                if (isNotEmpty()) append(" · ")
                append("${index + 1}/${playlist.size}")
            }
        }
        BackgroundPlaybackBridge.updateMetadata(
            BackgroundPlaybackBridge.PlaybackMetadata(
                title = video.title,
                subtitle = metaSubtitle,
                hasPrevious = index > 0,
                hasNext = index + 1 < playlist.size,
            )
        )
        if (latestSettings.backgroundPlayOnScreenLock) {
            hasStartedBackgroundSession = true
            BackgroundPlaybackService.startOrUpdate(context)
        }
    }

    fun goToVideo(index: Int) {
        if (index !in playlist.indices) return
        saveCurrentProgress()
        repeatA = null
        repeatB = null
        currentIndex = index
        // 화면 OFF(ON_STOP) 상태에서는 Compose LaunchedEffect가 중단되므로 즉시 새 영상을 로드한다.
        loadVideo(playlist[index], index)
    }

    val latestSaveProgress by rememberUpdatedState { saveCurrentProgress() }
    val latestGoToVideo by rememberUpdatedState<(Int) -> Unit> { idx -> goToVideo(idx) }
    val latestAdvanceNextOrShowControls by rememberUpdatedState {
        saveCurrentProgress()
        if (currentIndex + 1 < playlist.size) {
            goToVideo(currentIndex + 1)
        } else {
            controlsVisible = true
        }
    }

    fun handleDoubleTap(tapX: Float, width: Float, effectiveFps: Float) {
        if (tapX in (width * 0.35f)..(width * 0.65f)) {
            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            return
        }
        val isRight = tapX > width * 0.5f
        if (latestSettings.doubleTapAction == DoubleTapAction.PREV_NEXT_VIDEO) {
            val targetIndex = GestureMath.computeAdjacentVideoIndex(
                currentIndex = currentIndex,
                playlistSize = playlist.size,
                isNext = isRight,
            )
            val feedbackLabel = if (targetIndex != null) {
                goToVideo(targetIndex)
                if (isRight) "다음 파일" else "이전 파일"
            } else {
                if (isRight) "마지막 파일" else "첫 번째 파일"
            }
            doubleTapFeedback = DoubleTapFeedback(
                isRightSide = isRight,
                label = feedbackLabel,
                token = SystemClock.uptimeMillis(),
                isFileSwitch = true,
            )
            return
        }
        val deltaMs = GestureMath.computeDoubleTapDeltaMs(
            isRightSide = isRight,
            unit = latestSettings.doubleTapUnit,
            seconds = latestSettings.doubleTapSeconds,
            frames = latestSettings.doubleTapFrames,
            fps = effectiveFps,
        )
        val dur = durationMs
        val target = (exoPlayer.currentPosition + deltaMs).coerceIn(
            0L,
            if (dur > 0L) dur else Long.MAX_VALUE
        )
        exoPlayer.seekTo(target)
        positionMs = target
        val label = when (latestSettings.doubleTapUnit) {
            SeekUnit.SECONDS -> {
                if (isRight) "+${latestSettings.doubleTapSeconds}초"
                else "-${latestSettings.doubleTapSeconds}초"
            }
            SeekUnit.FRAMES -> {
                if (isRight) "+${latestSettings.doubleTapFrames}프레임"
                else "-${latestSettings.doubleTapFrames}프레임"
            }
        }
        doubleTapFeedback = DoubleTapFeedback(
            isRightSide = isRight,
            label = label,
            token = SystemClock.uptimeMillis(),
        )
    }

    val latestHandleDoubleTap by rememberUpdatedState<(Float, Float, Float) -> Unit> { tapX, w, fps ->
        handleDoubleTap(tapX, w, fps)
    }

    fun stepByButton(forward: Boolean) {
        val unit = latestSettings.buttonSeekUnit
        val fps = if (videoFps in 1f..240f) videoFps else latestSettings.defaultFps
        if (unit == SeekUnit.FRAMES && exoPlayer.isPlaying) {
            exoPlayer.pause()
        }
        val deltaMs = GestureMath.computeStepButtonDeltaMs(
            forward = forward,
            unit = unit,
            seconds = latestSettings.buttonSeekSeconds,
            frames = latestSettings.buttonSeekFrames,
            fps = fps,
        )
        val label = when (unit) {
            SeekUnit.SECONDS -> {
                val sec = latestSettings.buttonSeekSeconds.coerceAtLeast(1)
                if (forward) "+${sec}초" else "-${sec}초"
            }
            SeekUnit.FRAMES -> {
                val frames = latestSettings.buttonSeekFrames.coerceAtLeast(1)
                if (forward) "+${frames} 프레임" else "-${frames} 프레임"
            }
        }
        val target = (exoPlayer.currentPosition + deltaMs)
            .coerceIn(0L, durationMs.coerceAtLeast(0L))
        exoPlayer.seekTo(target)
        positionMs = target
        hudState = HudOverlayState(
            icon = if (forward) Icons.Default.FastForward else Icons.Default.FastRewind,
            title = label,
            subtitle = Format.duration(target),
        )
    }

    fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            val fmt = exoPlayer.videoFormat
            val w = (fmt?.width ?: currentVideo.width).coerceAtLeast(16)
            val h = (fmt?.height ?: currentVideo.height).coerceAtLeast(9)
            val ratio = (w.toFloat() / h.toFloat()).coerceIn(0.42f, 2.39f)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational((ratio * 100).roundToInt(), 100))
                .build()
            runCatching { activity.enterPictureInPictureMode(params) }
        } else {
            showNotice("이 기기에서는 PIP 모드를 지원하지 않습니다.")
        }
    }

    // 몰입형 전체 화면 & 종료 정리
    DisposableEffect(activity, view) {
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            latestSaveProgress()
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            BackgroundPlaybackBridge.clear(exoPlayer)
            BackgroundPlaybackService.stop(context)
            subtitleController.release()
            exoPlayer.release()
        }
    }

    // 재생 중일 때만 화면 켜짐 유지
    LaunchedEffect(isPlaying) {
        val window = activity?.window
        if (BackgroundPlaybackPolicy.shouldKeepScreenOn(isPlaying)) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // 화면 OFF / 백그라운드 전환 시 설정에 따라 재생 유지 또는 자동 일시정지
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (BackgroundPlaybackPolicy.shouldPauseOnStop(
                            backgroundPlayEnabled = latestSettings.backgroundPlayOnScreenLock,
                            isInPipMode = latestIsInPipMode,
                            isPlaying = exoPlayer.isPlaying,
                        )
                    ) {
                        exoPlayer.pause()
                        latestSaveProgress()
                    } else if (latestSettings.backgroundPlayOnScreenLock) {
                        latestSaveProgress()
                        if (exoPlayer.isPlaying) {
                            BackgroundPlaybackService.startOrUpdate(context)
                        }
                    }
                }

                Lifecycle.Event.ON_START -> {
                    positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                    isPlaying = exoPlayer.isPlaying
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 백그라운드 재생 브리지 및 포그라운드 서비스 동기화
    LaunchedEffect(
        currentVideo.id,
        currentIndex,
        playlist.size,
        settings.backgroundPlayOnScreenLock,
        hasStartedBackgroundSession,
    ) {
        val subtitle = buildString {
            if (currentVideo.folderName.isNotBlank()) append(currentVideo.folderName)
            if (playlist.size > 1) {
                if (isNotEmpty()) append(" · ")
                append("${currentIndex + 1}/${playlist.size}")
            }
        }
        val meta = BackgroundPlaybackBridge.PlaybackMetadata(
            title = currentVideo.title,
            subtitle = subtitle,
            hasPrevious = currentIndex > 0,
            hasNext = currentIndex + 1 < playlist.size,
        )
        BackgroundPlaybackBridge.attach(
            player = exoPlayer,
            metadata = meta,
            onPrevious = {
                GestureMath.computeAdjacentVideoIndex(
                    currentIndex = currentIndex,
                    playlistSize = playlist.size,
                    isNext = false,
                )?.let { latestGoToVideo(it) }
            },
            onNext = {
                GestureMath.computeAdjacentVideoIndex(
                    currentIndex = currentIndex,
                    playlistSize = playlist.size,
                    isNext = true,
                )?.let { latestGoToVideo(it) }
            },
            onTogglePlay = {
                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            },
            onStopBackground = {
                if (exoPlayer.isPlaying) exoPlayer.pause()
                hasStartedBackgroundSession = false
            },
        )
        if (BackgroundPlaybackPolicy.shouldRunForegroundService(
                backgroundPlayEnabled = settings.backgroundPlayOnScreenLock,
                hasStartedPlayback = hasStartedBackgroundSession,
                isExiting = false,
            )
        ) {
            BackgroundPlaybackService.startOrUpdate(context)
        } else {
            BackgroundPlaybackService.stop(context)
        }
    }

    // 화면 방향 설정 적용
    LaunchedEffect(orientationMode) {
        activity?.requestedOrientation = when (orientationMode) {
            ScreenOrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            ScreenOrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            ScreenOrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // ExoPlayer 리스너 등록
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing && latestSettings.backgroundPlayOnScreenLock) {
                    hasStartedBackgroundSession = true
                    BackgroundPlaybackService.startOrUpdate(context)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val dur = exoPlayer.duration
                    if (dur > 0L) durationMs = dur
                    val fmtFps = exoPlayer.videoFormat?.frameRate
                    if (fmtFps != null && fmtFps in 1f..240f) {
                        videoFps = fmtFps
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    latestAdvanceNextOrShowControls()
                }
            }

            override fun onCues(cueGroup: CueGroup) {
                val combined = cueGroup.cues.mapNotNull { it.text?.toString()?.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString("\n")
                subtitleController.onEmbeddedCueText(combined, latestSettings)
            }

            override fun onTracksChanged(tracks: Tracks) {
                val audios = ArrayList<TrackOption>()
                val texts = ArrayList<TrackOption>()
                tracks.groups.forEachIndexed { gIdx, group ->
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (tIdx in 0 until group.length) {
                            val fmt = group.getTrackFormat(tIdx)
                            val lang = fmt.language ?: "und"
                            val label = fmt.label ?: "오디오 트랙 ${audios.size + 1} ($lang)"
                            audios.add(
                                TrackOption(
                                    groupIndex = gIdx,
                                    trackIndex = tIdx,
                                    label = label,
                                    language = lang,
                                    selected = group.isTrackSelected(tIdx),
                                )
                            )
                        }
                    } else if (group.type == C.TRACK_TYPE_TEXT) {
                        for (tIdx in 0 until group.length) {
                            val fmt = group.getTrackFormat(tIdx)
                            val lang = fmt.language ?: "und"
                            val label = fmt.label ?: "내장 자막 ${texts.size + 1} ($lang)"
                            texts.add(
                                TrackOption(
                                    groupIndex = gIdx,
                                    trackIndex = tIdx,
                                    label = label,
                                    language = lang,
                                    selected = group.isTrackSelected(tIdx),
                                )
                            )
                        }
                    }
                }
                audioTracks = audios
                textTracks = texts
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // 현재 동영상 로드 + 이어보기 + 자막 로드
    LaunchedEffect(currentVideo.id, currentVideo.uri) {
        loadVideo(currentVideo, currentIndex)
    }

    // AI 설정 변경 시 번역 갱신
    LaunchedEffect(
        settings.aiTranslateEnabled,
        settings.targetLangCode,
        settings.translationEngine,
        settings.geminiApiKey,
        settings.autoTranscribeNoSubtitle,
    ) {
        if (settings.aiTranslateEnabled) {
            val curPos = exoPlayer.currentPosition.coerceAtLeast(0L)
            subtitleController.pretranslateAround(curPos, settings)
            subtitleController.onPlaybackPosition(curPos, true, settings)
        }
    }

    // 주기적 재생 위치 폴링 + A-B 반복 + 자막 동기화 + 이어보기 저장
    LaunchedEffect(currentVideo.id) {
        var tick = 0
        while (true) {
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            if (!isScrubbingBar) {
                positionMs = pos
            }
            val dur = exoPlayer.duration
            if (dur > 0L) durationMs = dur

            // 구간 반복 체크
            val a = repeatA
            val b = repeatB
            if (a != null && b != null && b > a + 300L && pos >= b) {
                exoPlayer.seekTo(a)
            }

            subtitleController.onPlaybackPosition(pos, exoPlayer.isPlaying, latestSettings)

            tick++
            if (tick % 10 == 0 && exoPlayer.isPlaying) {
                saveCurrentProgress()
            }
            delay(150L)
        }
    }

    // 취침 타이머 카운트다운
    LaunchedEffect(sleepRemainingSec) {
        val rem = sleepRemainingSec ?: return@LaunchedEffect
        if (rem <= 0) {
            exoPlayer.pause()
            sleepRemainingSec = null
            showNotice("취침 타이머로 재생이 일시정지되었습니다.")
        } else {
            delay(1000L)
            sleepRemainingSec = rem - 1
        }
    }

    // 컨트롤 바 자동 숨김
    LaunchedEffect(controlsVisible, isPlaying, isLocked) {
        if (controlsVisible && isPlaying && !isLocked) {
            delay(3800L)
            controlsVisible = false
        }
    }

    // 더블터치 피드백 자동 소멸
    LaunchedEffect(doubleTapFeedback?.token) {
        if (doubleTapFeedback != null) {
            delay(750L)
            doubleTapFeedback = null
        }
    }

    // 우측 상단 구석 안내 배지 자동 소멸
    LaunchedEffect(cornerNotice?.token) {
        val notice = cornerNotice ?: return@LaunchedEffect
        delay(notice.durationMs)
        if (cornerNotice?.token == notice.token) {
            cornerNotice = null
        }
    }

    // 단독 HUD 피드백(-1F/+1F 등) 자동 소멸
    LaunchedEffect(hudState) {
        if (hudState != null && !isScrubbingBar) {
            // 드래그 중이 아닐 때 1.2초 후 사라짐 (드래그 중에는 계속 덮어씌워짐)
            val snapshot = hudState
            delay(1200L)
            if (hudState == snapshot) {
                hudState = null
            }
        }
    }

    BackHandler {
        when {
            showTrimSheet -> {
                exoPlayer.pause()
                showTrimSheet = false
            }
            isLocked -> {
                controlsVisible = true
                showNotice("화면 잠금을 먼저 해제해 주세요.")
            }
            else -> onExit()
        }
    }

    val latestIsLocked by rememberUpdatedState(isLocked)
    val latestPlaybackSpeed by rememberUpdatedState(playbackSpeed)
    val latestDurationMs by rememberUpdatedState(durationMs)
    val latestVideoFps by rememberUpdatedState(videoFps)
    val latestZoomScale by rememberUpdatedState(zoomScale)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
            .onSizeChanged { containerSize = it },
    ) {
        // 1. 동영상 비디오 서피스 (PlayerView)
        val playerContainerModifier = when (scaleMode) {
            VideoScaleMode.RATIO_16_9 -> Modifier.align(Alignment.Center).aspectRatio(16f / 9f)
            VideoScaleMode.RATIO_4_3 -> Modifier.align(Alignment.Center).aspectRatio(4f / 3f)
            else -> Modifier.fillMaxSize()
        }
        Box(modifier = playerContainerModifier) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        keepScreenOn = true
                        subtitleView?.visibility = View.GONE
                    }
                },
                update = { pv ->
                    pv.player = exoPlayer
                    pv.resizeMode = when (scaleMode) {
                        VideoScaleMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        VideoScaleMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        VideoScaleMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        VideoScaleMode.RATIO_16_9,
                        VideoScaleMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    }
                    pv.scaleX = zoomScale
                    pv.scaleY = zoomScale
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 2. 제스처 인식 레이어 (밝기 / 볼륨 / 길게터치 배속 / 프레임 좌우드래그 / 더블터치 / 싱글탭)
        if (!isInPipMode && !showTrimSheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var lastSingleTapJobPending = false
                        var pendingTapTime = 0L
                        var pendingTapX = 0f

                        awaitPointerEventScope {
                            while (true) {
                                // 첫 번째 터치 DOWN 대기
                                val firstDown = awaitPointerEvent()
                                val downChange = firstDown.changes.firstOrNull()
                                if (downChange == null || !downChange.pressed) continue

                                val startX = downChange.position.x
                                val startY = downChange.position.y
                                val width = size.width.toFloat().coerceAtLeast(1f)
                                val height = size.height.toFloat().coerceAtLeast(1f)
                                val downTime = SystemClock.uptimeMillis()

                                // 잠금 상태면 단순 탭/더블탭이 아닌 잠금 UI 토글만 처리
                                if (latestIsLocked) {
                                    var moved = false
                                    while (true) {
                                        val ev = awaitPointerEvent()
                                        val ch = ev.changes.firstOrNull() ?: break
                                        if (abs(ch.position.x - startX) > viewConfig.touchSlop ||
                                            abs(ch.position.y - startY) > viewConfig.touchSlop
                                        ) {
                                            moved = true
                                        }
                                        if (!ch.pressed) break
                                    }
                                    if (!moved) controlsVisible = !controlsVisible
                                    continue
                                }

                                // 직전 탭과의 간격이 더블터치 허용 시간 이내인지 확인
                                val isDoubleTapCandidate = lastSingleTapJobPending &&
                                    (downTime - pendingTapTime) <= viewConfig.doubleTapTimeoutMillis

                                val startBrightness = brightness
                                val startVol = volumeFraction
                                val startSpeed = latestPlaybackSpeed
                                val startSeekPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                                val effectiveFps = if (latestVideoFps in 1f..240f) {
                                    latestVideoFps
                                } else {
                                    latestSettings.defaultFps
                                }

                                var mode = ActiveDragMode.NONE
                                var longPressAnchorX = startX
                                var lastSeekApplyAt = 0L
                                var pendingSeekTarget = startSeekPos
                                var wasPlayingBeforeSeek = false
                                var gestureEndedCleanly = false

                                var zoomStartScale = latestZoomScale
                                var twoFingerInitSpan = 0f
                                var twoFingerInitCenterX = 0f

                                val longPressDeadline = downTime + viewConfig.longPressTimeoutMillis

                                while (true) {
                                    val now = SystemClock.uptimeMillis()
                                    val waitMs = (longPressDeadline - now).coerceAtLeast(1L)
                                    val event = if (mode == ActiveDragMode.NONE && !isDoubleTapCandidate) {
                                        withTimeoutOrNull(waitMs) { awaitPointerEvent() }
                                    } else {
                                        awaitPointerEvent()
                                    }

                                    if (event == null) {
                                        // 롱프레스 타임아웃 도달! -> 배속 조절 모드 진입
                                        mode = ActiveDragMode.LONG_PRESS_SPEED
                                        longPressAnchorX = startX
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        hudState = HudOverlayState(
                                            icon = Icons.Default.Speed,
                                            title = Format.speed(latestPlaybackSpeed),
                                            subtitle = "좌/우 드래그 (x0.5 ~ x6.0)",
                                            progress = (latestPlaybackSpeed - GestureMath.MIN_SPEED) /
                                                (GestureMath.MAX_SPEED - GestureMath.MIN_SPEED),
                                        )
                                        continue
                                    }

                                    val pressedChanges = event.changes.filter { it.pressed }
                                    if (pressedChanges.isEmpty()) {
                                        gestureEndedCleanly = true
                                        break
                                    }

                                    // 두 손가락 터치 감지 시 즉시 화면 크기 조절 모드로 전환
                                    if (pressedChanges.size >= 2) {
                                        val p0 = pressedChanges[0].position
                                        val p1 = pressedChanges[1].position
                                        val span = hypot(p0.x - p1.x, p0.y - p1.y)
                                        val centerX = (p0.x + p1.x) * 0.5f

                                        if (mode != ActiveDragMode.TWO_FINGER_ZOOM) {
                                            if (mode == ActiveDragMode.SEEK) {
                                                // 탐색 중 두 손가락 줌으로 전환될 때 탐색 상태 복원
                                                exoPlayer.setSeekParameters(SeekParameters.EXACT)
                                                isScrubbingBar = false
                                                if (wasPlayingBeforeSeek) {
                                                    exoPlayer.play()
                                                    wasPlayingBeforeSeek = false
                                                }
                                            }
                                            lastSingleTapJobPending = false
                                            mode = ActiveDragMode.TWO_FINGER_ZOOM
                                            zoomStartScale = latestZoomScale
                                            twoFingerInitSpan = span
                                            twoFingerInitCenterX = centerX
                                            val pct = (zoomStartScale * 100f).roundToInt()
                                            hudState = HudOverlayState(
                                                icon = Icons.Default.AspectRatio,
                                                title = "화면 크기 $pct%",
                                                subtitle = "좌우 드래그/핀치 (50%~400%)",
                                                progress = (zoomStartScale - GestureMath.MIN_ZOOM) /
                                                    (GestureMath.MAX_ZOOM - GestureMath.MIN_ZOOM),
                                            )
                                        } else {
                                            val newZoom = GestureMath.computeTwoFingerZoom(
                                                startZoom = zoomStartScale,
                                                initialSpan = twoFingerInitSpan,
                                                currentSpan = span,
                                                initialCenterX = twoFingerInitCenterX,
                                                currentCenterX = centerX,
                                                viewWidth = width,
                                            )
                                            zoomScale = newZoom
                                            val pct = (newZoom * 100f).roundToInt()
                                            hudState = HudOverlayState(
                                                icon = Icons.Default.AspectRatio,
                                                title = "화면 크기 $pct%",
                                                subtitle = "50% ~ 400%",
                                                progress = (newZoom - GestureMath.MIN_ZOOM) /
                                                    (GestureMath.MAX_ZOOM - GestureMath.MIN_ZOOM),
                                            )
                                        }
                                        pressedChanges.forEach { it.consume() }
                                        continue
                                    }

                                    if (mode == ActiveDragMode.TWO_FINGER_ZOOM) {
                                        // 한 손가락만 남았을 때는 나머지 손가락이 떨어질 때까지 다른 제스처로 튀지 않게 대기
                                        pressedChanges.forEach { it.consume() }
                                        continue
                                    }

                                    val change = pressedChanges.firstOrNull { it.id == downChange.id }
                                        ?: pressedChanges.first()

                                    val curX = change.position.x
                                    val curY = change.position.y
                                    val dx = curX - startX
                                    val dy = curY - startY

                                    if (mode == ActiveDragMode.NONE) {
                                        val slop = viewConfig.touchSlop
                                        if (abs(dx) > slop || abs(dy) > slop) {
                                            lastSingleTapJobPending = false
                                            mode = when {
                                                abs(dx) > abs(dy) -> ActiveDragMode.SEEK
                                                startX < width * 0.5f -> ActiveDragMode.BRIGHTNESS
                                                else -> ActiveDragMode.VOLUME
                                            }
                                            if (mode == ActiveDragMode.SEEK) {
                                                longPressAnchorX = curX
                                                // 드래그 중 화면 프레임을 즉시 그려내기 위해 일시정지 + 고속 싱크 탐색 전환
                                                wasPlayingBeforeSeek = exoPlayer.isPlaying
                                                if (exoPlayer.isPlaying) exoPlayer.pause()
                                                exoPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                                                isScrubbingBar = true
                                                scrubBarPositionMs = startSeekPos
                                            }
                                        }
                                    }

                                    when (mode) {
                                        ActiveDragMode.BRIGHTNESS -> {
                                            val newBri = GestureMath.computeBrightness(
                                                startBrightness = startBrightness,
                                                deltaY = dy,
                                                viewHeight = height,
                                            )
                                            applyBrightness(newBri)
                                            val pct = (newBri * 100f).roundToInt()
                                            hudState = HudOverlayState(
                                                icon = Icons.Default.BrightnessHigh,
                                                title = "밝기 $pct%",
                                                progress = newBri,
                                            )
                                            change.consume()
                                        }

                                        ActiveDragMode.VOLUME -> {
                                            val newVol = GestureMath.computeVolumeFraction(
                                                startFraction = startVol,
                                                deltaY = dy,
                                                viewHeight = height,
                                            )
                                            applyVolumeFraction(newVol)
                                            val pct = (newVol * 100f).roundToInt()
                                            hudState = HudOverlayState(
                                                icon = Icons.AutoMirrored.Filled.VolumeUp,
                                                title = "볼륨 $pct%",
                                                progress = newVol,
                                            )
                                            change.consume()
                                        }

                                        ActiveDragMode.SEEK -> {
                                            val dur = latestDurationMs.coerceAtLeast(0L)
                                            val deltaMs = GestureMath.computeHorizontalSeekDeltaMs(
                                                deltaX = curX - longPressAnchorX,
                                                viewWidth = width,
                                                durationMs = dur,
                                                seekSeconds = latestSettings.dragSeekSeconds,
                                            )
                                            val target = (startSeekPos + deltaMs).coerceIn(
                                                0L,
                                                if (dur > 0L) dur else Long.MAX_VALUE
                                            )
                                            pendingSeekTarget = target
                                            scrubBarPositionMs = target
                                            positionMs = target
                                            val actualDelta = target - startSeekPos
                                            hudState = HudOverlayState(
                                                icon = if (actualDelta >= 0L) {
                                                    Icons.Default.FastForward
                                                } else {
                                                    Icons.Default.FastRewind
                                                },
                                                title = Format.duration(target),
                                                subtitle = Format.signedDuration(actualDelta),
                                                progress = if (dur > 0L) {
                                                    (target.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                                                } else null,
                                            )
                                            val tNow = SystemClock.uptimeMillis()
                                            if (tNow - lastSeekApplyAt >= 45L) {
                                                exoPlayer.seekTo(target)
                                                lastSeekApplyAt = tNow
                                            }
                                            change.consume()
                                        }

                                        ActiveDragMode.LONG_PRESS_SPEED -> {
                                            val newSpeed = GestureMath.computeLongPressDragSpeed(
                                                startSpeed = startSpeed,
                                                deltaX = curX - longPressAnchorX,
                                                viewWidth = width,
                                            )
                                            applySpeed(newSpeed)
                                            hudState = HudOverlayState(
                                                icon = Icons.Default.Speed,
                                                title = Format.speed(newSpeed),
                                                subtitle = "x0.5 ~ x6.0",
                                                progress = (newSpeed - GestureMath.MIN_SPEED) /
                                                    (GestureMath.MAX_SPEED - GestureMath.MIN_SPEED),
                                            )
                                            change.consume()
                                        }

                                        ActiveDragMode.NONE,
                                        ActiveDragMode.TWO_FINGER_ZOOM -> Unit
                                    }
                                }

                                // 포인터 UP 이후 마무리 처리
                                when (mode) {
                                    ActiveDragMode.SEEK -> {
                                        exoPlayer.setSeekParameters(SeekParameters.EXACT)
                                        exoPlayer.seekTo(pendingSeekTarget)
                                        positionMs = pendingSeekTarget
                                        isScrubbingBar = false
                                        if (wasPlayingBeforeSeek) {
                                            exoPlayer.play()
                                        }
                                        subtitleController.onPlaybackPosition(
                                            pendingSeekTarget,
                                            wasPlayingBeforeSeek,
                                            latestSettings,
                                        )
                                        hudState = null
                                    }

                                    ActiveDragMode.BRIGHTNESS,
                                    ActiveDragMode.VOLUME,
                                    ActiveDragMode.TWO_FINGER_ZOOM -> {
                                        hudState = null
                                    }

                                    ActiveDragMode.LONG_PRESS_SPEED -> {
                                        if (latestSettings.resetSpeedOnLongPressRelease) {
                                            applySpeed(1.0f)
                                        }
                                        hudState = null
                                    }

                                    ActiveDragMode.NONE -> {
                                        if (!gestureEndedCleanly) continue
                                        if (isDoubleTapCandidate) {
                                            // 더블터치 확정!
                                            lastSingleTapJobPending = false
                                            latestHandleDoubleTap(pendingTapX, width, effectiveFps)
                                        } else {
                                            // 싱글 탭 후보 -> 더블터치 윈도우 동안 다음 터치 대기
                                            lastSingleTapJobPending = true
                                            pendingTapTime = SystemClock.uptimeMillis()
                                            pendingTapX = startX
                                            val nextEvent: PointerEvent? = withTimeoutOrNull(
                                                viewConfig.doubleTapTimeoutMillis
                                            ) {
                                                var pressedEvent: PointerEvent? = null
                                                while (pressedEvent == null) {
                                                    val e = awaitPointerEvent()
                                                    if (e.type == PointerEventType.Press) {
                                                        pressedEvent = e
                                                    }
                                                }
                                                pressedEvent
                                            }
                                            if (nextEvent == null) {
                                                // 더블터치가 안 들어왔으므로 싱글 탭 확정
                                                lastSingleTapJobPending = false
                                                controlsVisible = !controlsVisible
                                            } else {
                                                // 두 번째 탭이 시작됨 -> 곧바로 더블터치 처리
                                                val secondChange = nextEvent.changes.firstOrNull()
                                                val secondX = secondChange?.position?.x ?: startX
                                                // 두 번째 탭의 UP 기다리기
                                                while (true) {
                                                    val e = awaitPointerEvent()
                                                    val ch = e.changes.firstOrNull() ?: break
                                                    if (!ch.pressed) break
                                                }
                                                lastSingleTapJobPending = false
                                                latestHandleDoubleTap(secondX, width, effectiveFps)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
            )
        }

        // 3. 자막 및 AI 번역 자막 오버레이
        if (subtitlesVisible) {
            val origLine = activeCue?.originalText?.takeIf { it.isNotBlank() } ?: embeddedOrig
            val transLine = activeCue?.translatedText?.takeIf { it.isNotBlank() } ?: embeddedTrans
            if (origLine.isNotBlank() || transLine.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            horizontal = 20.dp,
                            vertical = if (controlsVisible && !isInPipMode) 88.dp else 22.dp,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val showTranslated = settings.aiTranslateEnabled && transLine.isNotBlank()
                    val showOriginal = if (showTranslated) settings.dualSubtitles else true
                    if (showOriginal && origLine.isNotBlank() && origLine != transLine) {
                        SubtitleTextLine(
                            text = origLine,
                            fontSizeSp = if (showTranslated) (settings.subtitleFontSizeSp - 3).coerceAtLeast(12) else settings.subtitleFontSizeSp,
                            color = if (showTranslated) Color(0xFFE0E0E0) else Color.White,
                        )
                    }
                    if (showTranslated) {
                        Spacer(Modifier.height(2.dp))
                        SubtitleTextLine(
                            text = transLine,
                            fontSizeSp = settings.subtitleFontSizeSp,
                            color = Color(0xFFFFEB3B),
                        )
                    }
                }
            }
        }

        // 3-1. 우측 상단 구석 상태 & 안내 배지 (가로보기·이어보기·화면비율 등 안내 + AI 번역 중 상태)
        val showCornerAiBadge = subtitlesVisible &&
            !controlsVisible &&
            !isInPipMode &&
            !showTrimSheet &&
            settings.aiTranslateEnabled &&
            subtitleStatus.contains("중")
        val notice = cornerNotice
        if (!isInPipMode && (showCornerAiBadge || notice != null)) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(
                        top = if (controlsVisible && !isLocked && !showTrimSheet) 52.dp else 10.dp,
                        end = 12.dp,
                    ),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (showCornerAiBadge) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.32f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = subtitleStatus,
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                AnimatedVisibility(
                    visible = notice != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    if (notice != null) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.56f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.widthIn(max = 260.dp),
                        ) {
                            Text(
                                text = notice.message,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
            }
        }

        // 4. 더블터치 좌/우 컴팩트 반투명 피드백
        val dtf = doubleTapFeedback
        if (dtf != null && !isInPipMode) {
            Surface(
                modifier = Modifier
                    .align(if (dtf.isRightSide) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 24.dp),
                color = Color.Black.copy(alpha = 0.42f),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = when {
                            dtf.isFileSwitch && dtf.isRightSide -> Icons.Default.SkipNext
                            dtf.isFileSwitch -> Icons.Default.SkipPrevious
                            dtf.isRightSide -> Icons.Default.FastForward
                            else -> Icons.Default.FastRewind
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = dtf.label,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // 5. 컴팩트 반투명 하단 제스처 HUD (밝기 / 볼륨 / 프레임 이동 / 길게터치 배속)
        val hud = hudState
        if (hud != null && !isInPipMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (controlsVisible) 92.dp else 44.dp),
                color = Color.Black.copy(alpha = 0.44f),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = hud.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = hud.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (hud.subtitle.isNotBlank()) {
                        Text(
                            text = hud.subtitle,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                        )
                    }
                    if (hud.progress != null) {
                        LinearProgressIndicator(
                            progress = { hud.progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .width(52.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF42A5F5),
                            trackColor = Color.White.copy(alpha = 0.25f),
                        )
                    }
                }
            }
        }

        // 6. 잠금 모드 해제 버튼
        if (isLocked && controlsVisible && !isInPipMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .clickable {
                        isLocked = false
                        controlsVisible = true
                    },
                color = Color.Black.copy(alpha = 0.5f),
                shape = CircleShape,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "잠금 해제",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Text("잠금 해제", color = Color.White, fontSize = 11.sp)
                }
            }
        }

        // 7. 컴팩트 상/하단 컨트롤 오버레이 (화면 터치 시 표시)
        AnimatedVisibility(
            visible = controlsVisible && !isLocked && !isInPipMode && !showTrimSheet,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 슬림 상단 바
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.62f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onExit,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        Text(
                            text = currentVideo.title,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val infoLine = buildString {
                            if (playlist.size > 1) append("${currentIndex + 1}/${playlist.size}")
                            if (settings.aiTranslateEnabled) {
                                if (isNotEmpty()) append(" · ")
                                append("AI(${settings.targetLangCode})")
                                if (subtitleStatus.isNotBlank()) {
                                    append(" · $subtitleStatus")
                                }
                            }
                            if (repeatA != null) {
                                if (isNotEmpty()) append(" · ")
                                append(if (repeatB == null) "구간A" else "구간반복")
                            }
                            if (settings.backgroundPlayOnScreenLock) {
                                if (isNotEmpty()) append(" · ")
                                append("백그라운드")
                            }
                            if (sleepRemainingSec != null) {
                                if (isNotEmpty()) append(" · ")
                                append("${sleepRemainingSec!! / 60}분")
                            }
                        }
                        if (infoLine.isNotBlank()) {
                            Text(
                                text = infoLine,
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // 배속 칩
                    Surface(
                        color = if (abs(playbackSpeed - 1.0f) > 0.01f) {
                            Color(0xFF1E88E5).copy(alpha = 0.85f)
                        } else {
                            Color.White.copy(alpha = 0.16f)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clickable { showSpeedSheet = true },
                    ) {
                        Text(
                            text = Format.speed(playbackSpeed),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }

                    // 동영상 잘라내기 버튼
                    IconButton(
                        onClick = {
                            exoPlayer.pause()
                            showTrimSheet = true
                        },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "동영상 잘라내기",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp),
                        )
                    }

                    // 즐겨찾기 & 장면 북마크 버튼
                    IconButton(
                        onClick = { showBookmarkSheet = true },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = if (isCurrentFavorite || currentVideoBookmarks.isNotEmpty()) {
                                Icons.Default.Bookmark
                            } else {
                                Icons.Default.BookmarkAdd
                            },
                            contentDescription = "즐겨찾기 및 북마크",
                            tint = if (isCurrentFavorite || currentVideoBookmarks.isNotEmpty()) {
                                Color(0xFFFFCA28)
                            } else {
                                Color.White
                            },
                            modifier = Modifier.size(19.dp),
                        )
                    }

                    // AI 번역 버튼
                    IconButton(
                        onClick = { showAiSheet = true },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "AI 자막 번역",
                            tint = if (settings.aiTranslateEnabled) Color(0xFF64B5F6) else Color.White,
                            modifier = Modifier.size(19.dp),
                        )
                    }

                    // 동영상 삭제 버튼 (요구사항 3)
                    IconButton(
                        onClick = {
                            exoPlayer.pause()
                            showDeleteDialog = true
                        },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "동영상 삭제",
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(19.dp),
                        )
                    }

                    // 부가 기능 통합 더보기 메뉴
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "더보기",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("동영상 구간 잘라내기") },
                                leadingIcon = { Icon(Icons.Default.ContentCut, null) },
                                onClick = {
                                    showMoreMenu = false
                                    exoPlayer.pause()
                                    showTrimSheet = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("오디오 · 내장 자막 트랙") },
                                leadingIcon = { Icon(Icons.Default.Audiotrack, null) },
                                onClick = { showMoreMenu = false; showTrackSheet = true },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when {
                                            repeatA == null -> "구간 반복 설정 (A-B)"
                                            repeatB == null -> "반복 끝점 B 지정 (A: ${Format.duration(repeatA!!)})"
                                            else -> "구간 반복 해제"
                                        }
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.Repeat, null) },
                                onClick = {
                                    showMoreMenu = false
                                    val now = exoPlayer.currentPosition
                                    when {
                                        repeatA == null -> {
                                            repeatA = now
                                            showNotice("반복 시작점(A) 설정")
                                        }
                                        repeatB == null -> {
                                            if (now > repeatA!! + 300L) {
                                                repeatB = now
                                                exoPlayer.seekTo(repeatA!!)
                                                showNotice("구간 반복 시작")
                                            } else {
                                                showNotice("A 이후 지점에서 B를 눌러주세요.")
                                            }
                                        }
                                        else -> {
                                            repeatA = null
                                            repeatB = null
                                            showNotice("구간 반복 해제")
                                        }
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("PIP 팝업 창 모드") },
                                leadingIcon = { Icon(Icons.Default.PictureInPictureAlt, null) },
                                onClick = { showMoreMenu = false; enterPip() },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (settings.backgroundPlayOnScreenLock) {
                                            "화면 잠금 시 백그라운드 재생 (켜짐)"
                                        } else {
                                            "화면 잠금 시 백그라운드 재생 (꺼짐)"
                                        }
                                    )
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.VolumeUp, null) },
                                onClick = {
                                    showMoreMenu = false
                                    val next = !settings.backgroundPlayOnScreenLock
                                    if (next) {
                                        BackgroundPlaybackService.requestNotificationPermissionIfNeeded(context)
                                        if (exoPlayer.isPlaying) {
                                            hasStartedBackgroundSession = true
                                        }
                                    } else {
                                        hasStartedBackgroundSession = false
                                    }
                                    App.settings.update { it.copy(backgroundPlayOnScreenLock = next) }
                                    showNotice(
                                        if (next) "화면 잠금 시 백그라운드 재생 켜짐" else "화면 잠금 시 백그라운드 재생 꺼짐"
                                    )
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(sleepRemainingSec?.let { "취침 타이머 (${it / 60}분 남음)" } ?: "취침 타이머")
                                },
                                leadingIcon = { Icon(Icons.Default.Timer, null) },
                                onClick = { showMoreMenu = false; showSleepDialog = true },
                            )
                            DropdownMenuItem(
                                text = { Text("플레이어 & AI 설정") },
                                leadingIcon = { Icon(Icons.Default.Settings, null) },
                                onClick = { showMoreMenu = false; onOpenSettings() },
                            )
                        }
                    }
                }

                // 슬림 하단 바
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.64f))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    // 시간 탐색 슬라이더
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val shownPos = if (isScrubbingBar) scrubBarPositionMs else positionMs
                        Text(
                            text = Format.duration(shownPos),
                            color = Color.White,
                            fontSize = 11.sp,
                        )
                        Slider(
                            value = if (durationMs > 0L) {
                                (shownPos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                            } else 0f,
                            onValueChange = { ratio ->
                                if (!isScrubbingBar) {
                                    wasPlayingBeforeBarScrub = exoPlayer.isPlaying
                                    if (exoPlayer.isPlaying) exoPlayer.pause()
                                    exoPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                                    isScrubbingBar = true
                                    lastBarSeekAt = 0L
                                }
                                val target = (ratio * durationMs).roundToLong()
                                scrubBarPositionMs = target
                                positionMs = target
                                val now = SystemClock.uptimeMillis()
                                if (now - lastBarSeekAt >= 45L) {
                                    exoPlayer.seekTo(target)
                                    lastBarSeekAt = now
                                }
                            },
                            onValueChangeFinished = {
                                exoPlayer.setSeekParameters(SeekParameters.EXACT)
                                exoPlayer.seekTo(scrubBarPositionMs)
                                positionMs = scrubBarPositionMs
                                isScrubbingBar = false
                                if (wasPlayingBeforeBarScrub) {
                                    exoPlayer.play()
                                }
                                subtitleController.onPlaybackPosition(
                                    scrubBarPositionMs,
                                    wasPlayingBeforeBarScrub,
                                    latestSettings,
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .padding(horizontal = 6.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF42A5F5),
                                activeTrackColor = Color(0xFF42A5F5),
                                inactiveTrackColor = Color.White.copy(alpha = 0.28f),
                            ),
                        )
                        Text(
                            text = Format.duration(durationMs),
                            color = Color.White,
                            fontSize = 11.sp,
                        )
                    }

                    val stepLabel = if (settings.buttonSeekUnit == SeekUnit.SECONDS) {
                        "${settings.buttonSeekSeconds}초"
                    } else {
                        "${settings.buttonSeekFrames}F"
                    }

                    // 핵심 컴팩트 컨트롤 한 줄: 잠금 / 회전 / 뒤로이동 / 이전 / 재생 / 다음 / 앞으로이동 / 자막 / 비율
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { isLocked = true },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.Default.LockOpen,
                                contentDescription = "화면 잠금",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        IconButton(
                            onClick = {
                                val next = ScreenOrientationMode.entries[
                                    (orientationMode.ordinal + 1) % ScreenOrientationMode.entries.size
                                ]
                                orientationMode = next
                                showNotice(next.label)
                            },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.Default.ScreenRotation,
                                contentDescription = orientationMode.label,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        TextButton(
                            onClick = { stepByButton(forward = false) },
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.height(32.dp),
                        ) {
                            Text("-$stepLabel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        IconButton(
                            onClick = { if (currentIndex > 0) goToVideo(currentIndex - 1) },
                            enabled = currentIndex > 0,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "이전 동영상",
                                tint = if (currentIndex > 0) Color.White else Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(24.dp),
                            )
                        }

                        Surface(
                            color = Color.White.copy(alpha = 0.18f),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(38.dp)
                                .clickable {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "일시정지" else "재생",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }

                        IconButton(
                            onClick = { if (currentIndex + 1 < playlist.size) goToVideo(currentIndex + 1) },
                            enabled = currentIndex + 1 < playlist.size,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "다음 동영상",
                                tint = if (currentIndex + 1 < playlist.size) {
                                    Color.White
                                } else {
                                    Color.White.copy(alpha = 0.35f)
                                },
                                modifier = Modifier.size(24.dp),
                            )
                        }

                        TextButton(
                            onClick = { stepByButton(forward = true) },
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.height(32.dp),
                        ) {
                            Text("+$stepLabel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        IconButton(
                            onClick = { subtitlesVisible = !subtitlesVisible },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.Default.Subtitles,
                                contentDescription = "자막 토글",
                                tint = if (subtitlesVisible) Color(0xFF64B5F6) else Color.White.copy(alpha = 0.55f),
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        IconButton(
                            onClick = {
                                if (abs(zoomScale - 1.0f) > 0.05f) {
                                    zoomScale = 1.0f
                                    showNotice("화면 크기 100% 초기화 (${scaleMode.label})")
                                } else {
                                    val next = VideoScaleMode.entries[(scaleMode.ordinal + 1) % VideoScaleMode.entries.size]
                                    scaleMode = next
                                    showNotice("화면 비율: ${next.label}")
                                }
                            },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.Default.AspectRatio,
                                contentDescription = "화면 비율",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }

        // 8. 동영상 구간 잘라내기 & 실시간 미리보기 오버레이 패널 (상단 영상 노출 유지)
        if (showTrimSheet && !isInPipMode) {
            val totalMs = durationMs.coerceAtLeast(exoPlayer.duration.coerceAtLeast(2_000L))
            val initStart = remember(currentVideo.id, showTrimSheet) {
                val a = repeatA
                val b = repeatB
                if (a != null && b != null && b > a + 500L) {
                    a.coerceIn(0L, totalMs - 500L)
                } else {
                    val cur = exoPlayer.currentPosition.coerceIn(0L, totalMs)
                    if (cur >= totalMs - 2_000L) (totalMs - 15_000L).coerceAtLeast(0L) else cur
                }
            }
            val initEnd = remember(currentVideo.id, showTrimSheet) {
                val a = repeatA
                val b = repeatB
                if (a != null && b != null && b > a + 500L) {
                    b.coerceIn(initStart + 500L, totalMs)
                } else {
                    (initStart + 15_000L).coerceIn(initStart + 500L, totalMs)
                }
            }

            var trimStartMs by remember(currentVideo.id, showTrimSheet) { mutableLongStateOf(initStart) }
            var trimEndMs by remember(currentVideo.id, showTrimSheet) { mutableLongStateOf(initEnd) }
            var outputName by remember(currentVideo.id, showTrimSheet) {
                mutableStateOf("${File(currentVideo.title).nameWithoutExtension}_cut")
            }
            var isPreviewingTrim by remember(currentVideo.id, showTrimSheet) { mutableStateOf(false) }
            var isTrimming by remember { mutableStateOf(false) }
            var lastSavedClip by remember { mutableStateOf<File?>(null) }
            var startThumb by remember(currentVideo.id) { mutableStateOf<ImageBitmap?>(null) }
            var endThumb by remember(currentVideo.id) { mutableStateOf<ImageBitmap?>(null) }

            // 시작 지점 프레임 썸네일 추출 (디바운스)
            LaunchedEffect(currentVideo.id, trimStartMs) {
                delay(120L)
                startThumb = withContext(Dispatchers.IO) {
                    extractPreviewFrame(context, currentVideo, trimStartMs)
                }
            }
            // 끝 지점 프레임 썸네일 추출 (디바운스)
            LaunchedEffect(currentVideo.id, trimEndMs) {
                delay(120L)
                endThumb = withContext(Dispatchers.IO) {
                    extractPreviewFrame(context, currentVideo, trimEndMs)
                }
            }

            // 구간 미리보기 재생 루프 (trimStartMs ~ trimEndMs 구간만 반복 재생)
            LaunchedEffect(isPreviewingTrim, trimStartMs, trimEndMs) {
                if (!isPreviewingTrim) return@LaunchedEffect
                exoPlayer.seekTo(trimStartMs)
                exoPlayer.play()
                while (isPreviewingTrim) {
                    val pos = exoPlayer.currentPosition
                    positionMs = pos
                    if (pos >= trimEndMs || pos < trimStartMs - 500L) {
                        exoPlayer.seekTo(trimStartMs)
                    }
                    delay(80L)
                }
            }

            fun previewSeekTo(targetMs: Long) {
                isPreviewingTrim = false
                if (exoPlayer.isPlaying) exoPlayer.pause()
                exoPlayer.seekTo(targetMs)
                positionMs = targetMs
            }

            val fps = if (videoFps in 1f..240f) videoFps else latestSettings.defaultFps
            val oneFrameMs = GestureMath.framesToMs(1L, fps).coerceAtLeast(16L)

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                color = Color.Black.copy(alpha = 0.80f),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 1. 헤더 & 선택 구간 정보
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "구간 잘라내기 & 미리보기",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            val curPreviewStr = if (isPreviewingTrim) {
                                " · 미리보기 ${Format.duration(positionMs)}"
                            } else {
                                ""
                            }
                            Text(
                                text = "${Format.duration(trimStartMs)} ~ ${Format.duration(trimEndMs)} (길이 ${Format.duration((trimEndMs - trimStartMs).coerceAtLeast(0L))})$curPreviewStr",
                                color = Color(0xFF64B5F6),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        IconButton(
                            onClick = {
                                if (!isTrimming) {
                                    isPreviewingTrim = false
                                    showTrimSheet = false
                                }
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
                        }
                    }

                    // 2. 시작 장면 썸네일 + [▶ 구간 미리보기] 버튼 + 끝 장면 썸네일
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TrimFramePreviewChip(
                            label = "시작 ${Format.duration(trimStartMs)}",
                            bitmap = startThumb,
                            onClick = { previewSeekTo(trimStartMs) },
                        )

                        Surface(
                            color = if (isPreviewingTrim) Color(0xFFE53935) else Color(0xFF1E88E5),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.clickable(enabled = !isTrimming) {
                                if (isPreviewingTrim) {
                                    isPreviewingTrim = false
                                    exoPlayer.pause()
                                } else {
                                    isPreviewingTrim = true
                                }
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(
                                    imageVector = if (isPreviewingTrim) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = if (isPreviewingTrim) "미리보기 정지" else "구간 미리보기",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        TrimFramePreviewChip(
                            label = "끝 ${Format.duration(trimEndMs)}",
                            bitmap = endThumb,
                            onClick = { previewSeekTo(trimEndMs) },
                        )
                    }

                    // 3. 구간 슬라이더 + 미리보기 재생 위치 표시 바
                    RangeSlider(
                        value = trimStartMs.toFloat()..trimEndMs.toFloat(),
                        onValueChange = { range ->
                            val s = range.start.roundToLong().coerceIn(0L, totalMs - 500L)
                            val e = range.endInclusive.roundToLong().coerceIn(s + 500L, totalMs)
                            val previewPos = if (abs(s - trimStartMs) > abs(e - trimEndMs)) s else e
                            trimStartMs = s
                            trimEndMs = e
                            previewSeekTo(previewPos)
                        },
                        valueRange = 0f..totalMs.toFloat(),
                        enabled = !isTrimming,
                        modifier = Modifier.height(28.dp),
                    )
                    if (isPreviewingTrim && trimEndMs > trimStartMs) {
                        val previewRatio = ((positionMs - trimStartMs).toFloat() / (trimEndMs - trimStartMs).toFloat())
                            .coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { previewRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF64B5F6),
                            trackColor = Color.White.copy(alpha = 0.2f),
                        )
                    }

                    // 4. 시작/끝 초·프레임 미세 조절 버튼
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("시작:", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        TrimStepChip("-1초", enabled = !isTrimming) {
                            trimStartMs = (trimStartMs - 1_000L).coerceAtLeast(0L)
                            previewSeekTo(trimStartMs)
                        }
                        TrimStepChip("-1F", enabled = !isTrimming) {
                            trimStartMs = (trimStartMs - oneFrameMs).coerceAtLeast(0L)
                            previewSeekTo(trimStartMs)
                        }
                        TrimStepChip("+1F", enabled = !isTrimming) {
                            trimStartMs = (trimStartMs + oneFrameMs).coerceAtMost(trimEndMs - 500L)
                            previewSeekTo(trimStartMs)
                        }
                        TrimStepChip("+1초", enabled = !isTrimming) {
                            trimStartMs = (trimStartMs + 1_000L).coerceAtMost(trimEndMs - 500L)
                            previewSeekTo(trimStartMs)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("끝:", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        TrimStepChip("-1초", enabled = !isTrimming) {
                            trimEndMs = (trimEndMs - 1_000L).coerceAtLeast(trimStartMs + 500L)
                            previewSeekTo(trimEndMs)
                        }
                        TrimStepChip("-1F", enabled = !isTrimming) {
                            trimEndMs = (trimEndMs - oneFrameMs).coerceAtLeast(trimStartMs + 500L)
                            previewSeekTo(trimEndMs)
                        }
                        TrimStepChip("+1F", enabled = !isTrimming) {
                            trimEndMs = (trimEndMs + oneFrameMs).coerceAtMost(totalMs)
                            previewSeekTo(trimEndMs)
                        }
                        TrimStepChip("+1초", enabled = !isTrimming) {
                            trimEndMs = (trimEndMs + 1_000L).coerceAtMost(totalMs)
                            previewSeekTo(trimEndMs)
                        }
                    }

                    // 5. 저장 파일명 & 현재위치 지정
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = outputName,
                            onValueChange = { outputName = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("저장 파일명 (.mp4)", fontSize = 11.sp) },
                            singleLine = true,
                            enabled = !isTrimming,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF64B5F6),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                                focusedLabelColor = Color(0xFF64B5F6),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                            ),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            TrimStepChip("현재→시작", enabled = !isTrimming) {
                                trimStartMs = exoPlayer.currentPosition.coerceIn(0L, trimEndMs - 500L)
                            }
                            TrimStepChip("현재→끝", enabled = !isTrimming) {
                                trimEndMs = exoPlayer.currentPosition.coerceIn(trimStartMs + 500L, totalMs)
                            }
                        }
                    }

                    if (isTrimming) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("무손실 잘라내기 처리 중...", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    val savedFile = lastSavedClip
                    if (savedFile != null) {
                        Surface(
                            color = Color(0xFF1E88E5).copy(alpha = 0.28f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "저장됨: ${savedFile.name}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    onClick = {
                                        isPreviewingTrim = false
                                        val newItem = VideoScanner.videoItemFromFile(savedFile, extractMetadata = true)
                                        playlist = listOf(newItem) + playlist
                                        currentIndex = 0
                                        showTrimSheet = false
                                    },
                                ) { Text("바로 재생", color = Color(0xFF90CAF9)) }
                            }
                        }
                    }

                    // 6. 하단 저장 버튼
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                isPreviewingTrim = false
                                showTrimSheet = false
                            },
                            enabled = !isTrimming,
                            modifier = Modifier.weight(1f),
                        ) { Text("닫기", color = Color.White) }
                        Button(
                            onClick = {
                                isPreviewingTrim = false
                                exoPlayer.pause()
                                isTrimming = true
                                lastSavedClip = null
                                scope.launch {
                                    val res = VideoTrimmer.trimVideo(
                                        context = context,
                                        video = currentVideo,
                                        startMs = trimStartMs,
                                        endMs = trimEndMs,
                                        desiredFileName = outputName,
                                        cues = allCues,
                                    )
                                    isTrimming = false
                                    res.onSuccess { outFile ->
                                        lastSavedClip = outFile
                                        onDeletedVideo(currentVideo.id)
                                        showNotice("잘라내기 완료: ${outFile.name}", longDuration = true)
                                    }.onFailure { err ->
                                        showNotice(err.message ?: "잘라내기에 실패했습니다.", longDuration = true)
                                    }
                                }
                            },
                            enabled = !isTrimming && outputName.isNotBlank() && trimEndMs > trimStartMs + 300L,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.ContentCut, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("잘라내기 저장")
                        }
                    }
                }
            }
        }
    }

    // 삭제 확인 다이얼로그 (요구사항 3)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("동영상 삭제") },
            text = { Text("'${currentVideo.title}' 파일을 삭제하시겠습니까?\n삭제 후에는 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        val targetToDelete = currentVideo
                        scope.launch {
                            val targets = if (targetToDelete.path.isNotBlank()) {
                                listOf(File(targetToDelete.path))
                            } else {
                                emptyList()
                            }
                            val res = withContext(Dispatchers.IO) {
                                FileExplorerManager.deleteEntries(
                                    targets = targets,
                                    context = context,
                                    fallbackUris = listOf(targetToDelete.uri),
                                )
                            }
                            res.onSuccess {
                                App.history.removeByPathOrId(targetToDelete.id)
                                App.bookmarks.removeForDeletedPath(targetToDelete.id)
                                onDeletedVideo(targetToDelete.id)
                                val updatedList = playlist.filterNot { it.id == targetToDelete.id }
                                if (updatedList.isEmpty()) {
                                    Toast.makeText(context, "동영상을 삭제했습니다.", Toast.LENGTH_SHORT).show()
                                    onExit()
                                } else {
                                    showNotice("동영상을 삭제했습니다.")
                                    playlist = updatedList
                                    currentIndex = currentIndex.coerceIn(0, updatedList.lastIndex)
                                }
                            }.onFailure { err ->
                                showNotice(err.message ?: "삭제하지 못했습니다.", longDuration = true)
                            }
                        }
                    },
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            },
        )
    }

    // 배속 직접 선택 바텀 시트
    if (showSpeedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("재생 속도 (${Format.speed(playbackSpeed)})", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { applySpeed(1.0f) }) { Text("x1.0 초기화") }
                }
                Slider(
                    value = playbackSpeed,
                    onValueChange = { raw ->
                        val snapped = ((raw / GestureMath.SPEED_STEP).roundToInt() * GestureMath.SPEED_STEP)
                        applySpeed(snapped)
                    },
                    valueRange = GestureMath.MIN_SPEED..GestureMath.MAX_SPEED,
                    steps = 10,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 4.0f, 5.0f, 6.0f).forEach { preset ->
                        FilterChip(
                            selected = abs(playbackSpeed - preset) < 0.05f,
                            onClick = { applySpeed(preset) },
                            label = { Text(Format.speed(preset)) },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // AI 번역 컨트롤 시트
    if (showAiSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAiSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("AI 자동 자막 번역", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("실시간 AI 번역 활성화", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = settings.aiTranslateEnabled,
                        onCheckedChange = { on ->
                            App.settings.update { it.copy(aiTranslateEnabled = on) }
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("원문 + 번역 이중 자막 표시", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = settings.dualSubtitles,
                        onCheckedChange = { dual ->
                            App.settings.update { it.copy(dualSubtitles = dual) }
                        },
                    )
                }
                Text("번역 대상 언어", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SupportedLanguages.items.forEach { (code, name) ->
                        FilterChip(
                            selected = settings.targetLangCode == code,
                            onClick = { App.settings.update { it.copy(targetLangCode = code) } },
                            label = { Text(name) },
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = "자막 파일: ${loadedSubName ?: "없음 (내장 자막 또는 AI 음성 인식)"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (subtitleStatus.isNotBlank()) {
                    Text(
                        text = "상태: $subtitleStatus",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showSubtitleFileDialog = true }) {
                        Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("같은 폴더 자막 선택")
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                subtitleController.exportTranslatedSrt(settings.targetLangCode)
                                    .onSuccess { out ->
                                        Toast.makeText(
                                            context,
                                            "자막 저장됨: ${out.name}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    .onFailure {
                                        Toast.makeText(
                                            context,
                                            it.message ?: "저장 실패",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        },
                    ) {
                        Text("번역 SRT 저장")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("자막 싱크: ${syncOffsetMs / 1000.0}초", modifier = Modifier.weight(1f))
                    TextButton(onClick = { subtitleController.adjustSync(-500L) }) { Text("-0.5초") }
                    TextButton(onClick = { subtitleController.resetSync() }) { Text("0") }
                    TextButton(onClick = { subtitleController.adjustSync(500L) }) { Text("+0.5초") }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { showAiSheet = false; onOpenSettings() }) {
                        Text("API 키 / 상세 설정")
                    }
                    TextButton(onClick = { showAiSheet = false }) {
                        Text("닫기")
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // 오디오 / 내장 자막 트랙 선택 시트
    if (showTrackSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTrackSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("오디오 및 내장 자막 트랙", style = MaterialTheme.typography.titleLarge)

                Text("오디오 트랙", style = MaterialTheme.typography.titleSmall)
                if (audioTracks.isEmpty()) {
                    Text("선택 가능한 오디오 트랙이 없습니다.", style = MaterialTheme.typography.bodySmall)
                } else {
                    audioTracks.forEach { opt ->
                        FilterChip(
                            selected = opt.selected,
                            onClick = {
                                val group = exoPlayer.currentTracks.groups.getOrNull(opt.groupIndex)
                                if (group != null) {
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .setOverrideForType(
                                            TrackSelectionOverride(group.mediaTrackGroup, opt.trackIndex)
                                        )
                                        .build()
                                }
                            },
                            label = { Text(opt.label) },
                        )
                    }
                }

                HorizontalDivider()

                Text("내장 자막 트랙", style = MaterialTheme.typography.titleSmall)
                if (textTracks.isEmpty()) {
                    Text("내장 자막 트랙이 없습니다.", style = MaterialTheme.typography.bodySmall)
                } else {
                    textTracks.forEach { opt ->
                        FilterChip(
                            selected = opt.selected,
                            onClick = {
                                val group = exoPlayer.currentTracks.groups.getOrNull(opt.groupIndex)
                                if (group != null) {
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .setOverrideForType(
                                            TrackSelectionOverride(group.mediaTrackGroup, opt.trackIndex)
                                        )
                                        .build()
                                }
                            },
                            label = { Text(opt.label) },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // 같은 폴더 내 자막 파일 선택 다이얼로그
    if (showSubtitleFileDialog) {
        val dirFiles = remember(currentVideo.path) {
            val dir = File(currentVideo.path).parentFile
            dir?.listFiles().orEmpty().filter { VideoScanner.isSubtitleFile(it) }.sortedBy { it.name }
        }
        AlertDialog(
            onDismissRequest = { showSubtitleFileDialog = false },
            title = { Text("자막 파일 선택") },
            text = {
                if (dirFiles.isEmpty()) {
                    Text("현재 폴더에 .srt / .vtt / .ass 자막 파일이 없습니다.")
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        dirFiles.forEach { sub ->
                            TextButton(
                                onClick = {
                                    showSubtitleFileDialog = false
                                    scope.launch {
                                        subtitleController.loadSubtitleFile(sub, latestSettings)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(sub.name, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleFileDialog = false }) { Text("닫기") }
            },
        )
    }

    // 취침 타이머 다이얼로그
    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = { Text("취침 타이머") },
            text = {
                Column {
                    listOf(15, 30, 45, 60).forEach { min ->
                        TextButton(
                            onClick = {
                                sleepRemainingSec = min * 60
                                showSleepDialog = false
                                showNotice("${min}분 후 정지됩니다.")
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${min}분 후 일시정지")
                        }
                    }
                    if (sleepRemainingSec != null) {
                        TextButton(
                            onClick = {
                                sleepRemainingSec = null
                                showSleepDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("타이머 해제", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepDialog = false }) { Text("닫기") }
            },
        )
    }

    // 즐겨찾기 & 장면 북마크 바텀 시트
    if (showBookmarkSheet) {
        var bookmarkNote by remember { mutableStateOf("") }
        ModalBottomSheet(
            onDismissRequest = { showBookmarkSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "즐겨찾기 & 장면 북마크",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    FilterChip(
                        selected = isCurrentFavorite,
                        onClick = {
                            val added = App.bookmarks.toggleFavoriteVideo(currentVideo)
                            Toast.makeText(
                                context,
                                if (added) "즐겨찾는 동영상에 추가됨" else "즐겨찾기 해제됨",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isCurrentFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (isCurrentFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        label = { Text(if (isCurrentFavorite) "즐겨찾기됨" else "즐겨찾기") },
                    )
                }

                HorizontalDivider()

                val capturePosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                OutlinedTextField(
                    value = bookmarkNote,
                    onValueChange = { bookmarkNote = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("북마크 메모 (선택)") },
                    placeholder = { Text("예: 주요 장면, 다시 볼 구간") },
                    singleLine = true,
                )
                Button(
                    onClick = {
                        val targetPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                        App.bookmarks.addVideoBookmark(
                            video = currentVideo,
                            positionMs = targetPos,
                            note = bookmarkNote,
                        )
                        bookmarkNote = ""
                        Toast.makeText(
                            context,
                            "${Format.duration(targetPos)} 장면 북마크 저장됨",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.BookmarkAdd, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("현재 위치 북마크 추가 (${Format.duration(capturePosMs)})")
                }

                Text(
                    text = "이 영상의 북마크 (${currentVideoBookmarks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (currentVideoBookmarks.isEmpty()) {
                    Text(
                        text = "저장된 장면 북마크가 없습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    currentVideoBookmarks.forEach { bm ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    exoPlayer.seekTo(bm.positionMs)
                                    positionMs = bm.positionMs
                                    showBookmarkSheet = false
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = Format.duration(bm.positionMs),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = bm.note.ifBlank { "저장된 장면" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = { App.bookmarks.removeVideoBookmark(bm.bookmarkId) },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "북마크 삭제",
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun TrimFramePreviewChip(
    label: String,
    bitmap: ImageBitmap?,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 42.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(text = label, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
private fun TrimStepChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = Color.White.copy(alpha = if (enabled) 0.16f else 0.08f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = if (enabled) 1f else 0.4f),
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

private fun extractPreviewFrame(
    context: Context,
    video: VideoItem,
    timeMs: Long,
): ImageBitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        if (video.path.isNotBlank() && File(video.path).canRead()) {
            retriever.setDataSource(video.path)
        } else {
            retriever.setDataSource(context, Uri.parse(video.uri))
        }
        val timeUs = timeMs.coerceAtLeast(0L) * 1_000L
        val bmp: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                220,
                124,
            ) ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } else {
            retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
        bmp?.asImageBitmap()
    } catch (_: Throwable) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

@Composable
private fun SubtitleTextLine(
    text: String,
    fontSizeSp: Int,
    color: Color,
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSizeSp.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        style = TextStyle(
            shadow = Shadow(
                color = Color.Black,
                blurRadius = 6f,
            ),
        ),
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.56f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
