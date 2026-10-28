package io.github.jennaslee.silverapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 어르신용: 높은 대비 + 큰 글씨. 다크 모드는 대비 검증 후 추가한다.
private val SilverColors = lightColorScheme(
    primary = Color(0xFF1F5FAE),
    onPrimary = Color.White,
    error = Color(0xFFB3261E),
    onError = Color.White,
    background = Color.White,
    onBackground = Color(0xFF111111),
    surface = Color.White,
    onSurface = Color(0xFF111111),
)

private val SilverTypography = Typography(
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 22.sp, lineHeight = 32.sp),
    bodyMedium = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
    labelLarge = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
)

@Composable
fun SilverTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SilverColors,
        typography = SilverTypography,
        content = content,
    )
}
