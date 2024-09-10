package com.jiangker.noteboard.app

import ElementDeserializer
import NoteDeserializer
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.jiangker.noteboard.common.NoteElement
import com.jiangker.noteboard.common.NoteOperation
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

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

    val activeBoard = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    private var openCoroutine: CancellableContinuation<ByteArray>? = null

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
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                println(text)
                val element = JsonParser.parseString(text)
                val jsonObject = element.asJsonObject
                when (jsonObject.get("type").asInt) {
                    2 -> {
                        val token = object : TypeToken<List<NoteOperation>>() {}
                        val operations =
                            gson.fromJson<List<NoteOperation>>(jsonObject.get("data"), token.type)
                        rsyncScope.launch {
                            sharedFlow.emit(operations)
                        }
                    }

                    3 -> {
                        val token = object : TypeToken<List<Map<String, String>>>() {}
                        val operations =
                            gson.fromJson<List<Map<String, String>>>(
                                jsonObject.get("data"),
                                token.type
                            )
                        rsyncScope.launch {
                            activeBoard.emit(operations.map { it["user"].orEmpty() to it["name"].orEmpty() })
                        }
                    }

                    4 -> {
                        val fileData =
                            jsonObject.get("data").asJsonArray.map { it.asByte }.toByteArray()
                        openCoroutine?.resume(fileData)
                    }
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
                val message = gson.toJson(
                    mapOf(
                        "data" to cacheOpt,
                        "user" to App.androidId,
                        "type" to 2,
                    )
                )
                if (newWebSocket?.send(message) == true) {
                    cacheOpt.clear()
                }
            }
            sending = false
        }
    }

    fun createBoard(file: File) {
        FileInputStream(file).use {
            newWebSocket?.send(
                Gson().toJson(
                    mapOf(
                        "data" to it.readBytes(),
                        "type" to 1,
                        "name" to file.name,
                        "user" to App.androidId
                    )
                )
            )
        }
    }

    fun initBoard() {
        rsyncScope.launch {
            newWebSocket?.send(
                Gson().toJson(
                    mapOf(
                        "type" to 0,
                        "user" to App.androidId
                    )
                )
            )
        }
    }

    suspend fun openBoard(file: File): Boolean {
        val data = suspendCancellableCoroutine<ByteArray> {
            openCoroutine = it
            newWebSocket?.send(
                Gson().toJson(
                    mapOf(
                        "type" to 4,
                        "name" to file.name,
                    )
                )
            )
        }
        return if (data.isNotEmpty()) {
            file.outputStream().use {
                it.write(data)
            }
            true
        } else {
            false
        }
    }

    fun queryBoards() {
        newWebSocket?.send(
            Gson().toJson(
                mapOf(
                    "type" to 3,
                    "user" to App.androidId
                )
            )
        )
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

    fun quitBoard() {
        newWebSocket?.send(
            Gson().toJson(
                mapOf(
                    "type" to 9,
                    "user" to App.androidId
                )
            )
        )
    }


}