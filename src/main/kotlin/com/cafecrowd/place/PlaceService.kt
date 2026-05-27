package com.cafecrowd.place

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class PlaceService(
    private val client: NaverSearchClient,
    private val converter: Tm128Converter,
) {

    @Cacheable("places", key = "#query + ':' + #limit")
    fun search(query: String, limit: Int): PlaceSearchResponse {
        val response = client.searchLocal(query, limit)
        val items = response.items.mapNotNull { it.toDto() }
        return PlaceSearchResponse(items)
    }

    private fun NaverLocalItem.toDto(): PlaceItemDto? {
        val (lat, lng) = converter.toWgs84(mapx, mapy) ?: return null
        return PlaceItemDto(
            name = title.stripHtml(),
            category = category,
            address = address,
            roadAddress = roadAddress,
            phone = telephone,
            lat = lat,
            lng = lng,
        )
    }

    private fun String.stripHtml(): String = replace(HTML_TAG_REGEX, "")

    private companion object {
        val HTML_TAG_REGEX = Regex("<[^>]+>")
    }
}
