package com.cafecrowd.place

import com.cafecrowd.common.UpstreamException
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

data class NaverLocalItem(
    val title: String = "",
    val category: String = "",
    val address: String = "",
    val roadAddress: String = "",
    val telephone: String = "",
    val mapx: String = "0",
    val mapy: String = "0",
)

data class NaverLocalResponse(
    val items: List<NaverLocalItem> = emptyList(),
)

@Configuration
class NaverSearchClientConfig(
    private val properties: NaverSearchProperties,
) {

    @Bean
    fun naverSearchRestClient(): RestClient =
        RestClient.builder()
            .baseUrl(properties.baseUrl)
            .defaultHeader("X-Naver-Client-Id", properties.clientId)
            .defaultHeader("X-Naver-Client-Secret", properties.clientSecret)
            .build()
}

@Component
class NaverSearchClient(
    private val naverSearchRestClient: RestClient,
    private val metrics: NaverMetrics,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun searchLocal(query: String, display: Int): NaverLocalResponse {
        return metrics.latency.recordCallable {
            try {
                naverSearchRestClient.get()
                    .uri { it.path("/v1/search/local.json").queryParam("query", query).queryParam("display", display).build() }
                    .retrieve()
                    .body(NaverLocalResponse::class.java) ?: NaverLocalResponse()
            } catch (ex: RestClientException) {
                metrics.failures.increment()
                log.warn("Naver search failed: query={}", query, ex)
                throw UpstreamException()
            }
        }!!
    }
}
