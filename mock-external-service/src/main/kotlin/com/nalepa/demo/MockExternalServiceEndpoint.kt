package com.nalepa.demo

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalTime

data class SomeResponse(
    val text: String,
)


@RestController
class MockExternalServiceEndpoint {

    @GetMapping("/mock/{index}/{delaySeconds}")
    fun endpoint(
        @PathVariable index: String,
        @PathVariable delaySeconds: Long,
    ): Mono<ResponseEntity<SomeResponse>> =
        Mono.just(ResponseEntity.ok(SomeResponse("text")))
            // delay is executed on Schedulers.parallel() by default
            .delayElement(Duration.ofSeconds(delaySeconds))
            .doOnNext {
                MockExternalServiceLogger.log(this, "Index: $index. Response returned after seconds: $delaySeconds")
            }

}

object MockExternalServiceLogger {
    fun log(caller: Any, message: String) {
        println("${LocalTime.now()} : ${caller.javaClass.simpleName} : ${Thread.currentThread().name} ### $message")
    }
}