package nl.stokpop.performance.reactive

import io.micrometer.context.ContextRegistry
import org.reactivestreams.FlowAdapters
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import reactor.core.publisher.Flux
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.Flow

class ReactiveDemoStandAlone {
    private val jdkHttpClient: HttpClient = HttpClient.newHttpClient()

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ReactiveDemoStandAlone::class.java)

        private val TRACE_ID: ThreadLocal<String> = ThreadLocal.withInitial { "INIT" }

        init {
            // what fails or what data is missing when this is not enabled?
            Hooks.enableAutomaticContextPropagation()

            ContextRegistry.getInstance()
                .registerThreadLocalAccessor("TRACE_ID", TRACE_ID)
                .registerThreadLocalAccessor("requestId",
                    { MDC.get("requestId") },
                    { r -> MDC.put("requestId", r)},
                    { MDC.remove("requestId") })
                .registerThreadLocalAccessor("traceId",
                    { MDC.get("traceId") },
                    { r -> MDC.put("traceId", r)},
                    { MDC.remove("traceId") })

            log.info("Initialized")
        }
    }

    fun startWithHttpClient() {
        initMdc()

        log.info("Start")

        Mono.fromFuture {
                log.info("[" + TRACE_ID.get() + "] Preparing request")

                // see if TRACE_ID propagates to the http client threads
                jdkHttpClient.sendAsync(
                    HttpRequest.newBuilder()
                        .uri(URI.create("https://httpbin.org/drip"))
                    .GET()
                    .build(),
                    HttpResponse.BodyHandlers.ofPublisher());
            }
            .flatMapMany { r: HttpResponse<Flow.Publisher<MutableList<ByteBuffer>>> ->
                log.info("[" + TRACE_ID.get() + "] " + "Handling response[" + r.statusCode() + "] and reading body")
                FlowAdapters.toPublisher(r.body())
            }
        .collect(ByteBufferToStringCollector())
        .doOnNext { v: String -> log.info("[" + TRACE_ID.get() + "] " + "Response body is $v") }
        .block()
    }

    private fun initMdc() {
        TRACE_ID.set("test-123-567-890")

        MDC.put("requestId", "req-123")
        MDC.put("traceId", TRACE_ID.get())
    }

    fun startSimple() {
        initMdc()

        log.info("Start simple")

        Flux.from( generateCharacters())
            .delayElements(Duration.ofMillis(1))
            .map { e -> e.uppercase() }
            .doOnNext { e -> log.info("[" + TRACE_ID.get() + "] " + " Element is " + e) }
            .blockLast()
    }

    private fun generateCharacters(): Flux<Char> {
        return Flux.generate({ 'a'.code }, { state: Int, sink: SynchronousSink<Char> ->
            val value = state.toChar()
            sink.next(value)
            if (value == 'z') {
                sink.complete()
            }
            state + 1
        })
    }

}

fun main(args: Array<String>) {
    val app = ReactiveDemoStandAlone()
    if (args.isEmpty() || args[0] == "simple") {
        app.startSimple()
    }
    else {
        app.startWithHttpClient()
    }

}
