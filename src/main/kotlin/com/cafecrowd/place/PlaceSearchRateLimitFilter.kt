package com.cafecrowd.place

import com.cafecrowd.common.ErrorResponse
import com.cafecrowd.common.RateLimitException
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Component
class PlaceSearchRateLimitFilter(
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    private val buckets: Cache<String, Bucket> = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(10))
        .maximumSize(100_000)
        .build()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI != "/api/places/search"

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val key = clientKey(request)
        val bucket = buckets.get(key) { newBucket() }!!
        if (!bucket.tryConsume(1)) {
            writeRateLimit(response)
            return
        }
        chain.doFilter(request, response)
    }

    private fun clientKey(request: HttpServletRequest): String =
        request.getHeader("X-Forwarded-For")?.substringBefore(',')?.trim()?.takeIf { it.isNotBlank() }
            ?: request.remoteAddr

    private fun newBucket(): Bucket =
        Bucket.builder()
            .addLimit(
                Bandwidth.builder()
                    .capacity(LIMIT)
                    .refillIntervally(LIMIT, Duration.ofMinutes(1))
                    .build()
            )
            .build()

    private fun writeRateLimit(response: HttpServletResponse) {
        val ex = RateLimitException()
        response.status = ex.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(
            objectMapper.writeValueAsString(
                ErrorResponse(code = ex.code, message = ex.message ?: "")
            )
        )
    }

    private companion object {
        const val LIMIT = 30L
    }
}
