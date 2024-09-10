import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.jiangker.noteboard.common.NoteElement
import com.jiangker.noteboard.common.NoteOperation
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpContentCompressor
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import java.io.File

data class BoardConfig(
    val name: String,
    val user: String,
    val optList: MutableList<NoteOperation>,
    val client: MutableSet<ChannelHandlerContext>
)


object WebSocketChatServer {
    private val boardList = mutableListOf<BoardConfig>()
    private val clientBoard = mutableMapOf<ChannelHandlerContext, BoardConfig>()

    private val gson =
        Gson().newBuilder().registerTypeAdapter(NoteOperation::class.java, NoteDeserializer())
            .registerTypeAdapter(NoteElement::class.java, ElementDeserializer()).create()


    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val file = File("data")
        if (file.isFile) {
            file.delete()
        }
        if (!file.exists()) {
            file.mkdir()
        }
        val bootstrap = ServerBootstrap()
        bootstrap.group(NioEventLoopGroup(), NioEventLoopGroup())
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                @Throws(Exception::class)
                override fun initChannel(ch: SocketChannel) {
                    val pipeline = ch.pipeline()
                    pipeline.addLast(HttpServerCodec())
                    pipeline.addLast(HttpObjectAggregator(16 * 1024 * 1024))
                    pipeline.addLast(HttpContentCompressor())
                    pipeline.addLast(
                        WebSocketServerProtocolHandler(
                            "/ws",
                            null,
                            true,
                            16 * 1024 * 1024
                        )
                    )
                    pipeline.addLast(WebSocketFrameAggregator(16 * 1024 * 1024))
                    pipeline.addLast(WebSocketFrameHandler())
                }
            })

        val future = bootstrap.bind(3000).sync()
        future.channel().closeFuture().sync()
    }

    private class WebSocketFrameHandler : ChannelInboundHandlerAdapter() {
        @Throws(Exception::class)
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            if (msg is WebSocketFrame) {
                if (msg is TextWebSocketFrame) {
                    val element = JsonParser.parseString(msg.text())
                    val jsonObject = element.asJsonObject
                    when (jsonObject.get("type").asInt) {
                        0 -> {
                            val boardConfig = clientBoard[ctx]
                            if (boardConfig != null) {
                                synchronized(boardConfig) {
                                    val frame = gson.toJson(
                                        mapOf(
                                            "type" to 2,
                                            "data" to boardConfig.optList
                                        )
                                    )
                                    println(frame)
                                    ctx.writeAndFlush(TextWebSocketFrame(frame))
                                }
                            }
                        }

                        1 -> {
                            val name = jsonObject.get("name").asString
                            val file = File("data/$name")
                            if (!file.exists()) {
                                file.outputStream().use { output ->
                                    val jsonArray = jsonObject.get("data").asJsonArray.map {
                                        it.asByte
                                    }.toByteArray()
                                    output.write(jsonArray)
                                }
                            }
                            val boardConfig = BoardConfig(
                                name = name,
                                user = jsonObject.get("user").asString,
                                optList = mutableListOf(),
                                client = mutableSetOf(ctx)
                            )
                            println(boardConfig)
                            clientBoard[ctx] = boardConfig
                            boardList.add(boardConfig)
                        }

                        2 -> {
                            val token = object : TypeToken<List<NoteOperation>>() {}
                            val operations = gson.fromJson<List<NoteOperation>>(
                                jsonObject.get("data"),
                                token.type
                            ).toMutableList()
                            clientBoard[ctx]?.let { board ->
                                if (operations.isNotEmpty()) {
                                    synchronized(board) {
                                        kotlin.runCatching {
                                            operations.forEach { opera ->
                                                val index =
                                                    board.optList.indexOfLast { item -> item.id == opera.id }
                                                when (opera) {
                                                    is NoteOperation.AddElement -> {
                                                        if (index == -1) {
                                                            board.optList.add(opera)
                                                        } else {
                                                            board.optList[index] =
                                                                board.optList[index].update(opera)
                                                        }
                                                    }

                                                    is NoteOperation.CleanElement -> {
                                                        board.optList.removeIf { (it is NoteOperation.AddElement && it.writing).not() }
                                                    }

                                                    is NoteOperation.RemoveElement -> {
                                                        if (index != -1) {
                                                            board.optList.removeAt(index)
                                                        }
                                                    }

                                                    else -> {}
                                                }
                                            }
                                        }

                                        val webSocketFrame =
                                            gson.toJson(
                                                mapOf(
                                                    "type" to 2,
                                                    "data" to operations
                                                )
                                            )
                                        println(webSocketFrame)
                                        for (client in board.client) {
                                            if (client !== ctx) {
                                                client.writeAndFlush(
                                                    TextWebSocketFrame(
                                                        webSocketFrame
                                                    )
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    ctx.writeAndFlush(TextWebSocketFrame(gson.toJson(board.optList)))
                                }
                            }

                        }

                        3 -> {
                            ctx.writeAndFlush(
                                TextWebSocketFrame(
                                    gson.toJson(mapOf(
                                        "type" to 3,
                                        "data" to boardList.map {
                                            mapOf(
                                                "name" to it.name,
                                                "user" to it.user
                                            )
                                        }
                                    ))
                                )
                            )
                        }

                        4 -> {
                            val fileName = jsonObject.get("name").asString
                            boardList.firstOrNull { it.name == fileName }?.let { boardConfig ->
                                boardConfig.client.add(ctx)
                                clientBoard[ctx] = boardConfig
                                println("open board: $boardConfig")
                                val webSocketFrame =
                                    gson.toJson(
                                        mapOf(
                                            "type" to 4,
                                            "data" to File("data/$fileName").inputStream()
                                                .readBytes()
                                        )
                                    )
                                ctx.writeAndFlush(TextWebSocketFrame(webSocketFrame))
                            }
                        }

                        9 -> {
                            clientBoard[ctx]?.let {
                                clientBoard.remove(ctx)
                                it.client.remove(ctx)
                            }
                        }
                    }
                }
            } else {
                super.channelRead(ctx, msg)
            }
        }

        @Throws(Exception::class)
        override fun handlerAdded(ctx: ChannelHandlerContext) {
            println("Client connected: " + ctx.channel().remoteAddress())
        }

        @Throws(Exception::class)
        override fun handlerRemoved(ctx: ChannelHandlerContext) {
            clientBoard[ctx]?.client?.remove(ctx)
            println("Client disconnected: " + ctx.channel().remoteAddress())
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            cause.printStackTrace()
            ctx.close()
        }
    }
}