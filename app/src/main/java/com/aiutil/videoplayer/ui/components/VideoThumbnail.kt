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
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

/** 동영상 썸네일을 비동기 로드·캐시하여 보여준다. 스크롤 시 렉을 막기 위해 디바운스와 동시성 제한을 사용한다. */
@Composable
fun VideoThumbnail(
    uri: String?,
    path: String? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cacheKey = path?.takeIf { it.isNotBlank() } ?: uri
    var image by remember(cacheKey) {
        mutableStateOf(
            if (cacheKey.isNullOrBlank()) null else ThumbnailCache.getCached(uri.orEmpty(), path.orEmpty())
        )
    }

    LaunchedEffect(cacheKey) {
        if (cacheKey.isNullOrBlank() || image != null) return@LaunchedEffect
        // 빠르게 스크롤하며 지나가는 항목은 디코딩하지 않고 건너뛴다.
        delay(75L)
        image = withContext(Dispatchers.IO) {
            ThumbnailCache.load(context, uri.orEmpty(), path.orEmpty())
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

    private val decodeSemaphore = Semaphore(2)

    private val cache = object : LruCache<String, ImageBitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt().coerceAtLeast(4096)
    ) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            (value.width * value.height * 4) / 1024
    }

    fun getCached(uriString: String, path: String): ImageBitmap? {
        val key = path.ifBlank { uriString }
        if (key.isBlank()) return null
        return cache.get(key)
    }

    suspend fun load(context: Context, uriString: String, path: String): ImageBitmap? {
        val key = path.ifBlank { uriString }
        if (key.isBlank()) return null
        cache.get(key)?.let { return it }

        return decodeSemaphore.withPermit {
            cache.get(key)?.let { return@withPermit it }
            val raw = if (uriString.isNotBlank()) {
                systemThumbnail(context, Uri.parse(uriString))
            } else {
                null
            } ?: extractFrame(context, uriString, path)

            val scaled = raw?.let { downscaleIfNeeded(it, 240, 135) }
            scaled?.asImageBitmap()?.also { cache.put(key, it) }
        }
    }

    private fun systemThumbnail(context: Context, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        if (uri.scheme != "content") return null
        return runCatching {
            context.contentResolver.loadThumbnail(uri, Size(256, 160), null)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    1_000_000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    240,
                    135,
                ) ?: retriever.frameAtTime
            } else {
                retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
            }
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun downscaleIfNeeded(bmp: Bitmap, maxW: Int, maxH: Int): Bitmap {
        if (bmp.width <= maxW && bmp.height <= maxH) return bmp
        val ratio = minOf(maxW.toFloat() / bmp.width, maxH.toFloat() / bmp.height)
        val targetW = (bmp.width * ratio).toInt().coerceAtLeast(1)
        val targetH = (bmp.height * ratio).toInt().coerceAtLeast(1)
        val resized = Bitmap.createScaledBitmap(bmp, targetW, targetH, true)
        if (resized !== bmp) bmp.recycle()
        return resized
    }
}
