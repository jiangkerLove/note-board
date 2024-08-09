package com.jiangker.noteboard

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import com.github.barteksc.pdfviewer.PDFView
import kotlin.math.max
import kotlin.math.min

@Composable
fun rememberNoteBoardScreenState(pdfView: PDFView): NoteBoardScreenState {
    return remember {
        object : NoteBoardScreenState {
            override val offsetX: Float
                get() = pdfView.currentXOffset
            override val offsetY: Float
                get() = pdfView.currentYOffset
            override val scale: Float
                get() = pdfView.zoom

        }
    }
}

@Stable
interface NoteBoardScreenState {
    val offsetX: Float
    val offsetY: Float
    val scale: Float
}

@SuppressLint("ResourceType")
@Composable
fun NoteBoardScreen(
    config: OptionConfig,
    onPathDown: (Color, Float) -> Unit,
    onPathMove: (Offset) -> Unit,
    onPathUp: (PathItem) -> Unit,
    onEraser: (List<String>) -> Unit,
    paths: List<PathItem>,
    noteState: NoteBoardScreenState,
    content: @Composable BoxScope.(Modifier) -> Unit,
    optionContent: @Composable BoxScope.(Modifier) -> Unit
) {
    var boxRect by remember { mutableStateOf(Rect.Zero) }
    val path = remember { mutableStateListOf<Offset>() }
    var isFirstMove = false
    var paintId by remember { mutableStateOf<PointerId?>(null) }

    var optConfig by remember { mutableStateOf(config) }

    LaunchedEffect(key1 = config) {
        optConfig = config
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content(Modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val pointerEvent = awaitPointerEvent()
                        when (pointerEvent.type) {
                            PointerEventType.Press -> {
                                if (pointerEvent.changes.size == 1) {
                                    isFirstMove = true
                                    path.clear()
                                    paintId = pointerEvent.changes.first().id
                                    if (optConfig.isClean.not())
                                        onPathDown(optConfig.color, optConfig.width)
                                }
                            }

                            PointerEventType.Move -> {
                                if (isFirstMove) {
                                    if (pointerEvent.changes.size == 1) {
                                        pointerEvent.changes
                                            .first()
                                            .consume()
                                    } else {
                                        paintId = null
                                        path.clear()
                                    }
                                }

                                isFirstMove = false
                            }

                            else -> {

                            }
                        }
                        if (paintId != null) {
                            pointerEvent.changes
                                .find { it.id == paintId }
                                ?.let {
                                    val offset = Offset(
                                        (it.position.x - noteState.offsetX) / noteState.scale,
                                        (it.position.y - noteState.offsetY) / noteState.scale
                                    )
                                    if (optConfig.isClean.not()) {
                                        onPathMove(offset)
                                    }
                                    path.add(offset)
                                }
                            if (pointerEvent.type == PointerEventType.Release && pointerEvent.changes.size == 1) {
                                if (path.isNotEmpty())
                                    if (optConfig.isClean) {
                                        val offsetY = noteState.offsetY / noteState.scale
                                        if (path.size > 2) {
                                            val itemList = paths
                                                .filter { item ->
                                                    item.writing.not() && item.bottom + offsetY >= 0 && item.top + offsetY <= boxRect.height &&
                                                            inCleanSpace(
                                                                path.first(),
                                                                path.last(),
                                                                item.positions
                                                            )
                                                }
                                                .map { it.id }
                                                .toList()
                                            onEraser(itemList)
                                        }
                                    } else {
                                        onPathUp(
                                            PathItem(
                                                id = "",
                                                color = optConfig.color,
                                                width = optConfig.width,
                                                positions = path.toList(),
                                                writing = false
                                            )
                                        )
                                    }
                                path.clear()
                                paintId = null
                            }
                        }
                    }
                }
            }
            .onGloballyPositioned {
                boxRect = it.boundsInParent()
            }
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize(),
            onDraw = {
                drawIntoCanvas {
                    it.saveLayer(
                        Rect(0F, 0F, this.size.width, this.size.height),
                        Paint()
                    )

                    it.scale(noteState.scale)

                    val offsetY = noteState.offsetY / noteState.scale
                    val offsetX = noteState.offsetX / noteState.scale
                    it.translate(offsetX, offsetY)
                    paths.forEach { item ->
                        if (item.bottom + offsetY >= 0 && item.top + offsetY <= boxRect.height)
                            it.drawPathWithOffset(
                                item.color,
                                item.width,
                                item.positions
                            )
                    }
                    if (optConfig.isClean.not()) {
                        it.drawPathWithOffset(
                            optConfig.color,
                            optConfig.width,
                            path,
                        )
                    } else {
                        if (path.size > 1) {
                            val paint = Paint()
                            paint.strokeWidth = 5F
                            paint.style = PaintingStyle.Stroke
                            it.drawRect(
                                path.first().x, path.first().y, path.last().x, path.last().y, paint
                            )
                        }
                    }
                    it.restore()
                }
            })
        optionContent(Modifier)
    }
}


private fun Canvas.drawPathWithOffset(
    color: Color,
    width: Float,
    path: List<Offset>,
) {
    if (path.isNotEmpty()) {
        val drawPath = Path()
        drawPath.moveTo(
            path[0].x,
            path[0].y
        )
        for (i in 1 until path.size) {
            drawPath.lineTo(path[i].x, path[i].y)
        }

        val paint = Paint()
        paint.color = color
        paint.style = PaintingStyle.Stroke
        paint.strokeWidth = width
        paint.blendMode = BlendMode.SrcOver
        drawPath(
            drawPath,
            paint
        )
    }
}

fun inCleanSpace(leftTop: Offset, endBottom: Offset, path: List<Offset>): Boolean {
    val minX = min(leftTop.x, endBottom.x)
    val minY = min(leftTop.y, endBottom.y)
    val maxX = max(leftTop.x, endBottom.x)
    val maxY = max(leftTop.y, endBottom.y)
    path.forEach {
        if (it.x > minX && it.y > minY && it.x < maxX && it.y < maxY)
            return true
    }
    return false
}