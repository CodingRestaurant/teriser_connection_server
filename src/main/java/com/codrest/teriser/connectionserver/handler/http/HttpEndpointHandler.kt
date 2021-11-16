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
                println(root)
                println(root.size())

                val apis = root["apis"].asJsonArray
                val types = root["types"].asJsonArray

                println(apis)
                println(types)
                listofcommand += "Available Apis"
                for (api in apis) {
                    val apiInfo = api.asJsonObject
                    println(api)
                    val apiName = apiInfo.keySet().iterator().next()
                    val apiParameters = apiInfo[apiName].asJsonObject
                    registerGetEndpoint(
                        "/$projectName/${apiName}",
                        apiParameters.toString()
                    )
                    registerPostEndpoint(projectName, apiName,
                        apiParameters.getAsJsonArray("parameters"))
                    listofcommand += "\n$apiName"
                }

                listofcommand += "\n"
                listofcommand += "\n"
                listofcommand += "DataTypes"

                for (type in types) {
                    listofcommand += "\n$type"
                }

                registerGetEndpoint("/$projectName",listofcommand)

                routeMap[projectName] = commandList
            }
        }

        private fun registerGetEndpoint(path:String, targetResponse:String){
            println("registered get endpoint : {$path}")
            Spark.get(path) { _, response ->
                response.type("application/json")
                response.status(200)
                targetResponse
            }
        }

        private fun registerPostEndpoint(projectName: String, apiName:String, reservedParameters:JsonArray){
            println("registered Post endpoint : {/$projectName/${apiName}}")
            Spark.post("/$projectName/${apiName}") { request, response ->
                val requestMessage = JsonParser.parseString(request.body().toString()).asJsonObject
                val requestParams = requestMessage["Parameters"].asJsonArray
                val parameters = JsonArray()
                println("Post Request : $requestParams")
                println(request.ip())
                for (i in 0 until reservedParameters.size()){
                    //ith request param
                    parameters.add(requestParams.get(i))
                }

                val executor = JsonObject()
                executor.addProperty("method",apiName)
                executor.add("parameters",parameters)

                response.type("application/json")
                //Concurrent problem? need check
                val res:String? = SocketHandler.executeMethod(projectName,executor.toString())
                if(res != null){
                    response.status(200)
                    res
                }else{
                    removeEndpoints(projectName)
                    Spark.halt(404)
                }
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
                .uri("${TeriserConnectionServer.serverAddress}?projectName")
                .response().subscribe()
        }

    }


}