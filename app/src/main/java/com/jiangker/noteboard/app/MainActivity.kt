package com.jiangker.noteboard.app

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jiangker.noteboard.app.ui.theme.NoteboardTheme

class MainActivity : ComponentActivity() {
    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoteboardTheme {
                // A surface container using the 'background' color from the theme

                val navController = rememberNavController()

                Scaffold { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = "main",
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        composable(route = "main") {
                            SelectScreen(navController)
                        }
                        composable(
                            route = "board?path={path}",
                            arguments = listOf(
                                navArgument("path") {
                                    type = NavType.StringType
                                    this.defaultValue = ""
                                }
                            )
                        ) {
                            val path = it.arguments?.getString("path") ?: return@composable
                            BoardScreen(navController, path)
                        }
                        composable(route = "listBoard") {
                            ListBoardScreen(navController)
                        }
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