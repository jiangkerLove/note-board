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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.github.barteksc.pdfviewer.PDFView
import com.jiangker.noteboard.NoteBoardScreen
import com.jiangker.noteboard.NoteSource
import com.jiangker.noteboard.PDFScreen
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

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NoteBoardScreen(noteState = rememberNoteBoardScreenState(pdfView)) {
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