package com.aiutil.videoplayer.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundPlaybackPolicyTest {

    @Test
    fun `shouldPauseOnStop pauses playing video when background play is disabled and not in PIP`() {
        assertTrue(
            BackgroundPlaybackPolicy.shouldPauseOnStop(
                backgroundPlayEnabled = false,
                isInPipMode = false,
                isPlaying = true,
            )
        )
        assertFalse(
            BackgroundPlaybackPolicy.shouldPauseOnStop(
                backgroundPlayEnabled = false,
                isInPipMode = false,
                isPlaying = false,
            )
        )
        assertFalse(
            BackgroundPlaybackPolicy.shouldPauseOnStop(
                backgroundPlayEnabled = true,
                isInPipMode = false,
                isPlaying = true,
            )
        )
        assertFalse(
            BackgroundPlaybackPolicy.shouldPauseOnStop(
                backgroundPlayEnabled = false,
                isInPipMode = true,
                isPlaying = true,
            )
        )
    }

    @Test
    fun `shouldRunForegroundService runs only when enabled started and not exiting`() {
        assertTrue(
            BackgroundPlaybackPolicy.shouldRunForegroundService(
                backgroundPlayEnabled = true,
                hasStartedPlayback = true,
                isExiting = false,
            )
        )
        assertFalse(
            BackgroundPlaybackPolicy.shouldRunForegroundService(
                backgroundPlayEnabled = false,
                hasStartedPlayback = true,
                isExiting = false,
            )
        )
        assertFalse(
            BackgroundPlaybackPolicy.shouldRunForegroundService(
                backgroundPlayEnabled = true,
                hasStartedPlayback = false,
                isExiting = false,
            )
        )
        assertFalse(
            BackgroundPlaybackPolicy.shouldRunForegroundService(
                backgroundPlayEnabled = true,
                hasStartedPlayback = true,
                isExiting = true,
            )
        )
    }

    @Test
    fun `shouldKeepScreenOn mirrors playback state`() {
        assertTrue(BackgroundPlaybackPolicy.shouldKeepScreenOn(isPlaying = true))
        assertFalse(BackgroundPlaybackPolicy.shouldKeepScreenOn(isPlaying = false))
    }
}
