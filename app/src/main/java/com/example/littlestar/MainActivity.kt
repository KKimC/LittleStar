package com.example.littlestar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.example.littlestar.core.design.theme.PianoFlowTheme
import com.example.littlestar.core.navigation.AppNavHost
import com.example.littlestar.di.AppContainer
import com.example.littlestar.di.LocalAppContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 앱 전체에서 시스템 바(상태바/내비게이션 바)를 숨기고,
        // 화면 하단에서 스와이프로 끌어올릴 때만 일시적으로 표시한다.
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        setContent {
            PianoFlowTheme {
                // Application에서 생성한 컨테이너를 재사용한다.
                val appContainer: AppContainer = (application as PianoFlowApplication).container
                CompositionLocalProvider(LocalAppContainer provides appContainer) {
                    val navController = rememberNavController()
                    AppNavHost(navController = navController)
                }
            }
        }
    }
}
