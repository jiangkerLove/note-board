package com.jiangker.noteboard.common

data class Pos(val x: Float, val y: Float)

sealed class NoteElement(
    val t: String
) {
    data class Line(
        val color: ULong,
        val width: Float,
        val poss: List<Pos>,
        val writing: Boolean,
    ) : NoteElement("Line") {
        override fun update(element: NoteElement): NoteElement {
            val line = element as Line
            val mutableList = poss.toMutableList()
            mutableList.addAll(line.poss)
            return copy(color = line.color, width = line.width, poss = mutableList, writing = line.writing)
        }
    }

    abstract fun update(element: NoteElement):NoteElement
}

sealed class NoteOperation(
    val t: String
) {
    abstract val id: String

    data class AddElement(
        val element: NoteElement,
        override val id: String
    ) : NoteOperation("Add") {
        override fun update(operation: NoteOperation): NoteOperation {
            val addElement = (operation as AddElement).element
            return copy(element = element.update(addElement))
        }
    }

    data class RemoveElement(
        override val id: String
    ) : NoteOperation("Remove") {
        override fun update(operation: NoteOperation): NoteOperation {
            return copy(id = operation.id)
        }
    }

    data class CleanElement(
        override val id: String
    ) : NoteOperation("Clean") {
        override fun update(operation: NoteOperation): NoteOperation {
            return copy(id = operation.id)
        }
    }

    abstract fun update(operation: NoteOperation):NoteOperation
}