/*
 * Author : Kasania
 * Filename : TeriserConnectionServerApplication
 * Desc :
 */
package com.codrest.teriser.connectionserver

import com.codrest.teriser.connectionserver.handler.socket.SocketHandler



class TeriserConnectionServer{

    companion object{
        val serverAddress = "http://teriser.codrest.com/connection/project"
    }
    fun start() {
        SocketHandler()
    }

}




