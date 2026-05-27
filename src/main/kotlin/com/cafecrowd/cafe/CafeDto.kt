package com.cafecrowd.cafe

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "카페 정보")
data class CafeDto(
    @field:Schema(example = "cafe-1") val id: String,
    @field:Schema(example = "국민카페") val name: String,
    @field:Schema(description = "true=한산, false=만석", example = "true") val available: Boolean,
    @field:Schema(example = "37.5409") val lat: Double,
    @field:Schema(example = "127.0696") val lng: Double,
    val intro: String,
    @field:Schema(example = "서울 광진구 광장동 111-22") val address: String,
    val thumbnailUrl: String,
    val photos: List<String>,
    val naverPlaceUrl: String,
)

fun CafeEntity.toDto(): CafeDto = CafeDto(
    id = id,
    name = name,
    available = available,
    lat = lat,
    lng = lng,
    intro = intro,
    address = address,
    thumbnailUrl = thumbnailUrl,
    photos = photos,
    naverPlaceUrl = naverPlaceUrl,
)
