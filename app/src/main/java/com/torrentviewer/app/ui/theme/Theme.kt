package com.torrentviewer.app.ui.theme

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

private val BrandBlue = Color(0xFF3B6FE0)
private val BrandBlueDark = Color(0xFF9DBBFF)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE4FF),
    onPrimaryContainer = Color(0xFF001848),
    background = Color(0xFFF5F6FA),
    onBackground = Color(0xFF17191F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17191F),
    surfaceVariant = Color(0xFFE9EBF2),
    onSurfaceVariant = Color(0xFF454852)
)

private val DarkColors = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = Color(0xFF002E6E),
    primaryContainer = Color(0xFF1E4BA8),
    onPrimaryContainer = Color(0xFFDCE4FF),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE4E6EE),
    surface = Color(0xFF1A1D24),
    onSurface = Color(0xFFE4E6EE),
    surfaceVariant = Color(0xFF272B34),
    onSurfaceVariant = Color(0xFFC3C6D0)
)

@Composable
fun TorrentViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
