/*
 * Author : Kasania
 * Filename : HttpEndpointMaker
 * Desc :
 */
package com.codrest.teriser.connectionserver.handler.http

import com.codrest.teriser.connectionserver.TeriserConnectionServer
import com.codrest.teriser.connectionserver.handler.socket.SocketHandler
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpHeaders
import reactor.core.publisher.Mono
import reactor.netty.ByteBufFlux
import reactor.netty.http.client.HttpClient
import spark.Spark
import java.util.*

class HttpEndpointHandler {

    companion object {
        private val routeMap: MutableMap<String, List<String>> = hashMapOf()

        //when establish socket connection
        fun registerEndpoint(projectName:String, root:JsonObject) {

            if(Objects.nonNull(projectName)){
                root.remove("Token")
                var listofcommand = ""
                val commandList:MutableList<String> = mutableListOf()
                for (mutableEntry in root.entrySet()) {
                    println("Establish endpoint /$projectName/${mutableEntry.key}")
                    commandList.add(mutableEntry.key)

                    Spark.get("/$projectName/${mutableEntry.key}") { request, response ->
                        response.type("application/json")
                        response.status(200)
                        mutableEntry.value.toString()
                    }

                    Spark.post("/$projectName/${mutableEntry.key}") { request, response ->
                        val requestMessage = JsonParser.parseString(request.body().toString()).asJsonObject
                        val requestParams = requestMessage["Parameters"].asJsonArray
                        val parameters = JsonArray()
                        println(requestParams)
                        val reservedParameters = mutableEntry.value.asJsonObject["parameters"].asJsonArray

                        for (i in 0 until reservedParameters.size()){
                            //ith request param
                            parameters.add(requestParams.get(i))
                        }

                        val executor = JsonObject()
                        executor.addProperty("method",mutableEntry.key)
                        executor.add("parameters",parameters)

                        response.type("application/json")

                        val res:String? = SocketHandler.executeMethod(projectName,executor.toString())
                        if(res != null){
                            response.status(200)
                            res
                        }else{
                            removeEndpoints(projectName)
                            Spark.halt(404)
                        }
                    }

                    listofcommand += "<br>{${mutableEntry.key} }"
                }
                Spark.get("/$projectName") { request, response ->
                    print(request.ip())
                    "List of $projectName's command$listofcommand"
                }

                routeMap[projectName] = commandList
            }
        }

        //when disconnect socket
        fun removeEndpoints(projectName: String){
            val commandList = routeMap[projectName]
            if (commandList != null) {
                for(element in commandList){
                    Spark.unmap("/$projectName/${element}")
                }
                Spark.unmap("/$projectName")
            }
            routeMap.remove(projectName)

            val httpClient = HttpClient.create()
            httpClient
                .headers { headers: HttpHeaders ->
                    headers[HttpHeaderNames.CONTENT_TYPE] = HttpHeaderValues.APPLICATION_JSON
                }
                .delete()
                .uri(TeriserConnectionServer.serverAddress)
                .send(ByteBufFlux.fromString(Mono.just(projectName)))
                .response().subscribe()
        }

    }


}