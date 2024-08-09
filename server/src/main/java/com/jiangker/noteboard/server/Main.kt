import com.google.gson.Gson
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

object WebSocketChatServer {
    private val operationList = mutableListOf<NoteOperation>()
    private val clients: MutableSet<ChannelHandlerContext> = HashSet()

    private val gson =
        Gson().newBuilder().registerTypeAdapter(NoteOperation::class.java, NoteDeserializer())
            .registerTypeAdapter(NoteElement::class.java, ElementDeserializer()).create()


    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val bootstrap = ServerBootstrap()
        bootstrap.group(NioEventLoopGroup(), NioEventLoopGroup())
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                @Throws(Exception::class)
                override fun initChannel(ch: SocketChannel) {
                    val pipeline = ch.pipeline()
                    pipeline.addLast(HttpServerCodec())
                    pipeline.addLast(HttpObjectAggregator(65536))
                    pipeline.addLast(HttpContentCompressor())
                    pipeline.addLast(WebSocketServerProtocolHandler("/ws"))
                    pipeline.addLast(WebSocketFrameAggregator(65536))
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
                    val token = object : TypeToken<List<NoteOperation>>() {}
                    val operations =
                        gson.fromJson<List<NoteOperation>>(msg.text(), token.type).toMutableList()
                    println(msg.text())
                    if (operations.isNotEmpty()) {
                        synchronized(WebSocketFrameHandler::class) {
                            kotlin.runCatching {
                                operations.forEach { opera ->
                                    val index =
                                        operationList.indexOfLast { item -> item.id == opera.id }
                                    when (opera) {
                                        is NoteOperation.AddElement -> {
                                            if (index == -1) {
                                                operationList.add(opera)
                                            } else {
                                                operationList[index] =
                                                    operationList[index].update(opera)
                                            }
                                        }

                                        is NoteOperation.CleanElement -> {
                                            operationList.removeIf { (it is NoteOperation.AddElement && it.writing).not() }
                                        }

                                        is NoteOperation.RemoveElement -> {
                                            if (index != -1) {
                                                operationList.removeAt(index)
                                            }
                                        }

                                        else -> {}
                                    }
                                }
                            }

                            for (client in clients) {
                                if (client !== ctx) {
                                    client.writeAndFlush(TextWebSocketFrame(msg.text()))
                                }
                            }
                        }
                    } else {
                        ctx.writeAndFlush(TextWebSocketFrame(gson.toJson(operationList)))
                    }
                }
            } else {
                super.channelRead(ctx, msg)
            }
        }

        @Throws(Exception::class)
        override fun handlerAdded(ctx: ChannelHandlerContext) {
            clients.add(ctx)
            println("Client connected: " + ctx.channel().remoteAddress())
        }

        @Throws(Exception::class)
        override fun handlerRemoved(ctx: ChannelHandlerContext) {
            clients.remove(ctx)
            println("Client disconnected: " + ctx.channel().remoteAddress())
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            cause.printStackTrace()
            ctx.close()
        }
    }
}