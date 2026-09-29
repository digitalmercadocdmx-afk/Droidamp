package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.CyberdeckSkin

val MatrixColorScheme = darkColorScheme(
    primary = MatrixGreenPrimary,
    onPrimary = Color(0xFF003822),
    primaryContainer = MatrixGreenSurfaceVariant,
    onPrimaryContainer = MatrixGreenPrimary,
    secondary = MatrixGreenSecondary,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = MatrixGreenSecondary,
    background = MatrixGreenBackground,
    onBackground = Color(0xFFE1F5EC),
    surface = MatrixGreenSurface,
    onSurface = Color(0xFFE1F5EC),
    surfaceVariant = MatrixGreenSurfaceVariant,
    onSurfaceVariant = Color(0xFF8FFAD1),
    tertiary = MatrixGreenAccent
)

val AmberColorScheme = darkColorScheme(
    primary = SynthwaveAmberPrimary,
    onPrimary = Color(0xFF422C00),
    primaryContainer = SynthwaveAmberSurfaceVariant,
    onPrimaryContainer = SynthwaveAmberPrimary,
    secondary = SynthwaveAmberSecondary,
    onSecondary = Color(0xFF551400),
    secondaryContainer = Color(0xFF752100),
    onSecondaryContainer = Color(0xFFFFCCAA),
    background = SynthwaveAmberBackground,
    onBackground = Color(0xFFFFF2DF),
    surface = SynthwaveAmberSurface,
    onSurface = Color(0xFFFFF2DF),
    surfaceVariant = SynthwaveAmberSurfaceVariant,
    onSurfaceVariant = Color(0xFFFFD48F),
    tertiary = SynthwaveAmberAccent
)

val CyberpunkColorScheme = darkColorScheme(
    primary = CyberpunkNeonPrimary,
    onPrimary = Color(0xFF00363D),
    primaryContainer = CyberpunkNeonSurfaceVariant,
    onPrimaryContainer = CyberpunkNeonPrimary,
    secondary = CyberpunkNeonSecondary,
    onSecondary = Color(0xFF520025),
    secondaryContainer = Color(0xFF7A0039),
    onSecondaryContainer = Color(0xFFFFB0CF),
    background = CyberpunkNeonBackground,
    onBackground = Color(0xFFF3EDFF),
    surface = CyberpunkNeonSurface,
    onSurface = Color(0xFFF3EDFF),
    surfaceVariant = CyberpunkNeonSurfaceVariant,
    onSurfaceVariant = Color(0xFFD4C2FF),
    tertiary = CyberpunkNeonAccent
)

@Composable
fun DroidampTheme(
    skin: CyberdeckSkin = CyberdeckSkin.MATRIX_GREEN,
    content: @Composable () -> Unit
) {
    val colorScheme = when (skin) {
        CyberdeckSkin.MATRIX_GREEN -> MatrixColorScheme
        CyberdeckSkin.SYNTHWAVE_AMBER -> AmberColorScheme
        CyberdeckSkin.NEON_CYBERPUNK -> CyberpunkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    DroidampTheme(skin = CyberdeckSkin.MATRIX_GREEN, content = content)
}

