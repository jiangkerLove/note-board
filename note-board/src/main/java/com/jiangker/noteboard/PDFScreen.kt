package com.jiangker.noteboard

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.util.FitPolicy

@SuppressLint("ResourceType", "ClickableViewAccessibility")
@Composable
fun PDFScreen(
    modifier: Modifier,
    pdfView: PDFView,
    noteSource: NoteSource
) {
    LaunchedEffect(key1 = pdfView, block = {
        noteSource.open(pdfView)
            .enableSwipe(true)
            .swipeHorizontal(false).autoSpacing(false).pageSnap(false)
            .pageFling(false).enableDoubletap(false)
            .enableAnnotationRendering(true).enableAntialiasing(true)
            .spacing(0).fitEachPage(true)
            .pageFitPolicy(
                FitPolicy.WIDTH
            ).load()
    })
    AndroidView(
        factory = {
            pdfView
        },
        modifier = modifier,
        update = {

        }
    )

}