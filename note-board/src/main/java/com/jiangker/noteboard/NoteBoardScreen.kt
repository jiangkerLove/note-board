package com.jiangker.noteboard

import android.annotation.SuppressLint
import android.util.Xml
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
    noteState: NoteBoardScreenState,
    content: @Composable BoxScope.(Modifier) -> Unit
) {
    var boxRect by remember { mutableStateOf(Rect.Zero) }
    val context = LocalContext.current
    val path = remember { mutableStateListOf<Offset>() }
    var isFirstMove = false
    var paintId by remember { mutableStateOf<PointerId?>(null) }

    val paths = remember { mutableStateListOf<PathItem>() }
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
                                    path.add(offset)
                                    if (isClean) {
                                        val newList = paths
                                            .filter { item ->
                                                pathSegment(path.last(), item.positions).not()
                                            }
                                            .toList()
                                        paths.clear()
                                        paths.addAll(newList)
                                    }
                                }
                            if (pointerEvent.type == PointerEventType.Release && pointerEvent.changes.size == 1) {
                                if (path.isNotEmpty() && isClean.not())
                                    paths.add(
                                        PathItem(
                                            color = pathColor,
                                            width = pathWidth,
                                            path.toList(),
                                            isClean = isClean
                                        )
                                    )
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
                                offsetX,
                                offsetY,
                                item.color,
                                item.width,
                                item.positions,
                                item.isClean
                            )
                    }
                    if (isClean.not()) {
                        it.drawPathWithOffset(
                            offsetX,
                            offsetY,
                            pathColor,
                            pathWidth,
                            path,
                            isClean
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
                    paths.clear()
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
                    painter = painterResource(id = if (isClean) R.drawable.ic_clean_select else R.drawable.ic_clean),
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
                    painter = painterResource(id = if (isClean) R.drawable.ic_clean else R.drawable.ic_clean_select),
                    contentDescription = null
                )
                Text(text = "画笔", modifier = Modifier, fontSize = 12.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(
                modifier = Modifier.clickable {
                    PDFUtil.write(basePath = context.filesDir.path, boxRect.width.roundToInt(), paths)
                }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_clean),
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
    offsetX: Float,
    offsetY: Float,
    color: Color,
    width: Float,
    path: List<Offset>,
    isClean: Boolean,
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
        paint.blendMode = if (isClean) BlendMode.DstOut else BlendMode.SrcOver
        drawPath(
            drawPath,
            paint
        )
    }
}

fun pathSegment(offset: Offset, path: List<Offset>): Boolean {
    path.forEach {
        val distance = (it - offset).getDistance()
        println(distance)
        if (distance < 1F) return true
    }
    return false
}

const val EPSILON = 0.5F; // 设定一个小的容差值
fun doLineSegmentsIntersect(startA: Offset, endA: Offset, startB: Offset, endB: Offset): Boolean {

    // 计算四个方向值
    val d1 = direction(startB, endB, startA);
    val d2 = direction(startB, endB, endA);
    val d3 = direction(startA, endA, startB);
    val d4 = direction(startA, endA, endB);

    // 检查是否相交
    if (d1 * d2 < -EPSILON && d3 * d4 < -EPSILON) {
        return true;
    }

    // 检查是否在边界上
    if (Math.abs(d1) < EPSILON && onSegment(startB, endB, startA, EPSILON)) return true;
    if (Math.abs(d2) < EPSILON && onSegment(startB, endB, endA, EPSILON)) return true;
    if (Math.abs(d3) < EPSILON && onSegment(startA, endA, startB, EPSILON)) return true;
    if (Math.abs(d4) < EPSILON && onSegment(startA, endA, endB, EPSILON)) return true;

    return false;
}

// 计算方向值
fun direction(p: Offset, q: Offset, r: Offset): Float {
    return (q.x - p.x) * (r.y - p.y) - (q.y - p.y) * (r.x - p.x);
}

// 检查点是否在线段上
fun onSegment(p: Offset, q: Offset, r: Offset, epsilon: Float): Boolean {
    return Math.min(p.x, q.x) - epsilon <= r.x && r.x <= Math.max(p.x, q.x) + epsilon &&
            Math.min(p.y, q.y) - epsilon <= r.y && r.y <= Math.max(p.y, q.y) + epsilon;
}
