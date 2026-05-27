package com.cafecrowd

import com.cafecrowd.support.PostgresTestContainer
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(PostgresTestContainer::class)
class CafeCrowdApplicationTests {

    @Test
    fun contextLoads() {
    }
}
