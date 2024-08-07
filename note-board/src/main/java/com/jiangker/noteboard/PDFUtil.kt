package com.jiangker.noteboard

import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject


object PDFUtil {

    fun write(basePath: String, disWidth: Int, paths: List<PathItem>) {
        val pdfReader = PdfReader("$basePath/api.pdf")
        val pdfWriter = PdfWriter("$basePath/api_tmp.pdf")
        val pdfDeaderDoc = PdfDocument(pdfReader)
        val pdfWriterDoc = PdfDocument(pdfWriter)
        var totalHeight = 0.0
        for (i in 1..pdfDeaderDoc.numberOfPages) {
            val pdfPage = pdfDeaderDoc.getPage(i)
            val addPage =
                pdfWriterDoc.addNewPage(PageSize(pdfPage.pageSize.width, pdfPage.pageSize.height))
            val pageCopy: PdfFormXObject = pdfPage.copyAsFormXObject(pdfWriterDoc)
            val canvas = PdfCanvas(addPage)
            canvas.addXObject(pageCopy)
            val pageHeight = pageCopy.height

            val scale = pageCopy.width / disWidth

            println("scale:${pageCopy.width / disWidth}")
            paths.forEach { item ->
                if (item.bottom * scale - totalHeight >= 0 && item.top * scale - totalHeight <= pageHeight) {
                    if (item.isClean) {
                        canvas.clip()
                    } else {
                        canvas.setStrokeColorRgb(
                            item.color.red,
                            item.color.green,
                            item.color.blue
                        )
                    }
                    canvas.setLineWidth(item.width * scale)
                    if (item.positions.isNotEmpty()) {
                        canvas.moveTo(
                            (item.positions[0].x.toDouble() * scale),
                            (pageHeight - (item.positions[0].y * scale - totalHeight))
                        )

                        for (y in 1 until item.positions.size) {
                            canvas.lineTo(
                                (item.positions[y].x.toDouble() * scale),
                                (pageHeight - (item.positions[y].y * scale - totalHeight))
                            )
                        }
                    }
                    if (item.isClean) {
                        canvas.clip()
                    } else {
                        canvas.stroke()
                    }
                }
            }
            totalHeight += pageHeight
            canvas.closePath()
        }

        pdfDeaderDoc.close()
        pdfWriterDoc.close()
    }
}