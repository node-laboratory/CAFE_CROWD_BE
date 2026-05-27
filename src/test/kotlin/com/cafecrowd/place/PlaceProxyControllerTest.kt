package com.cafecrowd.place

import com.cafecrowd.common.GlobalExceptionHandler
import com.cafecrowd.common.UpstreamException
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(PlaceProxyController::class)
@Import(GlobalExceptionHandler::class)
class PlaceProxyControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var service: PlaceService

    @Test
    fun `GET search returns items`() {
        `when`(service.search("스타벅스", 5)).thenReturn(
            PlaceSearchResponse(
                listOf(
                    PlaceItemDto(
                        name = "스타벅스 광장점",
                        category = "카페",
                        address = "서울",
                        roadAddress = "서울 광나루로",
                        phone = "02-1234-5678",
                        lat = 37.5409,
                        lng = 127.0696,
                    )
                )
            )
        )

        mockMvc.perform(get("/api/places/search").param("q", "스타벅스"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].name").value("스타벅스 광장점"))
            .andExpect(jsonPath("$.items[0].lat").value(37.5409))
    }

    @Test
    fun `GET search with empty q returns 400`() {
        mockMvc.perform(get("/api/places/search").param("q", ""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    @Test
    fun `GET search returns 502 on upstream failure`() {
        `when`(service.search("fail", 5)).thenThrow(UpstreamException())

        mockMvc.perform(get("/api/places/search").param("q", "fail"))
            .andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.code").value("UPSTREAM_ERROR"))
    }
}
