package com.jiangker.noteboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


data class OptionConfig(
    val color: Color,
    val width: Float,
    val isClean: Boolean,
)

@Composable
fun OptionScreen(
    config: OptionConfig,
    onCleanChanged: (Boolean) -> Unit,
    onColorChanged: (Color) -> Unit,
    onWidthChanged: (Float) -> Unit,
    onClean: () -> Unit,
) {
    var showSelectColor by remember { mutableStateOf(false) }
    var showSelectWith by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(color = Color.Gray)
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.clickable {
                onClean()
            }
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_clean),
                contentDescription = null
            )
            Text(text = "清除", modifier = Modifier, fontSize = 12.sp, color = Color.White)
        }
        Spacer(modifier = Modifier.width(20.dp))

        Column(
            modifier = Modifier.clickable {
                showSelectColor = true
            }
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(color = config.color)
            )

            Text(text = "颜色", modifier = Modifier, fontSize = 12.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.width(20.dp))


        Column(
            modifier = Modifier.clickable {
                showSelectWith = true
            }
        ) {
            Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .size(config.width.dp)
                        .clip(CircleShape)
                        .background(color = config.color)
                )
            }
            Text(text = "画笔大小", modifier = Modifier, fontSize = 12.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.width(20.dp))

        Column(
            modifier = Modifier.clickable {
                onCleanChanged(true)
            }
        ) {
            Image(
                painter = painterResource(id = if (config.isClean) R.drawable.ic_eraser_select else R.drawable.ic_eraser),
                contentDescription = null
            )
            Text(text = "橡皮", modifier = Modifier, fontSize = 12.sp, color = Color.White)
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column(
            modifier = Modifier.clickable {
                onCleanChanged(false)
            }
        ) {
            Image(
                painter = painterResource(id = if (config.isClean) R.drawable.ic_pen else R.drawable.ic_pen_select),
                contentDescription = null
            )
            Text(text = "画笔", modifier = Modifier, fontSize = 12.sp, color = Color.White)
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column(
            modifier = Modifier.clickable {

            }
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_save),
                contentDescription = null
            )
            Text(text = "保存", modifier = Modifier, fontSize = 12.sp, color = Color.White)
        }
    }

    if (showSelectColor) {
        Column(modifier = Modifier
            .fillMaxSize()
            .clickable { showSelectColor = false }) {
            Spacer(modifier = Modifier.height(70.dp))
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .background(Color.Gray)
            ) {
                listOf(Color.Red, Color.Blue, Color.Green).forEach {
                    Box(modifier = Modifier
                        .padding(10.dp)
                        .size(30.dp)
                        .background(it)
                        .clickable {
                            onColorChanged(it)
                            showSelectColor = false
                        })
                }
            }
        }
    }

    if (showSelectWith) {
        Column(modifier = Modifier
            .fillMaxSize()
            .clickable { showSelectWith = false }) {
            Spacer(modifier = Modifier.height(70.dp))
            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .width(500.dp)
                    .background(Color.Gray)
            ) {
                Slider(
                    value = config.width,
                    onValueChange = { onWidthChanged(it) },
                    steps = 11,
                    valueRange = 2f..20f
                )
            }
        }
    }

}