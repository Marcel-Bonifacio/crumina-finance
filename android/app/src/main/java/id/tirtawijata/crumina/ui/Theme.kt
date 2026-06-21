package id.tirtawijata.crumina.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF0A1B3D)
private val Teal = Color(0xFF2FE0C2)

private val LightColors = lightColorScheme(primary = Navy, secondary = Teal)
private val DarkColors = darkColorScheme(primary = Teal, secondary = Navy)

@Composable
fun CruminaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
