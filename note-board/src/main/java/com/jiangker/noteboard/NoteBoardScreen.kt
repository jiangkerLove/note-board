package com.jiangker.noteboard

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.barteksc.pdfviewer.PDFView
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
    onPathDown: (Color, Float) -> Unit,
    onPathMove: (Offset) -> Unit,
    onPathUp: (PathItem) -> Unit,
    onClean: () -> Unit,
    onEraser: (List<String>) -> Unit,
    paths: List<PathItem>,
    noteState: NoteBoardScreenState,
    content: @Composable BoxScope.(Modifier) -> Unit
) {
    var boxRect by remember { mutableStateOf(Rect.Zero) }
    val context = LocalContext.current
    val path = remember { mutableStateListOf<Offset>() }
    var isFirstMove = false
    var paintId by remember { mutableStateOf<PointerId?>(null) }

    var pathColor by remember { mutableStateOf(Color.Red) }
    var pathWidth by remember { mutableFloatStateOf(5F) }
    var isClean by remember { mutableStateOf(false) }
    var showSelectColor by remember { mutableStateOf(false) }
    var showSelectWith by remember { mutableStateOf(false) }

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
                                    if (isClean.not())
                                        onPathDown(pathColor, pathWidth)
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
                                    if (isClean.not()) {
                                        onPathMove(offset)
                                    }
                                    path.add(offset)
                                }
                            if (pointerEvent.type == PointerEventType.Release && pointerEvent.changes.size == 1) {
                                if (path.isNotEmpty())
                                    if (isClean) {
                                        val offsetY = noteState.offsetY / noteState.scale
                                        if (path.size > 2) {
                                            val itemList = paths
                                                .filter { item ->
                                                    item.bottom + offsetY >= 0 && item.top + offsetY <= boxRect.height &&
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
                                                color = pathColor,
                                                width = pathWidth,
                                                path.toList(),
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
                    if (isClean.not()) {
                        it.drawPathWithOffset(
                            pathColor,
                            pathWidth,
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(color = Color.Gray)
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier.clickable {
                    onClean()
                }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_clean),
                    contentDescription = null
                )
                Text(text = "清除", modifier = Modifier, fontSize = 12.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.width(20.dp))

            Column(
                modifier = Modifier.clickable {
                    showSelectColor = true
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(color = pathColor)
                )

                Text(text = "颜色", modifier = Modifier, fontSize = 12.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.width(20.dp))


            Column(
                modifier = Modifier.clickable {
                    showSelectWith = true
                }
            ) {
                Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(pathWidth.dp)
                            .clip(CircleShape)
                            .background(color = pathColor)
                    )
                }
                Text(text = "画笔大小", modifier = Modifier, fontSize = 12.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(
                modifier = Modifier.clickable {
                    isClean = true
                }
            ) {
                Image(
                    painter = painterResource(id = if (isClean) R.drawable.ic_eraser_select else R.drawable.ic_eraser),
                    contentDescription = null
                )
                Text(text = "橡皮", modifier = Modifier, fontSize = 12.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(
                modifier = Modifier.clickable {
                    isClean = false
                }
            ) {
                Image(
                    painter = painterResource(id = if (isClean) R.drawable.ic_pen else R.drawable.ic_pen_select),
                    contentDescription = null
                )
                Text(text = "画笔", modifier = Modifier, fontSize = 12.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(
                modifier = Modifier.clickable {
                    PDFUtil.write(
                        basePath = context.filesDir.path,
                        boxRect.width.roundToInt(),
                        paths
                    )
                }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_save),
                    contentDescription = null
                )
                Text(text = "保存", modifier = Modifier, fontSize = 12.sp, color = Color.White)
            }
        }

        if (showSelectColor) {
            Column(modifier = Modifier
                .fillMaxSize()
                .clickable { showSelectColor = false }) {
                Spacer(modifier = Modifier.height(70.dp))
                Row(
                    modifier = Modifier
                        .wrapContentSize()
                        .background(Color.Gray)
                ) {
                    listOf(Color.Red, Color.Blue, Color.Green).forEach {
                        Box(modifier = Modifier
                            .padding(10.dp)
                            .size(30.dp)
                            .background(it)
                            .clickable {
                                pathColor = it
                                showSelectColor = false
                            })
                    }
                }
            }
        }

        if (showSelectWith) {
            Column(modifier = Modifier
                .fillMaxSize()
                .clickable { showSelectWith = false }) {
                Spacer(modifier = Modifier.height(70.dp))
                Row(
                    modifier = Modifier
                        .wrapContentHeight()
                        .width(500.dp)
                        .background(Color.Gray)
                ) {
                    Slider(
                        value = pathWidth,
                        onValueChange = { pathWidth = it },
                        steps = 11,
                        valueRange = 2f..20f
                    )
                }
            }
        }
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