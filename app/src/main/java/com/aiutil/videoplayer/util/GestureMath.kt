package com.aiutil.videoplayer.util

import com.aiutil.videoplayer.model.SeekUnit
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * 플레이어 터치/드래그/더블터치 제스처의 순수 수치 계산 유틸리티.
 * Android 프레임워크 의존성이 없어 JVM 단위 테스트에서 직접 검증한다.
 */
object GestureMath {
    const val MIN_BRIGHTNESS = 0.01f
    const val MAX_BRIGHTNESS = 1.0f

    const val MIN_SPEED = 0.5f
    const val MAX_SPEED = 6.0f
    const val SPEED_STEP = 0.5f

    const val MIN_ZOOM = 0.50f
    const val MAX_ZOOM = 4.00f
    const val ZOOM_STEP = 0.05f

    /** 왼쪽 영역 상/하 드래그 밝기 계산 (deltaY < 0 이면 위로 드래그 -> 증가). */
    fun computeBrightness(startBrightness: Float, deltaY: Float, viewHeight: Float): Float {
        if (viewHeight <= 1f) return startBrightness.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
        val sensitivity = 1.15f
        val delta = (-deltaY / viewHeight) * sensitivity
        return (startBrightness + delta).coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
    }

    /** 오른쪽 영역 상/하 드래그 볼륨 비율 계산 (0.0f ~ 1.0f). */
    fun computeVolumeFraction(startFraction: Float, deltaY: Float, viewHeight: Float): Float {
        if (viewHeight <= 1f) return startFraction.coerceIn(0f, 1f)
        val sensitivity = 1.15f
        val delta = (-deltaY / viewHeight) * sensitivity
        return (startFraction + delta).coerceIn(0f, 1f)
    }

    /**
     * 길게 터치 후 좌/우 드래그 시 배속(x0.5 ~ x6.0) 조절 계산.
     * 오른쪽 드래그(+)는 가속, 왼쪽 드래그(-)는 감속하며 0.5x 단위로 스냅된다.
     */
    fun computeLongPressDragSpeed(startSpeed: Float, deltaX: Float, viewWidth: Float): Float {
        val safeWidth = viewWidth.coerceAtLeast(100f)
        val range = MAX_SPEED - MIN_SPEED
        // 화면 폭의 75%를 이동하면 전체 범위(5.5x)를 훑을 수 있도록 설정
        val deltaSpeed = (deltaX / (safeWidth * 0.75f)) * range
        val raw = (startSpeed + deltaSpeed).coerceIn(MIN_SPEED, MAX_SPEED)
        val stepped = (raw / SPEED_STEP).roundToInt() * SPEED_STEP
        return (stepped * 10f).roundToInt() / 10f
    }

    /** 화면 좌/우 드래그 시 설정된 초(seekSeconds) 기준 탐색 이동 시간(ms) 계산. */
    fun computeHorizontalSeekDeltaMs(
        deltaX: Float,
        viewWidth: Float,
        durationMs: Long,
        seekSeconds: Int = 30,
    ): Long {
        if (viewWidth <= 1f) return 0L
        val effectiveWidth = (viewWidth * 0.8f).coerceAtLeast(1f)
        val spanMs = seekSeconds.coerceAtLeast(1) * 1000L
        return ((deltaX / effectiveWidth) * spanMs).roundToLong()
    }

    /** 하단 앞/뒤 이동 버튼 클릭 시 설정된 단위(초 또는 프레임) 기준 이동 시간(ms) 계산. */
    fun computeStepButtonDeltaMs(
        forward: Boolean,
        unit: SeekUnit,
        seconds: Int,
        frames: Int,
        fps: Float,
    ): Long {
        val magnitudeMs = when (unit) {
            SeekUnit.SECONDS -> seconds.coerceAtLeast(1) * 1000L
            SeekUnit.FRAMES -> framesToMs(frames.coerceAtLeast(1).toLong(), fps).coerceAtLeast(16L)
        }
        return if (forward) magnitudeMs else -magnitudeMs
    }

    /** 설정된 단위(초 또는 프레임)에 따른 더블터치 앞/뒤 이동 시간(ms) 계산. */
    fun computeDoubleTapDeltaMs(
        isRightSide: Boolean,
        unit: SeekUnit,
        seconds: Int,
        frames: Int,
        fps: Float,
    ): Long {
        val magnitudeMs = when (unit) {
            SeekUnit.SECONDS -> seconds.coerceAtLeast(1) * 1000L
            SeekUnit.FRAMES -> framesToMs(frames.coerceAtLeast(1).toLong(), fps)
        }
        return if (isRightSide) magnitudeMs else -magnitudeMs
    }

    fun msToFrames(ms: Long, fps: Float): Long {
        val effectiveFps = if (fps in 1f..240f) fps else 30f
        return ((ms / 1000.0) * effectiveFps).roundToLong()
    }

    fun framesToMs(frames: Long, fps: Float): Long {
        val effectiveFps = if (fps in 1f..240f) fps else 30f
        return ((frames / effectiveFps.toDouble()) * 1000.0).roundToLong()
    }

    /**
     * 두 손가락 멀티터치로 화면 크기(50% ~ 400%)를 조절한다.
     * - 두 손가락 사이 거리를 벌리거나 오므리는 동작(핀치 줌)
     * - 두 손가락을 터치한 채 좌/우로 함께 이동하는 동작(우측=확대, 좌측=축소)
     * 두 가지 방식 모두 지원하며 5%(0.05) 단위로 스냅된다.
     */
    fun computeTwoFingerZoom(
        startZoom: Float,
        initialSpan: Float,
        currentSpan: Float,
        initialCenterX: Float,
        currentCenterX: Float,
        viewWidth: Float,
    ): Float {
        val spanDelta = currentSpan - initialSpan
        val centerDeltaX = currentCenterX - initialCenterX
        val raw = if (initialSpan > 20f && kotlin.math.abs(spanDelta) >= kotlin.math.abs(centerDeltaX) * 0.6f) {
            startZoom * (currentSpan / initialSpan).coerceIn(0.25f, 4.0f)
        } else {
            val safeWidth = viewWidth.coerceAtLeast(100f)
            val deltaZoom = (centerDeltaX / (safeWidth * 0.55f)) * (MAX_ZOOM - MIN_ZOOM)
            startZoom + deltaZoom
        }
        val clamped = raw.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val stepped = (clamped / ZOOM_STEP).roundToInt() * ZOOM_STEP
        return (stepped * 100f).roundToInt() / 100f
    }
}
