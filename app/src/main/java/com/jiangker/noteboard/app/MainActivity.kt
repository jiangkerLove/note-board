package com.jiangker.noteboard.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Xml
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.github.barteksc.pdfviewer.PDFView
import com.jiangker.noteboard.NoteBoardScreen
import com.jiangker.noteboard.NoteElement
import com.jiangker.noteboard.NoteOperation
import com.jiangker.noteboard.NoteSource
import com.jiangker.noteboard.PDFScreen
import com.jiangker.noteboard.PathItem
import com.jiangker.noteboard.Pos
import com.jiangker.noteboard.R
import com.jiangker.noteboard.app.ui.theme.NoteboardTheme
import com.jiangker.noteboard.rememberNoteBoardScreenState
import org.xmlpull.v1.XmlPullParser

class MainActivity : ComponentActivity() {
    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoteboardTheme {
                // A surface container using the 'background' color from the theme

                val context = LocalContext.current

                val paths = remember { mutableStateListOf<PathItem>() }
                var currentOpt: NoteOperation? = null

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
                    RsyncSocket.sharedFlow.collect { list ->
                        synchronized(MainActivity::class) {
                            runCatching {
                                val newList = list.toMutableList()
                                val mutableList = paths.toMutableList()
                                for (i in mutableList.indices) {
                                    newList.find { mutableList[i].id == it.id }?.let {
                                        if (it is NoteOperation.AddElement) {
                                            val line = it.element as NoteElement.Line
                                            mutableList[i] = PathItem(
                                                id = it.id,
                                                color = Color(line.color),
                                                width = line.width,
                                                positions = line.poss.map {
                                                    Offset(
                                                        it.x * pdfView.width,
                                                        it.y * pdfView.width
                                                    )
                                                }
                                            )
                                            newList.remove(it)
                                        }
                                    }
                                }
                                if (newList.isNotEmpty()) {
                                    newList.forEach { operation ->
                                        when (operation) {
                                            is NoteOperation.AddElement -> {
                                                val line = (operation.element as NoteElement.Line)
                                                mutableList.add(PathItem(
                                                    id = operation.id,
                                                    color = Color(line.color),
                                                    width = line.width,
                                                    positions = line.poss.map {
                                                        Offset(
                                                            it.x * pdfView.width,
                                                            it.y * pdfView.width
                                                        )
                                                    }
                                                ))
                                            }

                                            is NoteOperation.CleanElement -> {
                                                mutableList.clear()
                                            }

                                            is NoteOperation.RemoveElement -> {
                                                val pathItems =
                                                    mutableList.filter { it.id != operation.id }
                                                        .toList()
                                                mutableList.clear()
                                                mutableList.addAll(pathItems)
                                            }
                                        }

                                    }
                                }
                                paths.clear()
                                paths.addAll(mutableList)
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NoteBoardScreen(
                        onPathDown = { color, fl ->
                            currentOpt = NoteOperation.AddElement(
                                id = System.currentTimeMillis().toString(),
                                element = NoteElement.Line(
                                    color.value,
                                    width = fl,
                                    poss = emptyList(),
                                    true
                                )
                            )
                            currentOpt?.let { RsyncSocket.rsyncItem(it) }
                        },
                        onPathMove = { off ->
                            val element = currentOpt as NoteOperation.AddElement
                            val line =
                                (element.element as NoteElement.Line).poss.toMutableList()

                            line.add(Pos(off.x / pdfView.width, off.y / pdfView.width))
                            currentOpt = element.copy(
                                element = (element.element as NoteElement.Line).copy(poss = line)
                            )
                            currentOpt?.let { RsyncSocket.rsyncItem(it) }
                        },
                        onPathUp = {
                            val element = currentOpt as NoteOperation.AddElement
                            RsyncSocket.rsyncItem(
                                element.copy(
                                    element = (element.element as NoteElement.Line).copy(writing = false)
                                )
                            )
                            synchronized(MainActivity::class) {
                                paths.add(it.copy(id = currentOpt!!.id))
                            }
                        },
                        onClean = {
                            synchronized(MainActivity::class) {
                                paths.clear()
                                RsyncSocket.rsyncItem(
                                    NoteOperation.CleanElement(
                                        id = System.currentTimeMillis().toString()
                                    )
                                )
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
                        paths,
                        noteState = rememberNoteBoardScreenState(pdfView),
                    ) {
                        PDFScreen(
                            modifier = it.fillMaxSize(),
                            pdfView = pdfView,
                            noteSource = NoteSource.File(this@MainActivity.filesDir.path + "/api.pdf")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NoteboardTheme {
        Greeting("Android")
    }
}