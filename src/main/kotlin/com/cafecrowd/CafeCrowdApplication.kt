package com.cafecrowd

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class CafeCrowdApplication

fun main(args: Array<String>) {
    runApplication<CafeCrowdApplication>(*args)
}