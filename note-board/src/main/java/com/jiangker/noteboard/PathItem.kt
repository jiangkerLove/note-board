package com.jiangker.noteboard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.jiangker.noteboard.common.NoteElement
import com.jiangker.noteboard.common.NoteOperation

data class PathItem(
    val id: String,
    val color: Color,
    val width: Float,
    val writing: Boolean,
    val positions: List<Offset>,
) {
    val top: Float = positions.minOf { it.y }
    val bottom: Float = positions.maxOf { it.y }
    val start: Float = positions.minOf { it.x }
    val end: Float = positions.maxOf { it.x }

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

fun PathItem.update(opera: NoteOperation.AddElement, pageWidth: Int): PathItem {
    return when (opera.element) {
        is NoteElement.Line -> {
            val line = opera.element as NoteElement.Line
            val posList = line.poss.map {
                Offset(
                    it.x * pageWidth,
                    it.y * pageWidth
                )
            }
            val mutableList = positions.toMutableList()
            mutableList.addAll(posList)
            copy(writing = line.writing, positions = mutableList)
        }
    }
}

fun NoteOperation.AddElement.toPathItem(pageWidth: Int): PathItem {
    return when (this.element) {
        is NoteElement.Line -> {
            val line = element as NoteElement.Line
            PathItem(
                id = id,
                color = Color(line.color),
                width = line.width,
                positions = line.poss.map {
                    Offset(
                        it.x * pageWidth,
                        it.y * pageWidth
                    )
                },
                writing = line.writing
            )
        }
    }
}