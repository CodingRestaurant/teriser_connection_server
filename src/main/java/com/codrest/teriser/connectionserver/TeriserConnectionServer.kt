/*
 * Author : Kasania
 * Filename : TeriserConnectionServerApplication
 * Desc :
 */
package com.codrest.teriser.connectionserver

import mu.KLogging
import spark.kotlin.Http
import spark.kotlin.ignite

class TeriserConnectionServer{
    companion object : KLogging()

    fun test(){
        val http: Http = ignite()
        for(i in 0..10){
            http.get("/test$i"){
                logger.debug("hello")
                request.body() +"id $i"
            }
        }
    }

}




