package com.jiangker.noteboard.app

import ElementDeserializer
import NoteDeserializer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jiangker.noteboard.common.NoteElement
import com.jiangker.noteboard.common.NoteOperation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object RsyncSocket {

    private val rsyncScope =
        CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    private val cacheOpt = mutableListOf<NoteOperation>()

    private var newWebSocket: WebSocket? = null

    private var sending = false

    private val gson =
        Gson().newBuilder().registerTypeAdapter(NoteOperation::class.java, NoteDeserializer())
            .registerTypeAdapter(NoteElement::class.java, ElementDeserializer()).create()

    val sharedFlow = MutableSharedFlow<List<NoteOperation>>()

    init {
        connect()
    }

    private fun connect() {
        val httpClient = OkHttpClient.Builder()
            .callTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder().url("ws://33.33.36.199:3000/ws").build()
        newWebSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                t.printStackTrace()
                rsyncScope.launch {
                    delay(5000)
                    connect()
                }
            }

            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                println("open")
                webSocket.send("[]")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                println(text)
                val token = object : TypeToken<List<NoteOperation>>() {}
                val operations = gson.fromJson<List<NoteOperation>>(text, token.type)
                rsyncScope.launch {
                    sharedFlow.emit(operations)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                rsyncScope.launch {
                    delay(5000)
                    connect()
                }
            }
        })
    }

    private fun printItem() {
        rsyncScope.launch {
            if (cacheOpt.isNotEmpty()) {
                val message = gson.toJson(cacheOpt)
                if(newWebSocket?.send(message) == true) {
                    cacheOpt.clear()
                }
            }
            sending = false
        }
    }


    fun rsyncItem(operation: NoteOperation) {
        rsyncScope.launch {
            val index = cacheOpt.indexOfLast { it.id == operation.id }
            if (index != -1) {
                cacheOpt[index] = cacheOpt[index].update(operation)
            } else {
                cacheOpt.add(operation)
            }
            if (sending.not()) {
                sending = true
                printItem()
            }
        }
    }


}