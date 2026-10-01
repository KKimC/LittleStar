package com.example.littlestar.core.navigation

/**
 * 앱 전체 화면 경로 정의.
 * Navigation Compose의 route 문자열과, 인자를 포함한 경로를 만드는 헬퍼를 제공한다.
 */
object Routes {
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val SCANNER = "scanner"

    const val SCANNER_REPLACE = "scanner_replace/{sheetId}"

    const val SHEET_DETAIL = "sheet_detail/{sheetId}"
    const val PRACTICE_VIEWER = "practice_viewer/{sheetId}"

    const val ARG_SHEET_ID = "sheetId"

    fun sheetDetail(sheetId: Long): String = "sheet_detail/$sheetId"
    fun practiceViewer(sheetId: Long): String = "practice_viewer/$sheetId"
    fun scannerReplace(sheetId: Long): String = "scanner_replace/$sheetId"
}
