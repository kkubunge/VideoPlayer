package com.aiutil.videoplayer.player

import androidx.media3.exoplayer.ExoPlayer

/**
 * PlayerScreen과 BackgroundPlaybackService 사이에서 현재 ExoPlayer, 메타데이터,
 * 그리고 잠금화면/알림창 액션 콜백을 공유하는 경량 브리지.
 */
object BackgroundPlaybackBridge {

    data class PlaybackMetadata(
        val title: String,
        val subtitle: String,
        val hasPrevious: Boolean,
        val hasNext: Boolean,
    )

    @Volatile
    var player: ExoPlayer? = null
        private set

    @Volatile
    var metadata: PlaybackMetadata = PlaybackMetadata("", "", hasPrevious = false, hasNext = false)
        private set

    @Volatile
    var onPrevious: (() -> Unit)? = null
        private set

    @Volatile
    var onNext: (() -> Unit)? = null
        private set

    @Volatile
    var onTogglePlay: (() -> Unit)? = null
        private set

    @Volatile
    var onStopBackground: (() -> Unit)? = null
        private set

    fun attach(
        player: ExoPlayer,
        metadata: PlaybackMetadata,
        onPrevious: () -> Unit,
        onNext: () -> Unit,
        onTogglePlay: () -> Unit,
        onStopBackground: () -> Unit,
    ) {
        this.player = player
        this.metadata = metadata
        this.onPrevious = onPrevious
        this.onNext = onNext
        this.onTogglePlay = onTogglePlay
        this.onStopBackground = onStopBackground
    }

    fun updateMetadata(metadata: PlaybackMetadata) {
        this.metadata = metadata
    }

    fun clear(expectedPlayer: ExoPlayer? = null) {
        if (expectedPlayer != null && this.player !== expectedPlayer) return
        this.player = null
        this.metadata = PlaybackMetadata("", "", hasPrevious = false, hasNext = false)
        this.onPrevious = null
        this.onNext = null
        this.onTogglePlay = null
        this.onStopBackground = null
    }
}
