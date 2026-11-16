package com.aiutil.videoplayer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aiutil.videoplayer.App
import com.aiutil.videoplayer.model.VideoItem
import com.aiutil.videoplayer.ui.explorer.ExplorerScreen
import com.aiutil.videoplayer.ui.player.PlayerScreen
import com.aiutil.videoplayer.ui.settings.PlayerSettingsSheet

data class ActivePlaybackSession(
    val video: VideoItem,
    val playlist: List<VideoItem>,
    val startPositionMs: Long? = null,
)

@Composable
fun AppRoot(
    hasReadPermission: Boolean,
    hasAllFilesAccess: Boolean,
    externalSession: ActivePlaybackSession?,
    isInPipMode: Boolean,
    onConsumeExternalSession: () -> Unit,
    onRequestReadPermission: () -> Unit,
    onRequestAllFilesAccess: () -> Unit,
) {
    val settings by App.settings.settings.collectAsState()
    var activeSession by remember { mutableStateOf<ActivePlaybackSession?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var refreshSignal by remember { mutableIntStateOf(0) }

    if (externalSession != null) {
        activeSession = externalSession
        onConsumeExternalSession()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 탐색기를 항상 컴포지션에 유지해 동영상 종료 시 이전 폴더와 스크롤 위치를 그대로 복원한다.
        ExplorerScreen(
            hasReadPermission = hasReadPermission,
            hasAllFilesAccess = hasAllFilesAccess,
            refreshSignal = refreshSignal,
            onRequestReadPermission = onRequestReadPermission,
            onRequestAllFilesAccess = onRequestAllFilesAccess,
            onPlayVideo = { video, playlist, startPosMs ->
                activeSession = ActivePlaybackSession(
                    video = video,
                    playlist = playlist,
                    startPositionMs = startPosMs,
                )
            },
            onOpenSettings = { showSettingsSheet = true },
        )

        val currentSession = activeSession
        if (currentSession != null) {
            PlayerScreen(
                initialVideo = currentSession.video,
                initialPlaylist = currentSession.playlist,
                initialSeekPositionMs = currentSession.startPositionMs,
                isInPipMode = isInPipMode,
                onOpenSettings = { showSettingsSheet = true },
                onDeletedVideo = { refreshSignal++ },
                onExit = { activeSession = null },
            )
        }
    }

    if (showSettingsSheet) {
        PlayerSettingsSheet(
            settings = settings,
            onUpdate = { transform -> App.settings.update(transform) },
            onDismiss = { showSettingsSheet = false },
        )
    }
}
