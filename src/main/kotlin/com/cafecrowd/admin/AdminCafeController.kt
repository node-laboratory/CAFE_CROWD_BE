package com.cafecrowd.admin

import com.cafecrowd.cafe.CafeDto
import com.cafecrowd.cafe.CafeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin", description = "운영자 도구")
@SecurityRequirement(name = "bearer-auth")
@RestController
@RequestMapping("/api/admin/cafes")
class AdminCafeController(
    private val service: CafeService,
) {

    data class AvailabilityRequest(val available: Boolean)

    @Operation(summary = "카페 혼잡도 갱신")
    @PatchMapping("/{id}/availability")
    fun updateAvailability(
        @PathVariable id: String,
        @RequestBody body: AvailabilityRequest,
    ): CafeDto = service.updateAvailability(id, body.available)
}
