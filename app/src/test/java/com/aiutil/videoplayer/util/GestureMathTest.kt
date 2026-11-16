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
}
