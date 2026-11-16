package com.aiutil.videoplayer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.aiutil.videoplayer.files.VideoScanner
import com.aiutil.videoplayer.model.VideoItem
import com.aiutil.videoplayer.ui.theme.VideoPlayerTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private var hasReadPermission by mutableStateOf(false)
    private var hasAllFilesAccess by mutableStateOf(false)
    private var externalSession by mutableStateOf<ActivePlaybackSession?>(null)
    private var isInPipMode by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshPermissionFlags()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshPermissionFlags()
        if (!hasReadPermission) {
            requestReadPermissions()
        }
        handleIncomingIntent(intent)

        setContent {
            VideoPlayerTheme {
                AppRoot(
                    hasReadPermission = hasReadPermission,
                    hasAllFilesAccess = hasAllFilesAccess,
                    externalSession = externalSession,
                    isInPipMode = isInPipMode,
                    onConsumeExternalSession = { externalSession = null },
                    onRequestReadPermission = { requestReadPermissions() },
                    onRequestAllFilesAccess = { requestAllFilesAccess() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionFlags()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    private fun refreshPermissionFlags() {
        hasAllFilesAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
        val readPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        hasReadPermission = hasAllFilesAccess ||
            ContextCompat.checkSelfPermission(this, readPerm) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestReadPermissions() {
        val perms = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }.toTypedArray()
        permissionLauncher.launch(perms)
    }

    private fun requestAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            runCatching { startActivity(intent) }.onFailure {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        val path = if (uri.scheme == "file") uri.path.orEmpty() else ""
        val file = path.takeIf { it.isNotBlank() }?.let { File(it) }
        val displayName = file?.name ?: queryDisplayName(uri) ?: uri.lastPathSegment ?: "동영상"
        val item = if (file != null && file.exists()) {
            VideoScanner.videoItemFromFile(file, extractMetadata = true)
        } else {
            VideoItem(
                id = uri.toString(),
                title = displayName,
                uri = uri.toString(),
                path = path,
                folderPath = file?.parent ?: "",
                folderName = file?.parentFile?.name ?: "외부 열기",
                sizeBytes = file?.length() ?: 0L,
                durationMs = 0L,
                dateModifiedSec = System.currentTimeMillis() / 1000L,
            )
        }
        externalSession = ActivePlaybackSession(video = item, playlist = listOf(item))
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        }.getOrNull()
    }
}
