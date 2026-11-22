package com.aiutil.videoplayer.player

/**
 * 화면 잠금·백그라운드 전환 시 재생 유지/일시정지 및 포그라운드 서비스 실행 여부를 결정하는 순수 정책.
 * Android 프레임워크 의존성이 없어 JVM 단위 테스트에서 직접 검증한다.
 */
object BackgroundPlaybackPolicy {

    /** Activity ON_STOP 시 재생을 일시정지해야 하는지 판단한다 (PIP 모드에서는 중단하지 않음). */
    fun shouldPauseOnStop(
        backgroundPlayEnabled: Boolean,
        isInPipMode: Boolean,
        isPlaying: Boolean,
    ): Boolean {
        if (isInPipMode) return false
        if (backgroundPlayEnabled) return false
        return isPlaying
    }

    /** 포그라운드 미디어 재생 서비스를 유지해야 하는지 판단한다. */
    fun shouldRunForegroundService(
        backgroundPlayEnabled: Boolean,
        hasStartedPlayback: Boolean,
        isExiting: Boolean,
    ): Boolean {
        return !isExiting && backgroundPlayEnabled && hasStartedPlayback
    }

    /** 재생 중일 때만 화면 켜짐 유지 플래그를 적용한다. */
    fun shouldKeepScreenOn(isPlaying: Boolean): Boolean = isPlaying
}
