package com.jiangker.noteboard

import com.github.barteksc.pdfviewer.PDFView

sealed interface NoteSource {

    fun open(pdfView: PDFView): PDFView.Configurator

    data class File(
        val path: String
    ) : NoteSource {
        override fun open(pdfView: PDFView): PDFView.Configurator {
            return pdfView.fromFile(java.io.File(path))
        }
    }

}