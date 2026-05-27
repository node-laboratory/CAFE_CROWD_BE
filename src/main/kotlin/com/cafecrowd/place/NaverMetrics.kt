package com.cafecrowd.place

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component

@Component
class NaverMetrics(registry: MeterRegistry) {

    val failures: Counter = Counter.builder("naver.search.failures")
        .description("Naver Local Search upstream failure count")
        .register(registry)

    val latency: Timer = Timer.builder("naver.search.latency")
        .description("Naver Local Search upstream latency")
        .register(registry)
}
