package com.cafecrowd.cafe

import com.cafecrowd.support.PostgresTestContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresTestContainer::class)
class CafeRepositoryIntegrationTest(
    @Autowired private val repository: CafeRepository,
) {

    @Test
    fun `seed data loaded with 5 cafes`() {
        val all = repository.findAll()
        assertThat(all).hasSize(5)
        assertThat(all.map { it.id }).contains("cafe-1", "cafe-2", "cafe-3", "cafe-4", "cafe-5")
    }

    @Test
    fun `photos JSONB column maps to list`() {
        val cafe = repository.findById("cafe-1").orElseThrow()
        assertThat(cafe.photos).isNotEmpty
        assertThat(cafe.photos).allMatch { it.startsWith("https://") }
    }

    @Test
    fun `empty photos array deserializes to empty list`() {
        val cafe = repository.findById("cafe-3").orElseThrow()
        assertThat(cafe.photos).isEmpty()
    }

    @Test
    fun `updating availability flips the boolean`() {
        val cafe = repository.findById("cafe-1").orElseThrow()
        val original = cafe.available
        cafe.available = !original
        repository.saveAndFlush(cafe)

        val reloaded = repository.findById("cafe-1").orElseThrow()
        assertThat(reloaded.available).isEqualTo(!original)
    }
}
