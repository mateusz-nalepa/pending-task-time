package com.nalepa.demo

import java.time.LocalTime

object UndertowAppLogger {
    fun log(caller: Any, message: String) {
        println("${LocalTime.now()} : ${caller.javaClass.simpleName} : ${Thread.currentThread().name} ### $message")
    }
}