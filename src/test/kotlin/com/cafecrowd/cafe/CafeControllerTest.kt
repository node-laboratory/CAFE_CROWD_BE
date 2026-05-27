package com.cafecrowd.cafe

import com.cafecrowd.common.CafeNotFoundException
import com.cafecrowd.common.GlobalExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(CafeController::class)
@Import(GlobalExceptionHandler::class)
class CafeControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var service: CafeService

    @Test
    fun `GET cafes returns list`() {
        `when`(service.findAll()).thenReturn(
            listOf(
                CafeDto(
                    id = "cafe-1", name = "국민카페", available = true,
                    lat = 37.5409, lng = 127.0696, intro = "intro",
                    address = "서울 광진구", thumbnailUrl = "https://x/thumb.webp",
                    photos = listOf("https://x/1.webp"),
                    naverPlaceUrl = "https://map.naver.com/v5/search/x",
                )
            )
        )

        mockMvc.perform(get("/api/cafes"))
            .andExpect(status().isOk)
            .andExpect(header().string("Cache-Control", "max-age=60, public"))
            .andExpect(jsonPath("$[0].id").value("cafe-1"))
            .andExpect(jsonPath("$[0].available").value(true))
            .andExpect(jsonPath("$[0].photos").isArray)
    }

    @Test
    fun `GET cafe by id returns 404 with error code`() {
        `when`(service.findById("missing")).thenThrow(CafeNotFoundException("missing"))

        mockMvc.perform(get("/api/cafes/missing"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("CAFE_NOT_FOUND"))
            .andExpect(jsonPath("$.details.id").value("missing"))
    }
}
