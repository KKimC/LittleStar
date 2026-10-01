package com.example.littlestar.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.littlestar.feature.dashboard.DashboardScreen
import com.example.littlestar.feature.detail.SheetDetailScreen
import com.example.littlestar.feature.scanner.ScannerScreen
import com.example.littlestar.feature.settings.SettingsScreen
import com.example.littlestar.feature.viewer.PracticeViewerScreen

/**
 * 앱 전체 네비게이션 그래프.
 * Dashboard -> SheetDetail -> PracticeViewer 흐름과
 * Dashboard -> Settings / Scanner 이동을 정의한다.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenScanner = { navController.navigate(Routes.SCANNER) },
                onOpenSheet = { sheetId -> navController.navigate(Routes.sheetDetail(sheetId)) },
            )
        }

        composable(
            route = Routes.SHEET_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_SHEET_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val sheetId = backStackEntry.arguments?.getLong(Routes.ARG_SHEET_ID) ?: 0L
            SheetDetailScreen(
                sheetId = sheetId,
                onBack = { navController.popBackStack() },
                onStartPractice = { navController.navigate(Routes.practiceViewer(sheetId)) },
                onRescanPages = { navController.navigate(Routes.scannerReplace(sheetId)) },
            )
        }

        composable(
            route = Routes.PRACTICE_VIEWER,
            arguments = listOf(navArgument(Routes.ARG_SHEET_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val sheetId = backStackEntry.arguments?.getLong(Routes.ARG_SHEET_ID) ?: 0L
            PracticeViewerScreen(
                sheetId = sheetId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SCANNER) {
            ScannerScreen(
                onBack = { navController.popBackStack() },
                onSaved = { sheetId ->
                    navController.navigate(Routes.sheetDetail(sheetId)) {
                        popUpTo(Routes.DASHBOARD)
                    }
                },
            )
        }

        composable(
            route = Routes.SCANNER_REPLACE,
            arguments = listOf(navArgument(Routes.ARG_SHEET_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val sheetId = backStackEntry.arguments?.getLong(Routes.ARG_SHEET_ID) ?: 0L
            ScannerScreen(
                replaceSheetId = sheetId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
    }
}
