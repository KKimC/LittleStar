package com.example.littlestar.feature.viewer

import androidx.compose.ui.graphics.Color

/** 필기 브러시 종류 */
enum class InkBrushType(val label: String) {
    PEN("펜"),
    MARKER("마커"),
    HIGHLIGHTER("형광펜"),
}

/** 팔레트 기본 색상 */
val InkPaletteColors = listOf(
    Color(0xFF303B32),
    Color(0xFF718A78),
    Color(0xFF000000),
    Color(0xFFE05252),
    Color(0xFF2F6FBF),
    Color(0xFFE8A33D),
)

/** 굵기 범위 */
const val INK_MIN_SIZE = 1f
const val INK_MAX_SIZE = 16f
