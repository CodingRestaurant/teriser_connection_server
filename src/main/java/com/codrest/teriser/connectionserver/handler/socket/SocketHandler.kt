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
import java.nio.channels.ClosedChannelException
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets

class SocketHandler(private val serverToken: String) {

    private val isServiceActive = AtomicBoolean()

    private lateinit var serverSocketChannel: ServerSocketChannel

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
        activateConnectionServer()
    }

    private fun activateConnectionServer(){
        try {
            serverSocketChannel = ServerSocketChannel.open()

            println("Server is online")
            serverSocketChannel.bind(InetSocketAddress(25565))
            while (isServiceActive.get()) {
                try {
                    val channel = serverSocketChannel.accept()
                    val root = extractFirstJsonFromClient(channel)
                    val token = root["Token"].toString().replace("\"", "")

                    //check connection is already established.
                    if(checkExistConnection(token)){
                        refuseConnection(token,channel)
                        continue
                    }

                    val tokenObject = JsonObject()
                    tokenObject.addProperty("Token", token)
                    tokenObject.addProperty("ServerToken", serverToken)

                    HttpClient.create()
                        .headers { headers: HttpHeaders ->
                            headers[HttpHeaderNames.CONTENT_TYPE] = HttpHeaderValues.APPLICATION_JSON
                        }
                        .post()
                        .uri(TeriserConnectionServer.serverEndpoint)
                        .send(ByteBufFlux.fromString(Mono.just(tokenObject.toString())))
                        .responseContent().aggregate().asString().subscribe { establishEndpoints(it,channel,root,token)}


                }catch (e:Exception){
                    e.printStackTrace()
                }
            }

            //cleanup
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun extractFirstJsonFromClient(channel:SocketChannel):JsonObject{
        val sizeBuffer = ByteBuffer.allocate(Integer.SIZE / java.lang.Byte.SIZE)
        channel.read(sizeBuffer)
        sizeBuffer.flip()
        val sz = sizeBuffer.int

        val dataBuffer = ByteBuffer.allocate(sz)
        channel.read(dataBuffer)
        dataBuffer.flip()

        val data = StandardCharsets.UTF_8.decode(dataBuffer).toString()
        return JsonParser.parseString(data).asJsonObject
    }

    private fun establishEndpoints(projectObject:String, channel:SocketChannel, root:JsonObject, token:String){

        val projectName =
            JsonParser.parseString(projectObject).asJsonObject["response"].asString.replace("\"", "")
        tokens[token] = projectName
        registerEndpoint(serverToken, projectName, root)
        channels[projectName] = channel
    }

    private fun checkExistConnection(token:String) : Boolean{
        val alreadyConnectedChannel = tokens[token]
        if(alreadyConnectedChannel != null){
            removeEndpoints(alreadyConnectedChannel, serverToken)

            //expire pre-connection
            val prechannel = channels[alreadyConnectedChannel]!!
            val errorMessage2 = ByteBuffer.wrap("E02 New Connection detected with token:${token}".toByteArray())
            val errorData2 = ByteBuffer.allocate(Int.SIZE_BYTES + errorMessage2.capacity())
            errorData2.putInt(errorMessage2.capacity())
            errorData2.put(errorMessage2)
            errorData2.flip()
            try {
                prechannel.write(errorData2)
                prechannel.shutdownInput()
                prechannel.shutdownOutput()
                prechannel.finishConnect()
                prechannel.close()
            } catch (e:ClosedChannelException){
                //already socket close : user closed(Handled), continue connection establish
                return false;
            } catch (e1:IOException){
                //already socket close : user closed(UnHandled), continue connection establish
                return false;
            }

            tokens.remove(token)
            return true
        }
        return false
    }

    private fun refuseConnection(token: String, currentChannel: SocketChannel){
        val errorMessage1 = ByteBuffer.wrap("E01 Already have connection with token:${token}".toByteArray())
        val errorData1 = ByteBuffer.allocate(Int.SIZE_BYTES + errorMessage1.capacity())
        errorData1.putInt(errorMessage1.capacity())
        errorData1.put(errorMessage1)
        errorData1.flip()
        currentChannel.write(errorData1)
        currentChannel.shutdownInput()
        currentChannel.shutdownOutput()
        currentChannel.finishConnect()
        currentChannel.close()
    }

}