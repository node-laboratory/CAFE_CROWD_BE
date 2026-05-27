package com.cafecrowd.place

import com.cafecrowd.common.BadRequestException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Places", description = "네이버 장소 검색 프록시")
@RestController
@RequestMapping("/api/places")
class PlaceProxyController(
    private val service: PlaceService,
) {

    @Operation(summary = "장소 검색")
    @GetMapping("/search")
    fun search(
        @RequestParam q: String,
        @RequestParam(required = false, defaultValue = "5") limit: Int,
    ): PlaceSearchResponse {
        if (q.isBlank()) throw BadRequestException("검색어가 비어있습니다.", mapOf("q" to q))
        val sanitized = limit.coerceIn(1, 10)
        return service.search(q.trim(), sanitized)
    }
}
