package com.cafecrowd.common

import org.springframework.http.HttpStatus

abstract class DomainException(
    val code: String,
    val status: HttpStatus,
    message: String,
    val details: Map<String, Any?>? = null,
) : RuntimeException(message)

class CafeNotFoundException(id: String) : DomainException(
    code = "CAFE_NOT_FOUND",
    status = HttpStatus.NOT_FOUND,
    message = "카페를 찾을 수 없습니다.",
    details = mapOf("id" to id),
)

class UnauthorizedException(message: String = "인증이 필요합니다.") : DomainException(
    code = "UNAUTHORIZED",
    status = HttpStatus.UNAUTHORIZED,
    message = message,
)

class BadRequestException(message: String, details: Map<String, Any?>? = null) : DomainException(
    code = "BAD_REQUEST",
    status = HttpStatus.BAD_REQUEST,
    message = message,
    details = details,
)

class UpstreamException(message: String = "외부 API 호출에 실패했습니다.") : DomainException(
    code = "UPSTREAM_ERROR",
    status = HttpStatus.BAD_GATEWAY,
    message = message,
)

class RateLimitException(message: String = "호출 한도를 초과했습니다.") : DomainException(
    code = "RATE_LIMIT",
    status = HttpStatus.TOO_MANY_REQUESTS,
    message = message,
)
