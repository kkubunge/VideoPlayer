package com.aiutil.videoplayer.util

import com.aiutil.videoplayer.model.SeekUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureMathTest {

    @Test
    fun `vertical drag adjusts brightness and clamps within 0_01 to 1_0`() {
        val increased = GestureMath.computeBrightness(
            startBrightness = 0.5f,
            deltaY = -200f,
            viewHeight = 1000f,
        )
        assertTrue(increased > 0.5f)
        assertTrue(increased <= 1.0f)

        val maxed = GestureMath.computeBrightness(
            startBrightness = 0.8f,
            deltaY = -1000f,
            viewHeight = 1000f,
        )
        assertEquals(GestureMath.MAX_BRIGHTNESS, maxed, 0.0001f)

        val minned = GestureMath.computeBrightness(
            startBrightness = 0.2f,
            deltaY = 1000f,
            viewHeight = 1000f,
        )
        assertEquals(GestureMath.MIN_BRIGHTNESS, minned, 0.0001f)
    }

    @Test
    fun `vertical drag adjusts volume fraction within 0_0 to 1_0`() {
        val up = GestureMath.computeVolumeFraction(0.3f, deltaY = -250f, viewHeight = 1000f)
        assertTrue(up > 0.3f)

        val down = GestureMath.computeVolumeFraction(0.3f, deltaY = 800f, viewHeight = 1000f)
        assertEquals(0f, down, 0.0001f)
    }

    @Test
    fun `long press horizontal drag tunes playback speed smoothly between 0_50 and 6_00`() {
        val faster = GestureMath.computeLongPressDragSpeed(
            startSpeed = 1.0f,
            deltaX = 300f,
            viewWidth = 1000f,
        )
        assertTrue(faster > 1.0f && faster <= 6.0f)

        val cappedMax = GestureMath.computeLongPressDragSpeed(
            startSpeed = 2.0f,
            deltaX = 2000f,
            viewWidth = 1000f,
        )
        assertEquals(6.0f, cappedMax, 0.001f)

        val cappedMin = GestureMath.computeLongPressDragSpeed(
            startSpeed = 1.0f,
            deltaX = -2000f,
            viewWidth = 1000f,
        )
        assertEquals(0.5f, cappedMin, 0.001f)

        // 0.5 단위로 스냅되어야 한다.
        val snapped = GestureMath.computeLongPressDragSpeed(
            startSpeed = 1.0f,
            deltaX = 80f,
            viewWidth = 1000f,
        )
        val remainder = kotlin.math.round(snapped * 10f).toInt() % 5
        assertEquals(0, remainder)
        assertEquals(1.5f, snapped, 0.001f)
    }

    @Test
    fun `double tap delta respects seconds and frames unit settings`() {
        assertEquals(
            15_000L,
            GestureMath.computeDoubleTapDeltaMs(
                isRightSide = true,
                unit = SeekUnit.SECONDS,
                seconds = 15,
                frames = 10,
                fps = 30f,
            ),
        )
        assertEquals(
            -15_000L,
            GestureMath.computeDoubleTapDeltaMs(
                isRightSide = false,
                unit = SeekUnit.SECONDS,
                seconds = 15,
                frames = 10,
                fps = 30f,
            ),
        )

        // 30fps 에서 15프레임 = 500ms
        assertEquals(
            500L,
            GestureMath.computeDoubleTapDeltaMs(
                isRightSide = true,
                unit = SeekUnit.FRAMES,
                seconds = 10,
                frames = 15,
                fps = 30f,
            ),
        )
        assertEquals(
            -500L,
            GestureMath.computeDoubleTapDeltaMs(
                isRightSide = false,
                unit = SeekUnit.FRAMES,
                seconds = 10,
                frames = 15,
                fps = 30f,
            ),
        )
    }

    @Test
    fun `two finger pinch and horizontal slide scale video within 50 to 400 percent`() {
        // 핀치 아웃(간격 넓히기): 200 -> 300 이면 1.5배 (150%)
        val pinchOut = GestureMath.computeTwoFingerZoom(
            startZoom = 1.0f,
            initialSpan = 200f,
            currentSpan = 300f,
            initialCenterX = 500f,
            currentCenterX = 500f,
            viewWidth = 1000f,
        )
        assertEquals(1.50f, pinchOut, 0.001f)

        // 두 손가락 함께 오른쪽으로 이동 (간격 유지): 확대
        val slideRight = GestureMath.computeTwoFingerZoom(
            startZoom = 1.0f,
            initialSpan = 200f,
            currentSpan = 200f,
            initialCenterX = 400f,
            currentCenterX = 700f,
            viewWidth = 1000f,
        )
        assertTrue(slideRight > 1.0f && slideRight <= GestureMath.MAX_ZOOM)

        // 두 손가락 함께 왼쪽으로 이동 (간격 유지): 축소 (최소 50%)
        val slideLeft = GestureMath.computeTwoFingerZoom(
            startZoom = 1.0f,
            initialSpan = 200f,
            currentSpan = 200f,
            initialCenterX = 700f,
            currentCenterX = 100f,
            viewWidth = 1000f,
        )
        assertEquals(GestureMath.MIN_ZOOM, slideLeft, 0.001f)
    }

    @Test
    fun `horizontal seek delta uses seconds only independently of button unit`() {
        // viewWidth = 1000 -> effectiveWidth = 800
        // 초 단위(30초 설정): 800px 드래그 시 +30,000ms, -400px 드래그 시 -15,000ms
        val secForward = GestureMath.computeHorizontalSeekDeltaMs(
            deltaX = 800f,
            viewWidth = 1000f,
            durationMs = 600_000L,
            seekSeconds = 30,
        )
        assertEquals(30_000L, secForward)

        val secBackward = GestureMath.computeHorizontalSeekDeltaMs(
            deltaX = -400f,
            viewWidth = 1000f,
            durationMs = 600_000L,
            seekSeconds = 30,
        )
        assertEquals(-15_000L, secBackward)

        val customSec = GestureMath.computeHorizontalSeekDeltaMs(
            deltaX = 400f,
            viewWidth = 1000f,
            durationMs = 600_000L,
            seekSeconds = 60,
        )
        assertEquals(30_000L, customSec)
    }

    @Test
    fun `step button delta respects seconds and frames unit settings independently`() {
        assertEquals(
            5_000L,
            GestureMath.computeStepButtonDeltaMs(
                forward = true,
                unit = SeekUnit.SECONDS,
                seconds = 5,
                frames = 10,
                fps = 30f,
            ),
        )
        assertEquals(
            -5_000L,
            GestureMath.computeStepButtonDeltaMs(
                forward = false,
                unit = SeekUnit.SECONDS,
                seconds = 5,
                frames = 10,
                fps = 30f,
            ),
        )
        // 30fps 에서 15프레임 = 500ms, 1프레임 = 33ms
        assertEquals(
            500L,
            GestureMath.computeStepButtonDeltaMs(
                forward = true,
                unit = SeekUnit.FRAMES,
                seconds = 5,
                frames = 15,
                fps = 30f,
            ),
        )
        assertEquals(
            -33L,
            GestureMath.computeStepButtonDeltaMs(
                forward = false,
                unit = SeekUnit.FRAMES,
                seconds = 5,
                frames = 1,
                fps = 30f,
            ),
        )
    }
}
