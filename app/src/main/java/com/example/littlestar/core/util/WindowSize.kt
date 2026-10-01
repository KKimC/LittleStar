package com.example.littlestar.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/**
 * 화면 크기 구분. Compose에서 제공하는 Window Size Class 개념을
 * `LocalConfiguration.screenWidthDp`로 간단히 재현한다.
 *
 *  - Compact  : 폰 세로
 *  - Medium   : 폰 가로 / 작은 태블릿 세로
 *  - Expanded : 태블릿 가로 (넓은 화면)
 */
enum class WindowSizeClass {
    Compact,
    Medium,
    Expanded,
}

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    // screenWidthDp는 회전 시 자동으로 갱신되므로 별도 상태 저장이 필요 없다.
    val widthDp = LocalConfiguration.current.screenWidthDp
    val heightDp = LocalConfiguration.current.screenHeightDp
    return remember(widthDp, heightDp) {
        when {
            widthDp >= 840 -> WindowSizeClass.Expanded
            widthDp >= 600 -> WindowSizeClass.Medium
            else -> WindowSizeClass.Compact
        }
    }
}

/** 가로(넓은) 화면에서 나란히 배치할지 여부 */
@Composable
fun isWideLayout(): Boolean =
    rememberWindowSizeClass() == WindowSizeClass.Expanded
