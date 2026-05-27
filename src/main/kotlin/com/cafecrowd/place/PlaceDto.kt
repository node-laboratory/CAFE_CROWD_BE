package com.cafecrowd.place

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "장소 검색 결과")
data class PlaceItemDto(
    val name: String,
    val category: String,
    val address: String,
    val roadAddress: String,
    val phone: String,
    val lat: Double,
    val lng: Double,
)

data class PlaceSearchResponse(
    val items: List<PlaceItemDto>,
)
