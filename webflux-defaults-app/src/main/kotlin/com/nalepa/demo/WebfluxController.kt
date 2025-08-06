package com.nalepa.demo

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration

data class SomeResponse(
    val text: String,
)

const val DUMMY_INDEX = "DUMMY_INDEX"

@RestController
class WebfluxController(
    private val webClientProvider: WebClientProvider,
) {

    private val webClient = webClientProvider.createWebClient()
    private val customParallelScheduler = Schedulers.newParallel("customParallelScheduler")

    @GetMapping("/endpoint/{index}/{usePublishOn}/{mockDelaySeconds}/{cpuOperationDelaySeconds}")
    fun endpoint(
        @PathVariable index: String,
        @PathVariable usePublishOn: Boolean,
        @PathVariable mockDelaySeconds: Long,
        @PathVariable cpuOperationDelaySeconds: Long,
    ): Mono<ResponseEntity<SomeResponse>> {
        val startTime = System.nanoTime()
        return getData(index, usePublishOn, mockDelaySeconds)
            .doOnNext {
                someHeavyCpuOperation(cpuOperationDelaySeconds)
            }
            .map { ResponseEntity.ok(it) }
            .doFinally {
                val duration = Duration.ofNanos(System.nanoTime() - startTime)
                WebfluxDefaultsAppLogger.log(this, "Index: $index. Got response from endpoint after: $duration")
            }
    }

    private fun getData(index: String, usePublishOn: Boolean, mockDelaySeconds: Long): Mono<SomeResponse> {
        val startTime = System.nanoTime()

        return webClient
            .get()
            .uri("http://localhost:8083/mock/$index/$mockDelaySeconds")
            .header(DUMMY_INDEX, index)
            .retrieve()
            .bodyToMono(SomeResponse::class.java)
            .doOnNext {
                val duration = Duration.ofNanos(System.nanoTime() - startTime)
                WebfluxDefaultsAppLogger.log(this, "Index: $index. Got response from webClient after: $duration")
            }
            .let {
                if (usePublishOn) {
                    it.publishOn(customParallelScheduler)
                } else {
                    it
                }
            }
    }

    private fun someHeavyCpuOperation(cpuOperationDelaySeconds: Long) {
        // Virtual Threads are not used, so sleep will actually make thread busy
        Thread.sleep(Duration.ofSeconds(cpuOperationDelaySeconds))
    }

}