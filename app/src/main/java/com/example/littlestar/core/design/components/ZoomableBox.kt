package com.example.littlestar.core.design.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 핀치 줌/팬을 지원하는 컨테이너.
 *
 * 줌이 안 된 상태(scale == 1)에서 한 손가락 드래그는 소모하지 않아서
 * 부모(HorizontalPager)의 좌우 스와이프가 정상 동작한다.
 * 확대된 상태에서는 한 손가락으로 팬하고, 두 손가락으로 핀치 줌/팬한다.
 */
@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()

                        if (pressedCount >= 2) {
                            // 두 손가락: 핀치 줌/팬
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            offset = if (newScale <= 1.001f) Offset.Zero else offset + pan
                            event.changes.forEach { it.consume() }
                        } else if (scale > 1f) {
                            // 한 손가락 + 확대 상태: 팬
                            offset += pan
                            event.changes.forEach { it.consume() }
                        }
                        // 한 손가락 + 축소 상태: 소모하지 않아 부모가 스와이프 처리
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        ) {
            content()
        }
    }
}
