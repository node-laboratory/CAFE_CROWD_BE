package com.cafecrowd.admin

import com.cafecrowd.common.ErrorResponse
import com.cafecrowd.common.UnauthorizedException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper

@Component
class AdminAuthFilter(
    @Value("\${admin.bearer-token:}") private val expectedToken: String,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.requestURI.startsWith("/api/admin/")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        val token = header?.removePrefix("Bearer ")?.trim()
        if (expectedToken.isBlank() || token != expectedToken) {
            writeUnauthorized(response)
            return
        }
        chain.doFilter(request, response)
    }

    private fun writeUnauthorized(response: HttpServletResponse) {
        val ex = UnauthorizedException()
        response.status = ex.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(
            objectMapper.writeValueAsString(
                ErrorResponse(code = ex.code, message = ex.message ?: "")
            )
        )
    }
}
