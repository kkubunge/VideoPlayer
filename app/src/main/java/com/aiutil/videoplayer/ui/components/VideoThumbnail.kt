package com.aiutil.videoplayer.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 동영상 썸네일을 비동기 로드·캐시하여 보여준다. */
@Composable
fun VideoThumbnail(
    uri: String?,
    path: String? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cacheKey = path?.takeIf { it.isNotBlank() } ?: uri
    var image by remember(cacheKey) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(cacheKey) {
        if (cacheKey.isNullOrBlank()) return@LaunchedEffect
        image = withContext(Dispatchers.IO) {
            ThumbnailCache.load(context, uri.orEmpty(), path.orEmpty())?.asImageBitmap()
        }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private object ThumbnailCache {

    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt().coerceAtLeast(2048)
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun load(context: Context, uriString: String, path: String): Bitmap? {
        val key = path.ifBlank { uriString }
        cache.get(key)?.let { return it }

        if (uriString.isNotBlank()) {
            val parsed = Uri.parse(uriString)
            systemThumbnail(context, parsed)?.let { bitmap ->
                cache.put(key, bitmap)
                return bitmap
            }
        }
        return extractFrame(context, uriString, path)?.also { cache.put(key, it) }
    }

    private fun systemThumbnail(context: Context, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        if (uri.scheme != "content") return null
        return runCatching {
            context.contentResolver.loadThumbnail(uri, Size(480, 360), null)
        }.getOrNull()
    }

    private fun extractFrame(context: Context, uriString: String, path: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            if (path.isNotBlank() && File(path).canRead()) {
                retriever.setDataSource(path)
            } else if (uriString.isNotBlank()) {
                retriever.setDataSource(context, Uri.parse(uriString))
            } else {
                return null
            }
            retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
