/*
 * Author : Kasania
 * Filename : TeriserConnectionServerApplication
 * Desc :
 */
package com.codrest.teriser.connectionserver

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.netty.ByteBufFlux
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.TcpServer
import spark.kotlin.Http
import spark.kotlin.ignite
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*


class TeriserConnectionServer{

    val serverAddress = "http://teriser.codrest.com/connection/project"
    val http: Http = ignite()
    fun start() {
        test2()
    }

    fun test2(){
        val cert = File("../resources/CERT/cert.crt")
        val key = File("../resources/CERT/private.key")

//        val tcpSslContextSpec = TcpSslContextSpec.forServer(cert, key)

        val server = TcpServer.create()
            .port(25565)
//            .secure { spec: SslContextSpec -> spec.sslContext(tcpSslContextSpec) }
            .handle(this::handler)
            .bindNow()

        server.onDispose()
            .block()
    }

    fun handler(inbound:NettyInbound, outbound: NettyOutbound) : Publisher<Void>{

        return inbound.receive().flatMap { bytes ->

            val root = JsonParser.parseString(bytes.toString(StandardCharsets.UTF_8)).asJsonObject

            val tokenObject = JsonObject()
            tokenObject.addProperty("Token",root["Token"].toString().replace("\"",""))

            val httpClient = HttpClient.create()

            httpClient
                .headers { headers -> headers.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON) }
                .post()
                .uri(serverAddress)
                .send(ByteBufFlux.fromString(Mono.just(tokenObject.toString())))
                .responseContent().aggregate().asString().subscribe { projectObject ->
                    run {

                        val projectName = JsonParser.parseString(projectObject).asJsonObject["response"].asString.replace("\"","")
                        if(Objects.nonNull(projectName)){
                            root.remove("Token")
                            var listofcommand = ""
                            for (mutableEntry in root.entrySet()) {
                                http.get("/$projectName/${mutableEntry.key}"){
                                    "Status $mutableEntry"
                                }

                                http.post("/$projectName/${mutableEntry.key}"){
                                    "run $mutableEntry"
                                }
                                listofcommand += "<br>{${mutableEntry.key} :${mutableEntry.value} }"
                            }
                            http.get("/$projectName"){
                                print(request.ip())
                                "List of $projectName's command$listofcommand"
                            }
                        }

                    }

                }
            outbound.sendString(Mono.just("OK"))
        }
    }

}




