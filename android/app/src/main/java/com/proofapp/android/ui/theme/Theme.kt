package com.proofapp.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.proofapp.android.domain.model.Category

// A palette taken from the bakery: flour, crust, butter, sage and dark rye.
private val Crust = Color(0xFF9A4A1E)
private val CrustLight = Color(0xFFFFB68F)
private val Flour = Color(0xFFFFF8F2)
private val Rye = Color(0xFF1A120E)

private val LightColors = lightColorScheme(
    primary = Crust,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC9),
    onPrimaryContainer = Color(0xFF3A1400),
    secondary = Color(0xFF7A5900),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE08A),
    onSecondaryContainer = Color(0xFF261A00),
    tertiary = Color(0xFF4E6A4B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFEAC9),
    onTertiaryContainer = Color(0xFF0B200C),
    background = Flour,
    onBackground = Color(0xFF221A15),
    surface = Flour,
    onSurface = Color(0xFF221A15),
    surfaceVariant = Color(0xFFF4DED4),
    onSurfaceVariant = Color(0xFF52443C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF1E8),
    surfaceContainer = Color(0xFFFBEBE1),
    surfaceContainerHigh = Color(0xFFF5E5DB),
    surfaceContainerHighest = Color(0xFFEFDFD5),
    outline = Color(0xFF85736A),
    outlineVariant = Color(0xFFD7C2B8),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = CrustLight,
    onPrimary = Color(0xFF552100),
    primaryContainer = Color(0xFF783512),
    onPrimaryContainer = Color(0xFFFFDBC9),
    secondary = Color(0xFFEDC248),
    onSecondary = Color(0xFF402D00),
    secondaryContainer = Color(0xFF5C4200),
    onSecondaryContainer = Color(0xFFFFE08A),
    tertiary = Color(0xFFB3CEAE),
    onTertiary = Color(0xFF203520),
    tertiaryContainer = Color(0xFF364C34),
    onTertiaryContainer = Color(0xFFCFEAC9),
    background = Rye,
    onBackground = Color(0xFFF0DFD7),
    surface = Rye,
    onSurface = Color(0xFFF0DFD7),
    surfaceVariant = Color(0xFF52443C),
    onSurfaceVariant = Color(0xFFD7C2B8),
    surfaceContainerLowest = Color(0xFF140D0A),
    surfaceContainerLow = Color(0xFF221A16),
    surfaceContainer = Color(0xFF271E19),
    surfaceContainerHigh = Color(0xFF322823),
    surfaceContainerHighest = Color(0xFF3D332D),
    outline = Color(0xFFA08D84),
    outlineVariant = Color(0xFF52443C),
    error = Color(0xFFFFB4AB),
)

private val Serif = FontFamily.Serif

private val ProofTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = Serif, fontWeight = FontWeight.SemiBold),
        headlineLarge = headlineLarge.copy(fontFamily = Serif, fontWeight = FontWeight.SemiBold),
        headlineMedium = headlineMedium.copy(fontFamily = Serif, fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontFamily = Serif, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontFamily = Serif, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    )
}

/** Card artwork colours per category. Softer in dark mode so emoji still pop. */
data class CategoryPalette(val start: Color, val end: Color) {
    val brush: Brush get() = Brush.linearGradient(listOf(start, end))
}

class ProofExtras(val dark: Boolean) {
    fun palette(category: Category): CategoryPalette = if (!dark) {
        when (category) {
            Category.BREAD -> CategoryPalette(Color(0xFFFFE3C4), Color(0xFFF5B98A))
            Category.PASTRY -> CategoryPalette(Color(0xFFFFF0B8), Color(0xFFF7CB6B))
            Category.CAKES -> CategoryPalette(Color(0xFFFFE0E6), Color(0xFFF5A9B8))
            Category.COOKIES -> CategoryPalette(Color(0xFFF1DFCF), Color(0xFFC99A74))
            Category.DESSERTS -> CategoryPalette(Color(0xFFEADCF5), Color(0xFFC3A3E0))
            Category.BREAKFAST -> CategoryPalette(Color(0xFFDDF0D8), Color(0xFFA9D49F))
        }
    } else {
        when (category) {
            Category.BREAD -> CategoryPalette(Color(0xFF5A3418), Color(0xFF3A2010))
            Category.PASTRY -> CategoryPalette(Color(0xFF5C4612), Color(0xFF3A2C08))
            Category.CAKES -> CategoryPalette(Color(0xFF5E2A36), Color(0xFF3C1A22))
            Category.COOKIES -> CategoryPalette(Color(0xFF4E3626), Color(0xFF2F2018))
            Category.DESSERTS -> CategoryPalette(Color(0xFF45305A), Color(0xFF2A1D38))
            Category.BREAKFAST -> CategoryPalette(Color(0xFF2F4A2A), Color(0xFF1C2E1A))
        }
    }

    val numberStyle = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold)
}

val LocalProofExtras = staticCompositionLocalOf { ProofExtras(dark = false) }

@Composable
fun ProofTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalProofExtras provides ProofExtras(darkTheme)) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = ProofTypography,
            content = content,
        )
    }
}
