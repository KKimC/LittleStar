package com.example.littlestar.feature.viewer

import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.ink.authoring.compose.InProgressStrokes
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import com.example.littlestar.data.storage.InkStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 악보 페이지 위 필기 오버레이. AndroidX Ink 기반.
 * 필압 반영(압력 펜 브러시) + 저지연 렌더 + undo/clear + 페이지별 저장/복원을 지원한다.
 */
@Composable
fun InkOverlay(
    sheetId: Long,
    pageOrder: Int,
    inkStorage: InkStorage,
    brushType: InkBrushType,
    color: Color,
    size: Float,
    clearSignal: Int,
    undoSignal: Int,
    saveScope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    val finishedStrokes = remember(sheetId, pageOrder) { mutableStateListOf<Stroke>() }
    val renderer = remember { CanvasStrokeRenderer.create() }
    val matrix = remember { Matrix() }

    fun save() {
        val snapshot = finishedStrokes.toList()
        saveScope.launch { inkStorage.saveStrokes(sheetId, pageOrder, snapshot) }
    }

    // 페이지 진입 시 기존 필기 로드
    LaunchedEffect(sheetId, pageOrder) {
        val loaded = inkStorage.loadStrokes(sheetId, pageOrder)
        if (loaded.isNotEmpty()) {
            finishedStrokes.clear()
            finishedStrokes.addAll(loaded)
        }
    }

    LaunchedEffect(clearSignal) {
        if (clearSignal > 0) {
            finishedStrokes.clear()
            save()
        }
    }
    LaunchedEffect(undoSignal) {
        if (undoSignal > 0 && finishedStrokes.isNotEmpty()) {
            finishedStrokes.removeAt(finishedStrokes.lastIndex)
            save()
        }
    }

    // 페이지 이동/회전/이탈 시 저장
    DisposableEffect(sheetId, pageOrder) {
        onDispose { save() }
    }

    Box(modifier = modifier) {
        // key()로 브러시 설정이 바뀌면 InProgressStrokes를 재생성해 새 브러시가 즉시 반영되게 한다.
        key(brushType, color, size) {
            val family = when (brushType) {
                InkBrushType.PEN -> StockBrushes.pressurePen()
                InkBrushType.MARKER -> StockBrushes.marker()
                InkBrushType.HIGHLIGHTER -> StockBrushes.highlighter()
            }
            val effectiveColor = if (brushType == InkBrushType.HIGHLIGHTER) {
                color.copy(alpha = 0.45f)
            } else {
                color
            }
            val brush = Brush.createWithColorIntArgb(
                family = family,
                colorIntArgb = effectiveColor.toArgb(),
                size = size,
                epsilon = 0.1f,
            )
            InProgressStrokes(
                defaultBrush = brush,
                onStrokesFinished = { strokes ->
                    finishedStrokes.addAll(strokes)
                    save()
                },
            )
        }
        Canvas(Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                finishedStrokes.forEach { stroke ->
                    renderer.draw(canvas.nativeCanvas, stroke, matrix)
                }
            }
        }
    }
}
