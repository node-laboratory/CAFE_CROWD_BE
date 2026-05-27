package com.cafecrowd.place

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "naver.search")
data class NaverSearchProperties(
    val baseUrl: String = "https://openapi.naver.com",
    val clientId: String = "",
    val clientSecret: String = "",
)
