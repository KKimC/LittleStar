package com.example.littlestar.di

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 컴포지션 트리에서 [AppContainer]를 전달받기 위한 CompositionLocal.
 * MainActivity에서 값을 제공한다.
 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer가 제공되지 않았습니다. MainActivity에서 CompositionLocalProvider로 감싸야 합니다.")
}
