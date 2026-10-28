package com.bahalabs.sortopia.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bahalabs.sortopia.R

/** Board backgrounds. Midnight is free; the rest are bought with coins (a reason to keep playing). */
enum class BoardTheme(@StringRes val label: Int, val price: Int, val top: Color, val bottom: Color, val accent: Color) {
    MIDNIGHT(R.string.theme_midnight, 0, Color(0xFF2A1F6B), Color(0xFF120C30), Color(0xFF8B6CFF)),
    OCEAN(R.string.theme_ocean, 500, Color(0xFF0E5A8A), Color(0xFF062236), Color(0xFF3FD0FF)),
    SUNSET(R.string.theme_sunset, 800, Color(0xFF8A2D5B), Color(0xFF2B0F2E), Color(0xFFFF8A5B)),
    FOREST(R.string.theme_forest, 800, Color(0xFF1E5E46), Color(0xFF0A2219), Color(0xFF5BE0A0)),
    CANDY(R.string.theme_candy, 1200, Color(0xFFB04FA8), Color(0xFF3A1A5E), Color(0xFFFFB3E6)),
    ;

    val brush: Brush get() = Brush.verticalGradient(listOf(top, bottom))
}

/**
 * Item colours, picked to stay distinct from each other on dark backgrounds. Up to 12 are used
 * in one level; colour-blind players can turn on a symbol per colour.
 */
val ItemColors = listOf(
    Color(0xFFFF4D6D), // red
    Color(0xFF3DA5FF), // blue
    Color(0xFFFFD23F), // yellow
    Color(0xFF2EDB8B), // green
    Color(0xFFB15CFF), // purple
    Color(0xFFFF9F1C), // orange
    Color(0xFFFF8FD8), // pink
    Color(0xFF26E0E0), // cyan
    Color(0xFFB7E34A), // lime
    Color(0xFF9C6B4E), // brown
    Color(0xFFF4F1EA), // white
    Color(0xFF3F51D9), // navy
)

val Gold = Color(0xFFFFC83D)
val CardWhite = Color(0x1FFFFFFF)
val CardBorder = Color(0x33FFFFFF)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Black, fontSize = 36.sp, letterSpacing = 0.5.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp),
)

@Composable
fun SortopiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF8B6CFF),
            onPrimary = Color.White,
            secondary = Gold,
            surface = Color(0xFF231A55),
            onSurface = Color.White,
            background = Color(0xFF1B1442),
            onBackground = Color.White,
        ),
        typography = AppTypography,
        content = content,
    )
}
