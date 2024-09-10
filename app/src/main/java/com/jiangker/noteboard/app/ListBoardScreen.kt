package com.jiangker.noteboard.app

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ListBoardScreen(navController: NavHostController) {

    val boards by RsyncSocket.activeBoard.collectAsState()

    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        items(boards.size) {
            Button(onClick = {
                val file = File(context.cacheDir, boards[it].second)
                file.delete()
                coroutineScope.launch {
                    val openBoard = RsyncSocket.openBoard(file)
                    if (openBoard) {
                        navController.navigate("board?path=${Uri.encode(file.path)}")
                    }
                }
            }, modifier = Modifier.padding(vertical = 10.dp)) {
                Column {
                    Text(text = boards[it].first)
                    Text(text = boards[it].second)
                }
            }
        }
    }

}