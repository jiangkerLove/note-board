package com.jiangker.noteboard.app

import android.annotation.SuppressLint
import android.util.Xml
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.github.barteksc.pdfviewer.PDFView
import com.jiangker.noteboard.NoteBoardScreen
import com.jiangker.noteboard.NoteSource
import com.jiangker.noteboard.OptionConfig
import com.jiangker.noteboard.OptionScreen
import com.jiangker.noteboard.PDFScreen
import com.jiangker.noteboard.PathItem
import com.jiangker.noteboard.R
import com.jiangker.noteboard.common.NoteElement
import com.jiangker.noteboard.common.NoteOperation
import com.jiangker.noteboard.common.Pos
import com.jiangker.noteboard.rememberNoteBoardScreenState
import com.jiangker.noteboard.toPathItem
import com.jiangker.noteboard.update
import org.xmlpull.v1.XmlPullParser

@SuppressLint("ResourceType")
@Composable
fun BoardScreen(navController: NavHostController, path: String) {

    val context = LocalContext.current

    val paths = remember { mutableStateListOf<PathItem>() }
    var optionConfig by remember {
        mutableStateOf(
            OptionConfig(
                color = Color.Red,
                width = 5F,
                isClean = false
            )
        )
    }
    var currentOpt: NoteOperation? = null

    val pageWidth = remember {
        context.resources.displayMetrics.widthPixels
    }

    val pdfView = remember {
        val resources = context.resources

        val parser = resources.getXml(R.layout.view_pdf)
        val attributes = Xml.asAttributeSet(parser)

        var type: Int = parser.next()

        while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
            type = parser.next()
        }
        PDFView(context, attributes)
    }
    LaunchedEffect(Unit) {
        RsyncSocket.initBoard()
        RsyncSocket.sharedFlow.collect { list ->
            synchronized(MainActivity::class) {
                runCatching {
                    val mutableList = paths.toMutableList()
                    list.forEach { opera ->
                        val index =
                            mutableList.indexOfLast { item -> item.id == opera.id }
                        when (opera) {
                            is NoteOperation.AddElement -> {
                                if (index == -1) {
                                    mutableList.add(opera.toPathItem(pageWidth))
                                } else {
                                    mutableList[index] =
                                        mutableList[index].update(opera, pageWidth)
                                }
                            }

                            is NoteOperation.CleanElement -> {
                                val writingList =
                                    mutableList.filter { it.writing }.toList()
                                mutableList.clear()
                                mutableList.addAll(writingList)
                            }

                            is NoteOperation.RemoveElement -> {
                                if (index != -1) {
                                    mutableList.removeAt(index)
                                }
                            }

                            else -> {}
                        }
                    }
                    paths.clear()
                    paths.addAll(mutableList)
                }
            }
        }
    }

    DisposableEffect(key1 = Unit){
        onDispose {
            RsyncSocket.quitBoard()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NoteBoardScreen(
            config = optionConfig,
            onPathDown = { color, fl ->
                currentOpt = NoteOperation.AddElement(
                    id = System.currentTimeMillis().toString(),
                    element = NoteElement.Line(
                        color.value,
                        width = fl,
                        poss = mutableListOf(),
                    ),
                    writing = true
                )
                currentOpt?.let { RsyncSocket.rsyncItem(it) }
            },
            onPathMove = { off ->
                val element = currentOpt as NoteOperation.AddElement
                currentOpt = element.copy(
                    element = (element.element as NoteElement.Line).copy(
                        poss = mutableListOf(
                            Pos(
                                off.x / pdfView.width,
                                off.y / pdfView.width
                            )
                        )
                    )
                )
                currentOpt?.let { RsyncSocket.rsyncItem(it) }
            },
            onPathUp = {
                val element = currentOpt as NoteOperation.AddElement
                RsyncSocket.rsyncItem(
                    element.copy(
                        element = (element.element as NoteElement.Line).copy(
                            poss = mutableListOf()
                        ),
                        writing = false
                    )
                )
                synchronized(MainActivity::class) {
                    paths.add(it.copy(id = currentOpt!!.id))
                }
            },
            onEraser = { list ->
                synchronized(MainActivity::class) {
                    list.forEach {
                        RsyncSocket.rsyncItem(
                            NoteOperation.RemoveElement(id = it)
                        )
                    }
                    val itemList = paths.filter { list.contains(it.id).not() }.toList()
                    paths.clear()
                    paths.addAll(itemList)
                }
            },
            paths = paths,
            noteState = rememberNoteBoardScreenState(pdfView),
            content = {
                PDFScreen(
                    modifier = it.fillMaxSize(),
                    pdfView = pdfView,
                    noteSource = NoteSource.File(path)
                )
            }
        ) {
            OptionScreen(
                config = optionConfig,
                onCleanChanged = {
                    optionConfig = optionConfig.copy(isClean = it)
                }, onClean = {
                    synchronized(MainActivity::class) {
                        val newList = paths.filter { it.writing }.toList()
                        paths.clear()
                        paths.addAll(newList)
                        RsyncSocket.rsyncItem(
                            NoteOperation.CleanElement(
                                id = System.currentTimeMillis().toString()
                            )
                        )
                    }
                }, onColorChanged = {
                    optionConfig = optionConfig.copy(color = it)
                }, onWidthChanged = {
                    optionConfig = optionConfig.copy(width = it)
                })
        }
    }
}