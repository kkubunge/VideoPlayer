package com.aiutil.videoplayer.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.media3.common.C
import androidx.media3.common.MediaItem
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
import com.aiutil.videoplayer.model.PlayerSettings
import com.aiutil.videoplayer.model.ScreenOrientationMode
import com.aiutil.videoplayer.model.SeekUnit
import com.aiutil.videoplayer.model.SupportedLanguages
import com.aiutil.videoplayer.model.TrackOption
import com.aiutil.videoplayer.model.VideoItem
import com.aiutil.videoplayer.model.VideoScaleMode
import com.aiutil.videoplayer.util.Format
import com.aiutil.videoplayer.util.GestureMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private enum class ActiveDragMode {
    NONE,
    BRIGHTNESS,
    VOLUME,
    SEEK,
    LONG_PRESS_SPEED,
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
)

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    initialVideo: VideoItem,
    initialPlaylist: List<VideoItem>,
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
        ExoPlayer.Builder(context).build().apply {
            setSeekParameters(SeekParameters.EXACT)
            playWhenReady = true
        }
    }
    val subtitleController = remember { AiSubtitleController(context, scope) }

    // 재생 상태
    var isPlaying by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(currentVideo.durationMs.coerceAtLeast(0L)) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var videoFps by remember { mutableFloatStateOf(settings.defaultFps) }
    var isScrubbingBar by remember { mutableStateOf(false) }
    var scrubBarPositionMs by remember { mutableLongStateOf(0L) }

    // UI 및 제어 상태
    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var scaleMode by remember { mutableStateOf(VideoScaleMode.FIT) }
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

    // HUD / 더블터치 피드백
    var hudState by remember { mutableStateOf<HudOverlayState?>(null) }
    var doubleTapFeedback by remember { mutableStateOf<DoubleTapFeedback?>(null) }
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

    fun saveCurrentProgress() {
        val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
        val dur = exoPlayer.duration.takeIf { it > 0L } ?: currentVideo.durationMs
        App.history.recordProgress(currentVideo, pos, dur)
    }

    fun goToVideo(index: Int) {
        if (index !in playlist.indices) return
        saveCurrentProgress()
        repeatA = null
        repeatB = null
        currentIndex = index
    }

    fun stepSingleFrame(forward: Boolean) {
        if (exoPlayer.isPlaying) exoPlayer.pause()
        val fps = if (videoFps in 1f..240f) videoFps else latestSettings.defaultFps
        val frameMs = GestureMath.framesToMs(1L, fps).coerceAtLeast(16L)
        val target = (exoPlayer.currentPosition + if (forward) frameMs else -frameMs)
            .coerceIn(0L, durationMs.coerceAtLeast(0L))
        exoPlayer.seekTo(target)
        positionMs = target
        hudState = HudOverlayState(
            icon = if (forward) Icons.Default.FastForward else Icons.Default.FastRewind,
            title = if (forward) "+1 프레임" else "-1 프레임",
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
            Toast.makeText(context, "이 기기에서는 PIP 모드를 지원하지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 몰입형 전체 화면 & 화면 켜짐 유지
    DisposableEffect(activity, view) {
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            saveCurrentProgress()
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            subtitleController.release()
            exoPlayer.release()
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
                    saveCurrentProgress()
                    if (currentIndex + 1 < playlist.size) {
                        goToVideo(currentIndex + 1)
                    } else {
                        controlsVisible = true
                    }
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
        val mediaUri = if (currentVideo.path.isNotBlank() && File(currentVideo.path).canRead()) {
            Uri.fromFile(File(currentVideo.path))
        } else {
            Uri.parse(currentVideo.uri)
        }
        exoPlayer.setMediaItem(MediaItem.fromUri(mediaUri))
        exoPlayer.prepare()
        if (latestSettings.resumePlayback) {
            val savedPos = App.history.getSavedPosition(currentVideo.id)
            if (savedPos > 1_500L) {
                exoPlayer.seekTo(savedPos)
                Toast.makeText(
                    context,
                    "이어보기 (${Format.duration(savedPos)})",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        exoPlayer.setPlaybackSpeed(playbackSpeed)
        exoPlayer.play()
        subtitleController.loadForVideo(currentVideo, latestSettings)
    }

    // AI 설정 변경 시 번역 갱신
    LaunchedEffect(
        settings.aiTranslateEnabled,
        settings.targetLangCode,
        settings.translationEngine,
        settings.geminiApiKey,
    ) {
        if (settings.aiTranslateEnabled) {
            subtitleController.pretranslateAround(exoPlayer.currentPosition, settings)
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
            Toast.makeText(context, "취침 타이머로 재생이 일시정지되었습니다.", Toast.LENGTH_SHORT).show()
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
        if (isLocked) {
            controlsVisible = true
            Toast.makeText(context, "화면 잠금을 먼저 해제해 주세요.", Toast.LENGTH_SHORT).show()
        } else {
            onExit()
        }
    }

    val latestIsLocked by rememberUpdatedState(isLocked)
    val latestPlaybackSpeed by rememberUpdatedState(playbackSpeed)
    val latestDurationMs by rememberUpdatedState(durationMs)
    val latestVideoFps by rememberUpdatedState(videoFps)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 2. 제스처 인식 레이어 (밝기 / 볼륨 / 길게터치 배속 / 프레임 좌우드래그 / 더블터치 / 싱글탭)
        if (!isInPipMode) {
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
                                var gestureEndedCleanly = false

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
                                            subtitle = "좌/우 드래그 (x0.50 ~ x6.00)",
                                            progress = (latestPlaybackSpeed - GestureMath.MIN_SPEED) /
                                                (GestureMath.MAX_SPEED - GestureMath.MIN_SPEED),
                                        )
                                        continue
                                    }

                                    val change = event.changes.firstOrNull { it.id == downChange.id }
                                        ?: event.changes.firstOrNull()
                                    if (change == null || !change.pressed) {
                                        gestureEndedCleanly = true
                                        break
                                    }

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
                                                icon = Icons.Default.VolumeUp,
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
                                            )
                                            val target = (startSeekPos + deltaMs).coerceIn(
                                                0L,
                                                if (dur > 0L) dur else Long.MAX_VALUE
                                            )
                                            pendingSeekTarget = target
                                            val actualDelta = target - startSeekPos
                                            val deltaFrames = GestureMath.msToFrames(actualDelta, effectiveFps)
                                            val frameSign = if (deltaFrames >= 0) "+" else ""
                                            hudState = HudOverlayState(
                                                icon = if (actualDelta >= 0L) {
                                                    Icons.Default.FastForward
                                                } else {
                                                    Icons.Default.FastRewind
                                                },
                                                title = Format.duration(target),
                                                subtitle = "${Format.signedDuration(actualDelta)} (${frameSign}${deltaFrames}F)",
                                                progress = if (dur > 0L) {
                                                    (target.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                                                } else null,
                                            )
                                            val tNow = SystemClock.uptimeMillis()
                                            if (tNow - lastSeekApplyAt >= 120L) {
                                                exoPlayer.seekTo(target)
                                                positionMs = target
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
                                                subtitle = "x0.50 ◄────────► x6.00",
                                                progress = (newSpeed - GestureMath.MIN_SPEED) /
                                                    (GestureMath.MAX_SPEED - GestureMath.MIN_SPEED),
                                            )
                                            change.consume()
                                        }

                                        ActiveDragMode.NONE -> Unit
                                    }
                                }

                                // 포인터 UP 이후 마무리 처리
                                when (mode) {
                                    ActiveDragMode.SEEK -> {
                                        exoPlayer.seekTo(pendingSeekTarget)
                                        positionMs = pendingSeekTarget
                                        hudState = null
                                    }

                                    ActiveDragMode.BRIGHTNESS,
                                    ActiveDragMode.VOLUME -> {
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
                                            val tapX = pendingTapX
                                            if (tapX in (width * 0.35f)..(width * 0.65f)) {
                                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                            } else {
                                                val isRight = tapX > width * 0.5f
                                                val deltaMs = GestureMath.computeDoubleTapDeltaMs(
                                                    isRightSide = isRight,
                                                    unit = latestSettings.doubleTapUnit,
                                                    seconds = latestSettings.doubleTapSeconds,
                                                    frames = latestSettings.doubleTapFrames,
                                                    fps = effectiveFps,
                                                )
                                                val dur = latestDurationMs
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
                                        } else {
                                            // 싱글 탭 후보 -> 더블터치 윈도우 동안 다음 터치 대기
                                            lastSingleTapJobPending = true
                                            pendingTapTime = SystemClock.uptimeMillis()
                                            pendingTapX = startX
                                            val nextEvent = withTimeoutOrNull(
                                                viewConfig.doubleTapTimeoutMillis
                                            ) {
                                                while (true) {
                                                    val e = awaitPointerEvent()
                                                    if (e.type == PointerEventType.Press) return@withTimeoutOrNull e
                                                }
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
                                                if (secondX in (width * 0.35f)..(width * 0.65f)) {
                                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                                } else {
                                                    val isRight = secondX > width * 0.5f
                                                    val deltaMs = GestureMath.computeDoubleTapDeltaMs(
                                                        isRightSide = isRight,
                                                        unit = latestSettings.doubleTapUnit,
                                                        seconds = latestSettings.doubleTapSeconds,
                                                        frames = latestSettings.doubleTapFrames,
                                                        fps = effectiveFps,
                                                    )
                                                    val dur = latestDurationMs
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
                            vertical = if (controlsVisible && !isInPipMode) 118.dp else 26.dp,
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

        // 4. 더블터치 좌/우 리플 피드백
        val dtf = doubleTapFeedback
        if (dtf != null && !isInPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.38f)
                    .align(if (dtf.isRightSide) Alignment.CenterEnd else Alignment.CenterStart)
                    .background(
                        Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(
                            topStart = if (dtf.isRightSide) 999.dp else 0.dp,
                            bottomStart = if (dtf.isRightSide) 999.dp else 0.dp,
                            topEnd = if (dtf.isRightSide) 0.dp else 999.dp,
                            bottomEnd = if (dtf.isRightSide) 0.dp else 999.dp,
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (dtf.isRightSide) Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = dtf.label,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
            }
        }

        // 5. 중앙 제스처 HUD (밝기 / 볼륨 / 프레임 이동 / 길게터치 배속)
        val hud = hudState
        if (hud != null && !isInPipMode) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Black.copy(alpha = 0.78f),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .width(210.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = hud.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp),
                    )
                    Text(
                        text = hud.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    if (hud.subtitle.isNotBlank()) {
                        Text(
                            text = hud.subtitle,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (hud.progress != null) {
                        LinearProgressIndicator(
                            progress = { hud.progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
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
                    .padding(16.dp)
                    .clickable {
                        isLocked = false
                        controlsVisible = true
                    },
                color = Color.Black.copy(alpha = 0.7f),
                shape = CircleShape,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "잠금 해제", tint = Color.White)
                    Text("잠금 해제", color = Color.White, fontSize = 13.sp)
                }
            }
        }

        // 7. 상/하단 컨트롤 오버레이 (화면 터치 시 표시)
        AnimatedVisibility(
            visible = controlsVisible && !isLocked && !isInPipMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 상단 바
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.82f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onExit) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로",
                            tint = Color.White,
                        )
                    }
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        Text(
                            text = currentVideo.title,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val infoLine = buildString {
                            if (playlist.size > 1) append("${currentIndex + 1}/${playlist.size}")
                            if (settings.aiTranslateEnabled) {
                                if (isNotEmpty()) append(" · ")
                                append("AI 번역(${settings.targetLangCode})")
                            }
                            if (subtitleStatus.isNotBlank()) {
                                if (isNotEmpty()) append(" · ")
                                append(subtitleStatus)
                            }
                        }
                        if (infoLine.isNotBlank()) {
                            Text(
                                text = infoLine,
                                color = Color.White.copy(alpha = 0.78f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // 배속 칩 (누르면 배속 시트, 길게누르기 외 직접 선택/1.0 초기화)
                    Surface(
                        color = if (abs(playbackSpeed - 1.0f) > 0.01f) {
                            Color(0xFF1E88E5)
                        } else {
                            Color.White.copy(alpha = 0.18f)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clickable { showSpeedSheet = true },
                    ) {
                        Text(
                            text = Format.speed(playbackSpeed),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }

                    // AI 번역 버튼
                    IconButton(onClick = { showAiSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "AI 자막 번역",
                            tint = if (settings.aiTranslateEnabled) Color(0xFF64B5F6) else Color.White,
                        )
                    }

                    // 동영상 삭제 버튼 (요구사항 3)
                    IconButton(
                        onClick = {
                            exoPlayer.pause()
                            showDeleteDialog = true
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "동영상 삭제",
                            tint = Color(0xFFFF8A80),
                        )
                    }

                    // 설정 버튼
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "설정",
                            tint = Color.White,
                        )
                    }
                }

                // 하단 바
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                            fontSize = 12.sp,
                        )
                        Slider(
                            value = if (durationMs > 0L) {
                                (shownPos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                            } else 0f,
                            onValueChange = { ratio ->
                                isScrubbingBar = true
                                scrubBarPositionMs = (ratio * durationMs).roundToLong()
                            },
                            onValueChangeFinished = {
                                exoPlayer.seekTo(scrubBarPositionMs)
                                positionMs = scrubBarPositionMs
                                isScrubbingBar = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF42A5F5),
                                activeTrackColor = Color(0xFF42A5F5),
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                            ),
                        )
                        Text(
                            text = Format.duration(durationMs),
                            color = Color.White,
                            fontSize = 12.sp,
                        )
                    }

                    // 핵심 재생 컨트롤: 잠금 / -1F / 이전곡 / 재생일시정지 / 다음곡 / +1F / 화면비율
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { isLocked = true }) {
                            Icon(Icons.Default.LockOpen, contentDescription = "화면 잠금", tint = Color.White)
                        }

                        // 1프레임 뒤로
                        TextButton(
                            onClick = { stepSingleFrame(forward = false) },
                            contentPadding = PaddingValues(horizontal = 6.dp),
                        ) {
                            Text("-1F", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // 이전 동영상 버튼 (요구사항 7)
                        IconButton(
                            onClick = { if (currentIndex > 0) goToVideo(currentIndex - 1) },
                            enabled = currentIndex > 0,
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "이전 동영상",
                                tint = if (currentIndex > 0) Color.White else Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(34.dp),
                            )
                        }

                        // 재생 / 일시정지
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(54.dp)
                                .clickable {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "일시정지" else "재생",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp),
                                )
                            }
                        }

                        // 다음 동영상 버튼 (요구사항 7)
                        IconButton(
                            onClick = { if (currentIndex + 1 < playlist.size) goToVideo(currentIndex + 1) },
                            enabled = currentIndex + 1 < playlist.size,
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "다음 동영상",
                                tint = if (currentIndex + 1 < playlist.size) {
                                    Color.White
                                } else {
                                    Color.White.copy(alpha = 0.35f)
                                },
                                modifier = Modifier.size(34.dp),
                            )
                        }

                        // 1프레임 앞으로
                        TextButton(
                            onClick = { stepSingleFrame(forward = true) },
                            contentPadding = PaddingValues(horizontal = 6.dp),
                        ) {
                            Text("+1F", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // 화면 비율 전환
                        IconButton(
                            onClick = {
                                val next = VideoScaleMode.entries[(scaleMode.ordinal + 1) % VideoScaleMode.entries.size]
                                scaleMode = next
                                Toast.makeText(context, "화면 비율: ${next.label}", Toast.LENGTH_SHORT).show()
                            },
                        ) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "화면 비율", tint = Color.White)
                        }
                    }

                    // 부가 유틸리티 바 (회전, 오디오/자막 트랙, 구간반복, PIP, 취침타이머)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlayerPillButton(
                            icon = Icons.Default.ScreenRotation,
                            label = orientationMode.label,
                            onClick = {
                                val next = ScreenOrientationMode.entries[
                                    (orientationMode.ordinal + 1) % ScreenOrientationMode.entries.size
                                ]
                                orientationMode = next
                            },
                        )
                        PlayerPillButton(
                            icon = Icons.Default.Subtitles,
                            label = if (subtitlesVisible) "자막 ON" else "자막 OFF",
                            highlighted = subtitlesVisible,
                            onClick = { subtitlesVisible = !subtitlesVisible },
                        )
                        PlayerPillButton(
                            icon = Icons.Default.Audiotrack,
                            label = "트랙/자막",
                            onClick = { showTrackSheet = true },
                        )
                        PlayerPillButton(
                            icon = Icons.Default.Repeat,
                            label = when {
                                repeatA == null -> "구간반복"
                                repeatB == null -> "A(${Format.duration(repeatA!!)})→B?"
                                else -> "반복중 ✕"
                            },
                            highlighted = repeatA != null,
                            onClick = {
                                val now = exoPlayer.currentPosition
                                when {
                                    repeatA == null -> {
                                        repeatA = now
                                        Toast.makeText(context, "반복 시작점(A) 설정", Toast.LENGTH_SHORT).show()
                                    }
                                    repeatB == null -> {
                                        if (now > repeatA!! + 300L) {
                                            repeatB = now
                                            exoPlayer.seekTo(repeatA!!)
                                            Toast.makeText(context, "구간 반복 시작", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "A 이후 지점에서 B를 눌러주세요.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    else -> {
                                        repeatA = null
                                        repeatB = null
                                        Toast.makeText(context, "구간 반복 해제", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        )
                        PlayerPillButton(
                            icon = Icons.Default.PictureInPictureAlt,
                            label = "PIP",
                            onClick = { enterPip() },
                        )
                        PlayerPillButton(
                            icon = Icons.Default.Timer,
                            label = sleepRemainingSec?.let { "${it / 60}분 남음" } ?: "취침타이머",
                            highlighted = sleepRemainingSec != null,
                            onClick = { showSleepDialog = true },
                        )
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
                                onDeletedVideo(targetToDelete.id)
                                Toast.makeText(context, "동영상을 삭제했습니다.", Toast.LENGTH_SHORT).show()
                                val updatedList = playlist.filterNot { it.id == targetToDelete.id }
                                if (updatedList.isEmpty()) {
                                    onExit()
                                } else {
                                    playlist = updatedList
                                    currentIndex = currentIndex.coerceIn(0, updatedList.lastIndex)
                                }
                            }.onFailure { err ->
                                Toast.makeText(
                                    context,
                                    err.message ?: "삭제하지 못했습니다.",
                                    Toast.LENGTH_LONG
                                ).show()
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
                    TextButton(onClick = { applySpeed(1.0f) }) { Text("x1.00 초기화") }
                }
                Slider(
                    value = playbackSpeed,
                    onValueChange = { raw ->
                        val snapped = ((raw / GestureMath.SPEED_STEP).roundToInt() * GestureMath.SPEED_STEP)
                        applySpeed(snapped)
                    },
                    valueRange = GestureMath.MIN_SPEED..GestureMath.MAX_SPEED,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f, 6.0f).forEach { preset ->
                        FilterChip(
                            selected = abs(playbackSpeed - preset) < 0.03f,
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
                                Toast.makeText(context, "${min}분 후 정지됩니다.", Toast.LENGTH_SHORT).show()
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

@Composable
private fun PlayerPillButton(
    icon: ImageVector,
    label: String,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        color = if (highlighted) Color(0xFF1E88E5).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.16f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Text(label, color = Color.White, fontSize = 12.sp)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
