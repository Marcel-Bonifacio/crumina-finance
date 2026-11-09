package id.tirtawijata.crumina.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.tirtawijata.crumina.R

/** Design tokens copied from the web app's CSS (:root and [data-theme="dark"] in app/index.html). */
@Immutable
data class CrColors(
    val bg: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val btn: Color,
    val dark: Boolean,
    /** High contrast ([data-theme="contrast"] on the web): black lines and darker accents. */
    val contrast: Boolean = false,
    val teal: Color = Color(0xFF16C7A8),
    val green: Color = Color(0xFF13B97A),
    val red: Color = Color(0xFFF0476A),
    val gold: Color = Color(0xFFF6B73C)
)

val LightCr = CrColors(
    bg = Color(0xFFEEF1F8), card = Color.White, ink = Color(0xFF0A1B3D), muted = Color(0xFF6B7A99),
    line = Color(0xFFE7EBF3), btn = Color(0xFF0A1B3D), dark = false
)
val ContrastCr = CrColors(
    bg = Color.White, card = Color.White, ink = Color.Black, muted = Color(0xFF33414F),
    line = Color.Black, btn = Color.Black, dark = false, contrast = true,
    teal = Color(0xFF005EA6), green = Color(0xFF006B3C), red = Color(0xFFC00000), gold = Color(0xFF7A4F00)
)
val DarkCr = CrColors(
    bg = Color(0xFF0A0F1C), card = Color(0xFF141E33), ink = Color(0xFFE8EEFA), muted = Color(0xFF9AA9C6),
    line = Color(0xFF26324C), btn = Color(0xFF2D5BD0), dark = true
)

/** Brand navy used by the hero card, avatar and logo in both themes. */
val Navy = Color(0xFF0A1B3D)
val Navy2 = Color(0xFF1B3D7A)

val LocalCr = staticCompositionLocalOf { LightCr }

/** Plus Jakarta Sans, the web app's font, bundled as a variable font (res/font). */
@OptIn(ExperimentalTextApi::class)
private fun jakarta(w: Int) = Font(
    R.font.plus_jakarta_sans,
    weight = FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w))
)

val Jakarta = FontFamily(jakarta(400), jakarta(500), jakarta(600), jakarta(700), jakarta(800))

private fun Typography.withFont(f: FontFamily): Typography {
    fun TextStyle.j() = copy(fontFamily = f)
    return copy(
        displayLarge = displayLarge.j(), displayMedium = displayMedium.j(), displaySmall = displaySmall.j(),
        headlineLarge = headlineLarge.j(), headlineMedium = headlineMedium.j(), headlineSmall = headlineSmall.j(),
        titleLarge = titleLarge.j(), titleMedium = titleMedium.j(), titleSmall = titleSmall.j(),
        bodyLarge = bodyLarge.j(), bodyMedium = bodyMedium.j(), bodySmall = bodySmall.j(),
        labelLarge = labelLarge.j(), labelMedium = labelMedium.j(), labelSmall = labelSmall.j()
    )
}

@Composable
fun CruminaTheme(content: @Composable () -> Unit) {
    // Light unless the user picks Dark or High contrast in Settings (not the system theme).
    val cr = when (id.tirtawijata.crumina.data.Repo.theme) { "dark" -> DarkCr; "contrast" -> ContrastCr; else -> LightCr }
    // Material components (dialogs, text fields, chips, switches) pick these up.
    val scheme = if (cr.dark) darkColorScheme(
        primary = cr.btn, onPrimary = Color.White, secondary = cr.teal, background = cr.bg, onBackground = cr.ink,
        surface = cr.card, onSurface = cr.ink, surfaceVariant = cr.bg, onSurfaceVariant = cr.muted,
        surfaceContainerHigh = cr.card, surfaceContainerHighest = cr.bg, outline = cr.line, outlineVariant = cr.line,
        error = cr.red
    ) else lightColorScheme(
        primary = cr.btn, onPrimary = Color.White, secondary = cr.teal, background = cr.bg, onBackground = cr.ink,
        surface = cr.card, onSurface = cr.ink, surfaceVariant = cr.bg, onSurfaceVariant = cr.muted,
        surfaceContainerHigh = cr.card, surfaceContainerHighest = cr.bg, outline = cr.line, outlineVariant = cr.line,
        error = cr.red
    )
    CompositionLocalProvider(LocalCr provides cr) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography().withFont(Jakarta),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp),
                extraLarge = RoundedCornerShape(24.dp)
            ),
            content = content
        )
    }
}
