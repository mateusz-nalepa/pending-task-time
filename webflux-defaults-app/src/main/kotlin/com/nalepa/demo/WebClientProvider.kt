package com.nalepa.demo

import org.springframework.boot.http.client.reactive.ReactorClientHttpConnectorBuilder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import kotlin.jvm.optionals.getOrNull

const val PENDING_REQUEST_TIME = "PENDING_REQUEST_TIME"

data class ContextWithStartTime(
    val index: String,
    val startTime: Long,
)

@Component
class WebClientProvider(
    private val webClientBuilder: WebClient.Builder,
    private val reactorClientHttpConnectorBuilder: ReactorClientHttpConnectorBuilder,
) {

    fun createWebClient(): WebClient {
        return webClientBuilder
            .filter { request, next ->
                next.exchange(request)
                    .contextWrite {
                        // executed on server thread
                        val index = request.headers()[DUMMY_INDEX]?.first()
                        if (index != null) {
                            WebfluxDefaultsAppLogger.log(this, "Index: $index. Start $PENDING_REQUEST_TIME")
                            it.put(PENDING_REQUEST_TIME, ContextWithStartTime(index, System.nanoTime()))
                        } else {
                            it
                        }
                    }
            }
            .clientConnector(
                reactorClientHttpConnectorBuilder
                    .withHttpClientCustomizer { httpClient ->
                        httpClient
                            .doOnRequest { httpClientRequest, _ ->
                                // executed on webClient thread
                                val contextWithStartTime =
                                    httpClientRequest
                                        .currentContextView()
                                        .getOrEmpty<ContextWithStartTime>(PENDING_REQUEST_TIME)
                                        .getOrNull()
                                        ?: return@doOnRequest

                                val duration = Duration.ofNanos(System.nanoTime() - contextWithStartTime.startTime)
                                WebfluxDefaultsAppLogger.log(
                                    this,
                                    "Index: ${contextWithStartTime.index}. End $PENDING_REQUEST_TIME. Took: $duration"
                                )
                            }
                    }
                    .build()
            )
            .build()
    }

}