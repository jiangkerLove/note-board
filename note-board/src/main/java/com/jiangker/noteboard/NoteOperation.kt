package com.jiangker.noteboard

data class Pos(val x: Float, val y: Float)

sealed class NoteElement(
    val t: String
) {
    data class Line(
        val color: ULong,
        val width: Float,
        val poss: List<Pos>,
        val writing: Boolean,
    ) : NoteElement("Line")
}

sealed class NoteOperation(
    val t: String
) {
    abstract val id: String

    data class AddElement(
        val element: NoteElement,
        override val id: String
    ) : NoteOperation("Add")

    data class RemoveElement(
        override val id: String
    ) : NoteOperation("Remove")

    data class CleanElement(
        override val id: String
    ) : NoteOperation("Clean")

}