/*
 * Author : Kasania
 * Filename : TeriserConnectionServerApplication
 * Desc :
 */
package com.codrest.teriser.connectionserver

import com.codrest.teriser.connectionserver.handler.socket.SocketHandler
import java.nio.file.Files
import java.nio.file.Path


class TeriserConnectionServer {

    companion object {
        val serverEndpoint = "http://teriser.codrest.com/connection/project"
        val credentialPath = Path.of("src/main/resources/CERT/Credential")
    }

    fun start() {
        val credentials = Files.readAllLines(credentialPath)
        SocketHandler(credentials[0])
    }


}




