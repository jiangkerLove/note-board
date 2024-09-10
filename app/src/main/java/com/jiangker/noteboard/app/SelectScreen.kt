package com.jiangker.noteboard.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import java.io.File
import java.util.UUID

@Composable
fun SelectScreen(navController: NavHostController) {

    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.data?.let { uri ->
                    context.contentResolver.openInputStream(uri).use { inputStream ->
                        inputStream?.let {
                            val file = File(context.cacheDir, "${UUID.randomUUID()}.pdf")
                            file.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                            RsyncSocket.createBoard(file = file)
                            navController.navigate("board?path=${Uri.encode(file.path)}")
                        }
                    }
                }
            }
        }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE) // 可打开的文件类别
                        type = "application/pdf" // "application/octet-stream" 二进制文件; "*/*"全部文件
                    }
                    launcher.launch(Intent.createChooser(intent, "选择一个文件"))
                }
            ) {
                Text(
                    text = "创建演示",
                    modifier = Modifier.padding(horizontal = 30.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    RsyncSocket.queryBoards()
                    navController.navigate("listBoard")
                },
            ) {
                Text(
                    text = "加入演示",
                    modifier = Modifier.padding(horizontal = 30.dp, vertical = 5.dp)
                )
            }
        }

    }


}