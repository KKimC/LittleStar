package com.example.littlestar.core.design.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// PianoFlow는 밝은 아이보리 톤의 라이트 테마만 사용한다.
// 다크 모드와 동적 색상은 추후 단계에서 지원 예정이다.
private val LightColorScheme = lightColorScheme(
    primary = SageGreen,
    onPrimary = Surface,
    primaryContainer = SageGreenContainer,
    onPrimaryContainer = TextPrimary,
    secondary = SageGreenMuted,
    onSecondary = Surface,
    secondaryContainer = SageGreenContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = SageGreenDim,
    onTertiary = TextPrimary,
    background = Ivory,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantSoft,
    onSurfaceVariant = TextSecondary,
    outline = OutlineSoft,
    outlineVariant = SurfaceVariantSoft,
)

@Composable
fun PianoFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
