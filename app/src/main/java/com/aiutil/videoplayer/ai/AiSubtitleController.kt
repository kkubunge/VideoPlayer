package com.aiutil.videoplayer.ai

import android.content.Context
import com.aiutil.videoplayer.files.VideoScanner
import com.aiutil.videoplayer.model.PlayerSettings
import com.aiutil.videoplayer.model.SubtitleCue
import com.aiutil.videoplayer.model.VideoItem
import com.aiutil.videoplayer.util.SubtitleParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 동영상 자막 로딩, 실시간 AI 번역, 오디오 기반 자막 생성 및 SRT 내보내기를 조율한다.
 */
class AiSubtitleController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val engine: TranslationEngine = TranslationEngine(),
) {

    companion object {
        private const val AUDIO_WINDOW_MS = 8_000L
    }

    private val _cues = MutableStateFlow<List<SubtitleCue>>(emptyList())
    val cues: StateFlow<List<SubtitleCue>> = _cues.asStateFlow()

    private val _activeCue = MutableStateFlow<SubtitleCue?>(null)
    val activeCue: StateFlow<SubtitleCue?> = _activeCue.asStateFlow()

    private val _statusText = MutableStateFlow("")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _loadedSubtitleName = MutableStateFlow<String?>(null)
    val loadedSubtitleName: StateFlow<String?> = _loadedSubtitleName.asStateFlow()

    private val _syncOffsetMs = MutableStateFlow(0L)
    val syncOffsetMs: StateFlow<Long> = _syncOffsetMs.asStateFlow()

    private val _liveEmbeddedText = MutableStateFlow("")
    private val _liveEmbeddedTranslated = MutableStateFlow("")
    val liveEmbeddedOriginal: StateFlow<String> = _liveEmbeddedText.asStateFlow()
    val liveEmbeddedTranslated: StateFlow<String> = _liveEmbeddedTranslated.asStateFlow()

    private var currentVideo: VideoItem? = null
    private var translateJob: Job? = null
    private var transcribeJob: Job? = null
    private val transcribedWindowStarts = HashSet<Long>()
    private var activeTranscribeWindowStart: Long? = null
    private var hasExternalSubtitleFile = false
    private var latestPositionMs = 0L
    private var lastLiveTranslatedSource = ""

    fun loadForVideo(video: VideoItem, settings: PlayerSettings) {
        currentVideo = video
        translateJob?.cancel()
        transcribeJob?.cancel()
        transcribedWindowStarts.clear()
        activeTranscribeWindowStart = null
        hasExternalSubtitleFile = false
        latestPositionMs = 0L
        _cues.value = emptyList()
        _activeCue.value = null
        _syncOffsetMs.value = 0L
        _liveEmbeddedText.value = ""
        _liveEmbeddedTranslated.value = ""
        lastLiveTranslatedSource = ""

        scope.launch(Dispatchers.IO) {
            val companion = VideoScanner.findCompanionSubtitles(video.path).firstOrNull()
            if (companion != null) {
                loadSubtitleFile(companion, settings)
            } else {
                _loadedSubtitleName.value = null
                if (settings.aiTranslateEnabled && settings.autoTranscribeNoSubtitle) {
                    if (settings.geminiApiKey.isNotBlank()) {
                        _statusText.value = "자막 없음 · Gemini AI 오디오 실시간 번역 대기"
                        withContext(Dispatchers.Main) {
                            maybeTranscribeWindow(
                                video = video,
                                windowStart = 0L,
                                settings = settings,
                                interruptIfDifferent = true,
                            )
                        }
                    } else {
                        _statusText.value = "무자막 음성 번역: 설정(⚙️)에서 Gemini API 키를 입력해 주세요"
                    }
                } else {
                    _statusText.value = ""
                }
            }
        }
    }

    suspend fun loadSubtitleFile(file: File, settings: PlayerSettings) {
        val parsed = withContext(Dispatchers.IO) {
            runCatching {
                SubtitleParser.parseAuto(file.readText(Charsets.UTF_8), file.extension)
            }.getOrDefault(emptyList())
        }
        hasExternalSubtitleFile = parsed.isNotEmpty()
        _cues.value = parsed
        _loadedSubtitleName.value = file.name
        _statusText.value = if (parsed.isEmpty()) {
            "자막 파일을 읽을 수 없습니다."
        } else {
            "자막 로드됨 (${parsed.size}줄)"
        }
        if (parsed.isNotEmpty() && settings.aiTranslateEnabled) {
            pretranslateAround(0L, settings)
        }
    }

    fun adjustSync(deltaMs: Long) {
        _syncOffsetMs.value += deltaMs
    }

    fun resetSync() {
        _syncOffsetMs.value = 0L
    }

    /** ExoPlayer 내장 자막(MKV/MP4 내부 트랙 등)이 들어올 때 호출된다. */
    fun onEmbeddedCueText(rawText: String, settings: PlayerSettings) {
        val clean = rawText.trim()
        if (clean == _liveEmbeddedText.value) return
        _liveEmbeddedText.value = clean
        if (clean.isEmpty()) {
            _liveEmbeddedTranslated.value = ""
            return
        }
        if (!settings.aiTranslateEnabled) {
            _liveEmbeddedTranslated.value = ""
            return
        }
        if (clean == lastLiveTranslatedSource) return
        lastLiveTranslatedSource = clean
        scope.launch {
            val tr = engine.translateText(
                text = clean,
                targetLangCode = settings.targetLangCode,
                mode = settings.translationEngine,
                geminiApiKey = settings.geminiApiKey,
            )
            if (_liveEmbeddedText.value == clean) {
                _liveEmbeddedTranslated.value = tr
            }
        }
    }

    fun onPlaybackPosition(positionMs: Long, isPlaying: Boolean, settings: PlayerSettings) {
        latestPositionMs = positionMs.coerceAtLeast(0L)
        val list = _cues.value
        val active = if (list.isNotEmpty()) {
            SubtitleParser.findActiveCue(list, latestPositionMs, _syncOffsetMs.value)
        } else {
            null
        }
        _activeCue.value = active

        // 외부 자막 파일(.srt/.vtt/.ass)이 있을 때는 기존 자막 줄 배치 번역만 수행
        if (hasExternalSubtitleFile) {
            if (settings.aiTranslateEnabled &&
                active != null &&
                active.translatedText.isNullOrBlank() &&
                translateJob?.isActive != true
            ) {
                pretranslateAround(latestPositionMs, settings)
            }
            return
        }

        // 자막 파일/트랙이 없는 경우 오디오 청크 기반 실시간 AI 음성 인식·번역 수행
        val video = currentVideo ?: return
        if (!settings.aiTranslateEnabled ||
            !settings.autoTranscribeNoSubtitle ||
            _liveEmbeddedText.value.isNotBlank()
        ) {
            return
        }
        if (settings.geminiApiKey.isBlank()) {
            _statusText.value = "무자막 음성 번역: 설정(⚙️)에서 Gemini API 키를 입력해 주세요"
            return
        }

        val currentWindow = (latestPositionMs / AUDIO_WINDOW_MS) * AUDIO_WINDOW_MS
        if (!transcribedWindowStarts.contains(currentWindow)) {
            maybeTranscribeWindow(
                video = video,
                windowStart = currentWindow,
                settings = settings,
                interruptIfDifferent = true,
            )
        } else if (isPlaying) {
            // 현재 구간 전사가 이미 완료되었으면 다음 구간을 선행 프리페치해 대사 공백 방지
            val nextWindow = currentWindow + AUDIO_WINDOW_MS
            if (!transcribedWindowStarts.contains(nextWindow)) {
                maybeTranscribeWindow(
                    video = video,
                    windowStart = nextWindow,
                    settings = settings,
                    interruptIfDifferent = false,
                )
            }
        }
    }

    fun pretranslateAround(positionMs: Long, settings: PlayerSettings) {
        if (!settings.aiTranslateEnabled) return
        val currentList = _cues.value
        if (currentList.isEmpty()) return

        translateJob?.cancel()
        translateJob = scope.launch {
            _statusText.value = "AI 번역 중..."
            val startIdx = currentList.indexOfFirst { it.endMs >= positionMs - 2_000L }
                .coerceAtLeast(0)
            val endIdx = (startIdx + 18).coerceAtMost(currentList.size)
            val targets = (startIdx until endIdx).filter { currentList[it].translatedText.isNullOrBlank() }

            if (targets.isNotEmpty()) {
                val texts = targets.map { currentList[it].originalText }
                val translated = engine.translateBatch(
                    texts = texts,
                    targetLangCode = settings.targetLangCode,
                    mode = settings.translationEngine,
                    geminiApiKey = settings.geminiApiKey,
                )
                val updated = _cues.value.toMutableList()
                targets.forEachIndexed { i, cueIndex ->
                    if (cueIndex < updated.size && i < translated.size) {
                        updated[cueIndex] = updated[cueIndex].copy(translatedText = translated[i])
                    }
                }
                _cues.value = updated
                _activeCue.value = SubtitleParser.findActiveCue(updated, positionMs, _syncOffsetMs.value)
            }
            _statusText.value = "AI 번역 활성 (${settings.targetLangCode})"
        }
    }

    private fun maybeTranscribeWindow(
        video: VideoItem,
        windowStart: Long,
        settings: PlayerSettings,
        interruptIfDifferent: Boolean,
    ) {
        val normalizedStart = (windowStart.coerceAtLeast(0L) / AUDIO_WINDOW_MS) * AUDIO_WINDOW_MS
        if (video.durationMs in 1L..normalizedStart) return
        if (transcribedWindowStarts.contains(normalizedStart)) return

        if (transcribeJob?.isActive == true) {
            if (!interruptIfDifferent || activeTranscribeWindowStart == normalizedStart) return
            transcribeJob?.cancel()
        }

        transcribedWindowStarts.add(normalizedStart)
        activeTranscribeWindowStart = normalizedStart
        transcribeJob = scope.launch {
            try {
                _statusText.value = "AI 오디오 인식·번역 중..."
                val wav = AudioChunkExtractor.extractWavChunk(
                    context = context,
                    uriString = video.uri,
                    filePath = video.path,
                    startMs = normalizedStart,
                    durationMs = AUDIO_WINDOW_MS,
                )
                if (wav == null) {
                    _statusText.value = "오디오 추출 불가"
                    return@launch
                }
                val segments = engine.transcribeAndTranslateAudio(
                    wavBytes = wav,
                    targetLangCode = settings.targetLangCode,
                    geminiApiKey = settings.geminiApiKey,
                )
                if (segments.isNotEmpty()) {
                    val baseIdx = _cues.value.size + 1
                    val newCues = segments.mapIndexed { idx, seg ->
                        SubtitleCue(
                            index = baseIdx + idx,
                            startMs = normalizedStart + seg.startOffsetMs,
                            endMs = normalizedStart + seg.endOffsetMs,
                            originalText = seg.originalText,
                            translatedText = seg.translatedText,
                        )
                    }
                    val merged = (_cues.value + newCues).sortedBy { it.startMs }
                    _cues.value = merged
                    _activeCue.value = SubtitleParser.findActiveCue(merged, latestPositionMs, _syncOffsetMs.value)
                    _statusText.value = "AI 음성 자막 활성 (${merged.size}줄)"
                } else {
                    _statusText.value = "AI 음성 인식 대기 중"
                }

                // 현재 위치 바로 다음 구간(최대 1윈도우 앞)만 연속 선행 전사
                val nextWindow = normalizedStart + AUDIO_WINDOW_MS
                if (currentVideo?.id == video.id &&
                    !hasExternalSubtitleFile &&
                    settings.aiTranslateEnabled &&
                    settings.autoTranscribeNoSubtitle &&
                    settings.geminiApiKey.isNotBlank() &&
                    nextWindow <= latestPositionMs + AUDIO_WINDOW_MS &&
                    !transcribedWindowStarts.contains(nextWindow) &&
                    (video.durationMs <= 0L || nextWindow < video.durationMs)
                ) {
                    activeTranscribeWindowStart = null
                    transcribeJob = null
                    maybeTranscribeWindow(
                        video = video,
                        windowStart = nextWindow,
                        settings = settings,
                        interruptIfDifferent = false,
                    )
                }
            } catch (ce: CancellationException) {
                transcribedWindowStarts.remove(normalizedStart)
                throw ce
            } finally {
                if (activeTranscribeWindowStart == normalizedStart) {
                    activeTranscribeWindowStart = null
                }
            }
        }
    }

    suspend fun exportTranslatedSrt(targetLangCode: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val video = currentVideo ?: error("재생 중인 동영상이 없습니다.")
            val list = _cues.value
            require(list.isNotEmpty()) { "저장할 자막 데이터가 없습니다." }
            val videoFile = File(video.path)
            val dir = videoFile.parentFile ?: context.getExternalFilesDir(null)
                ?: error("저장할 폴더를 찾을 수 없습니다.")
            val baseName = videoFile.nameWithoutExtension.ifBlank { "subtitle" }
            val outFile = File(dir, "$baseName.$targetLangCode.srt")
            outFile.writeText(SubtitleParser.toSrt(list, useTranslated = true), Charsets.UTF_8)
            outFile
        }
    }

    fun release() {
        translateJob?.cancel()
        transcribeJob?.cancel()
        engine.close()
    }
}
