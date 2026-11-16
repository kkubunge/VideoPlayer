package com.aiutil.videoplayer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    val currentSession = activeSession
    if (currentSession != null) {
        PlayerScreen(
            initialVideo = currentSession.video,
            initialPlaylist = currentSession.playlist,
            initialSeekPositionMs = currentSession.startPositionMs,
            isInPipMode = isInPipMode,
            onOpenSettings = { showSettingsSheet = true },
            onDeletedVideo = { refreshSignal++ },
            onExit = {
                activeSession = null
                refreshSignal++
            },
        )
    } else {
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
    }

    if (showSettingsSheet) {
        PlayerSettingsSheet(
            settings = settings,
            onUpdate = { transform -> App.settings.update(transform) },
            onDismiss = { showSettingsSheet = false },
        )
    }
}
