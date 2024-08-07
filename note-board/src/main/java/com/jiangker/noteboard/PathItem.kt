package com.jiangker.noteboard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class PathItem(
    val color: Color,
    val width: Float,
    val positions: List<Offset>,
    val isClean: Boolean,
) {
    val top: Float = positions.minOf { it.y }
    val bottom: Float = positions.maxOf { it.y }
    val start: Float = positions.minOf { it.x }
    val end: Float = positions.maxOf { it.x }
//    val controlItems: List<ControlPoint> = ControlPoint.getControlPointList(positions)
    private fun split(origin: List<Offset>, lines: MutableList<List<Offset>>, offset: Offset) {
        val line = mutableListOf<Offset>()
        origin.forEach {
            if ((it - offset).getDistance() < 20) {
                if (line.isNotEmpty()) {
                    lines.add(line.toList())
                    line.clear()
                }
            } else {
                line.add(it)
            }
        }
        if (line.isNotEmpty()) {
            lines.add(line.toList())
            line.clear()
        }
    }
}
