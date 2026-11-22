package com.aiutil.videoplayer.player

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.aiutil.videoplayer.ui.MainActivity

/**
 * 화면이 꺼지거나 앱이 백그라운드로 이동해도 오디오 재생과 잠금화면/알림창 컨트롤을 유지하는 포그라운드 서비스.
 */
@OptIn(UnstableApi::class)
class BackgroundPlaybackService : Service() {

    private var mediaSession: MediaSession? = null
    private var attachedPlayer: ExoPlayer? = null
    private var isForegroundStarted: Boolean = false

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            attachedPlayer?.let { updateNotification(it) }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            attachedPlayer?.let { updateNotification(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                if (!isForegroundStarted) {
                    startForegroundCompat(buildMinimalNotification())
                }
                BackgroundPlaybackBridge.onStopBackground?.invoke()
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_PREV -> BackgroundPlaybackBridge.onPrevious?.invoke()
            ACTION_TOGGLE_PLAY -> BackgroundPlaybackBridge.onTogglePlay?.invoke()
            ACTION_NEXT -> BackgroundPlaybackBridge.onNext?.invoke()
        }

        val player = BackgroundPlaybackBridge.player
        if (player == null) {
            if (!isForegroundStarted) {
                startForegroundCompat(buildMinimalNotification())
            }
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }

        ensureSessionFor(player)
        val notification = buildNotification(player)
        if (!isForegroundStarted) {
            startForegroundCompat(notification)
        } else {
            notifyNotification(notification)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        detachPlayerListener()
        runCatching { mediaSession?.release() }
        mediaSession = null
        isForegroundStarted = false
        super.onDestroy()
    }

    private fun ensureSessionFor(player: ExoPlayer) {
        if (attachedPlayer === player && mediaSession != null) return
        detachPlayerListener()
        runCatching { mediaSession?.release() }
        mediaSession = null

        attachedPlayer = player
        player.addListener(playerListener)
        mediaSession = runCatching {
            MediaSession.Builder(this, player)
                .setId("kkubunge_bg_${SystemClock.uptimeMillis()}")
                .setSessionActivity(createOpenAppPendingIntent())
                .build()
        }.getOrNull()
    }

    private fun detachPlayerListener() {
        attachedPlayer?.let { runCatching { it.removeListener(playerListener) } }
        attachedPlayer = null
    }

    private fun updateNotification(player: ExoPlayer) {
        if (!isForegroundStarted) return
        notifyNotification(buildNotification(player))
    }

    private fun notifyNotification(notification: Notification) {
        val nm = getSystemService(NotificationManager::class.java)
        runCatching { nm?.notify(NOTIFICATION_ID, notification) }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "백그라운드 재생",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "화면 잠금 중 동영상 오디오 재생 및 잠금화면 제어 알림"
                    setShowBadge(false)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun createOpenAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, BackgroundPlaybackService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildMinimalNotification(): Notification {
        ensureNotificationChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("백그라운드 재생")
            .setContentText("재생 준비 중...")
            .setContentIntent(createOpenAppPendingIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun buildNotification(player: ExoPlayer): Notification {
        ensureNotificationChannel()
        val meta = BackgroundPlaybackBridge.metadata
        val title = meta.title.ifBlank { "동영상 재생 중" }
        val subtitle = meta.subtitle.ifBlank { "백그라운드 재생 중" }
        val playing = player.isPlaying

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setContentIntent(createOpenAppPendingIntent())
            .setOngoing(playing)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_media_previous,
                "이전",
                createActionPendingIntent(ACTION_PREV, 1),
            )
            .addAction(
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (playing) "일시정지" else "재생",
                createActionPendingIntent(ACTION_TOGGLE_PLAY, 2),
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "다음",
                createActionPendingIntent(ACTION_NEXT, 3),
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "중지",
                createActionPendingIntent(ACTION_STOP, 4),
            )

        mediaSession?.let { session ->
            builder.setStyle(
                MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(0, 1, 2)
            )
        }
        return builder.build()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isForegroundStarted = true
    }

    private fun stopForegroundCompat() {
        if (!isForegroundStarted) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        isForegroundStarted = false
    }

    companion object {
        private const val CHANNEL_ID = "background_playback_channel"
        private const val NOTIFICATION_ID = 4102

        const val ACTION_START_OR_UPDATE = "com.aiutil.videoplayer.action.BG_START_OR_UPDATE"
        const val ACTION_PREV = "com.aiutil.videoplayer.action.BG_PREV"
        const val ACTION_TOGGLE_PLAY = "com.aiutil.videoplayer.action.BG_TOGGLE_PLAY"
        const val ACTION_NEXT = "com.aiutil.videoplayer.action.BG_NEXT"
        const val ACTION_STOP = "com.aiutil.videoplayer.action.BG_STOP"

        fun startOrUpdate(context: Context) {
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, BackgroundPlaybackService::class.java)
                .setAction(ACTION_START_OR_UPDATE)
            runCatching { ContextCompat.startForegroundService(appCtx, intent) }
        }

        fun stop(context: Context) {
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, BackgroundPlaybackService::class.java)
            runCatching { appCtx.stopService(intent) }
        }

        fun requestNotificationPermissionIfNeeded(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val activity = context.findActivityOrNull() ?: return
            runCatching {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001,
                )
            }
        }

        private tailrec fun Context.findActivityOrNull(): Activity? = when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivityOrNull()
            else -> null
        }
    }
}
