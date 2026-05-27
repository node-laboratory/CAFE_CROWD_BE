package com.cafecrowd.admin

import com.cafecrowd.cafe.CafeDto
import com.cafecrowd.cafe.CafeService
import com.cafecrowd.common.GlobalExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminCafeController::class)
@Import(GlobalExceptionHandler::class, AdminAuthFilter::class)
@TestPropertySource(properties = ["admin.bearer-token=secret-token"])
class AdminCafeControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var service: CafeService

    private fun cafe(available: Boolean) = CafeDto(
        id = "cafe-1", name = "국민카페", available = available,
        lat = 37.5, lng = 127.0, intro = "", address = "addr",
        thumbnailUrl = "https://x", photos = emptyList(),
        naverPlaceUrl = "https://x",
    )

    @Test
    fun `PATCH availability without token returns 401`() {
        mockMvc.perform(
            patch("/api/admin/cafes/cafe-1/availability")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"available":false}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    @Test
    fun `PATCH availability with valid token updates and returns dto`() {
        `when`(service.updateAvailability("cafe-1", false)).thenReturn(cafe(false))

        mockMvc.perform(
            patch("/api/admin/cafes/cafe-1/availability")
                .header("Authorization", "Bearer secret-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"available":false}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("cafe-1"))
            .andExpect(jsonPath("$.available").value(false))
    }
}
