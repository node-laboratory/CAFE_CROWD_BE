package com.cafecrowd.cafe

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@Tag(name = "Cafes", description = "카페 조회")
@RestController
@RequestMapping("/api/cafes")
class CafeController(
    private val service: CafeService,
) {

    @Operation(summary = "카페 전체 목록")
    @GetMapping
    fun list(
        @RequestParam(required = false) bbox: String?,
    ): ResponseEntity<List<CafeDto>> =
        ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
            .body(service.findAll())

    @Operation(summary = "단일 카페 조회")
    @GetMapping("/{id}")
    fun get(@PathVariable id: String): CafeDto = service.findById(id)
}
