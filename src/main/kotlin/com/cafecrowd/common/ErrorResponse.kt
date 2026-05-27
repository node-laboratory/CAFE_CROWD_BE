package com.cafecrowd.common

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공통 에러 응답")
data class ErrorResponse(
    @field:Schema(example = "CAFE_NOT_FOUND") val code: String,
    @field:Schema(example = "카페를 찾을 수 없습니다.") val message: String,
    val details: Map<String, Any?>? = null,
)
