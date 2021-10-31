/*
 * Author : Kasania
 * Filename : SocketReceiver
 * Desc :
 */
package com.codrest.teriser.connectionserver.handler.socket

import com.codrest.teriser.connectionserver.TeriserConnectionServer
import com.codrest.teriser.connectionserver.handler.http.HttpEndpointHandler.Companion.registerEndpoint
import com.codrest.teriser.connectionserver.handler.http.HttpEndpointHandler.Companion.removeEndpoints
import java.util.HashMap
import java.util.concurrent.atomic.AtomicBoolean
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import reactor.netty.ByteBufFlux
import reactor.core.publisher.Mono
import io.netty.handler.codec.http.HttpHeaders
import reactor.netty.http.client.HttpClient
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets

class SocketHandler() {


    private val isServiceActive = AtomicBoolean()

    companion object{
        private val tokens: MutableMap<String, String> = HashMap()
        private val channels: MutableMap<String, SocketChannel> = HashMap()

        fun executeMethod(projectName: String, data:String): String?{

            val channel = channels[projectName] ?: return null
            if(!channel.isConnected){
                channels.remove(projectName)
                return null
            }

            val sz = data.length
            val dataBytes = data.toByteArray()
            val writeDataBuffer = ByteBuffer.allocate(Int.SIZE_BYTES +dataBytes.size)
            writeDataBuffer.putInt(sz)
            writeDataBuffer.put(dataBytes)
            writeDataBuffer.flip()

            channel.write(writeDataBuffer)


            val readSizeBuffer = ByteBuffer.allocate(Int.SIZE_BYTES)

            channel.read(readSizeBuffer)
            readSizeBuffer.flip()
            val readSize = readSizeBuffer.int

            val readDataBuffer = ByteBuffer.allocate(readSize)
            channel.read(readDataBuffer)
            readDataBuffer.flip()

            return StandardCharsets.UTF_8.decode(readDataBuffer).toString()
        }

    }



    //when connection received, parse data, check token, if token is valid, establish endpoints. when disconnect, remove established endpoints.
    init {
        isServiceActive.set(true)
        val serverSocketChannel: ServerSocketChannel
        try {
            serverSocketChannel = ServerSocketChannel.open()

            println("Server is online")
            serverSocketChannel.bind(InetSocketAddress(25565))
            while (isServiceActive.get()) {
                try {
                    val channel = serverSocketChannel.accept()
                    println("Received connection : ${channel.socket().inetAddress}")
                    val sizeBuffer = ByteBuffer.allocate(Integer.SIZE / java.lang.Byte.SIZE)
                    channel.read(sizeBuffer)
                    sizeBuffer.flip()
                    val sz = sizeBuffer.int

                    val dataBuffer = ByteBuffer.allocate(sz)
                    channel.read(dataBuffer)
                    dataBuffer.flip()

                    val data = StandardCharsets.UTF_8.decode(dataBuffer).toString()
                    val root = JsonParser.parseString(data).asJsonObject
                    val token = root["Token"].toString().replace("\"", "")

                    //check connection is already established.

                    val preConnection = tokens[token]
                    if(preConnection != null){
                        if(channels[preConnection]!!.socket().inetAddress.isReachable(1000)){
                            //connection refuse
                            val byteBuffer = ByteBuffer.wrap("E01 Already have connection with token:${token}".toByteArray())
                            channel.write(byteBuffer)
                            channel.close()

                            //expire pre-connection
                            val prechannel = channels[preConnection]!!
                            val byteBuffer2 = ByteBuffer.wrap("E02 New Connection detected with token:${token}".toByteArray())

                            prechannel.write(byteBuffer2)
                            prechannel.close()
                            removeEndpoints(preConnection)
                            continue
                        }
                    }

                    val tokenObject = JsonObject()
                    tokenObject.addProperty("Token", token)
                    println("Request Token validation")

                    val httpClient = HttpClient.create()
                    httpClient
                        .headers { headers: HttpHeaders ->
                            headers[HttpHeaderNames.CONTENT_TYPE] = HttpHeaderValues.APPLICATION_JSON
                        }
                        .post()
                        .uri(TeriserConnectionServer.serverAddress)
                        .send(ByteBufFlux.fromString(Mono.just(tokenObject.toString())))
                        .responseContent().aggregate().asString().subscribe { projectObject: String? ->
                            val projectName =
                                JsonParser.parseString(projectObject).asJsonObject["response"].asString.replace("\"", "")
                            tokens[token] = projectName
                            println(root.toString())
                            registerEndpoint(projectName, root)
                            channels[projectName] = channel
                        }


                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }




}