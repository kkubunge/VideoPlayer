package com.aiutil.videoplayer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val MxBlue = Color(0xFF1565C0)
private val MxBlueLight = Color(0xFF82B1FF)

private val LightColors = lightColorScheme(
    primary = MxBlue,
    secondary = Color(0xFF3E5F8A),
    tertiary = Color(0xFF00897B),
)

private val DarkColors = darkColorScheme(
    primary = MxBlueLight,
    secondary = Color(0xFFA9C7EB),
    tertiary = Color(0xFF4DB6AC),
)

@Composable
fun VideoPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = colors, content = content)
}
