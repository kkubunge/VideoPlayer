package com.aiutil.videoplayer.util

import java.util.Locale
import kotlin.math.abs

object Format {

    fun duration(ms: Long): String {
        val safe = ms.coerceAtLeast(0L)
        val totalSec = safe / 1000L
        val hours = totalSec / 3600L
        val minutes = (totalSec % 3600L) / 60L
        val seconds = totalSec % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun signedDuration(deltaMs: Long): String {
        val sign = if (deltaMs >= 0L) "+" else "-"
        return "$sign${duration(abs(deltaMs))}"
    }

    fun size(bytes: Long): String {
        if (bytes < 0L) return "0 B"
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }

    fun speed(speed: Float): String {
        val clamped = speed.coerceIn(GestureMath.MIN_SPEED, GestureMath.MAX_SPEED)
        return String.format(Locale.US, "x%.2f", clamped)
    }

    fun resolution(width: Int, height: Int): String {
        if (width <= 0 || height <= 0) return ""
        return "${width}×${height}"
    }
}
