package com.cafecrowd.cafe

import com.cafecrowd.common.CafeNotFoundException
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CafeService(
    private val repository: CafeRepository,
) {
    @Cacheable("cafes")
    fun findAll(): List<CafeDto> =
        repository.findAll().map { it.toDto() }

    fun findById(id: String): CafeDto =
        repository.findById(id).orElseThrow { CafeNotFoundException(id) }.toDto()

    @Transactional
    @CacheEvict(value = ["cafes"], allEntries = true)
    fun updateAvailability(id: String, available: Boolean): CafeDto {
        val cafe = repository.findById(id).orElseThrow { CafeNotFoundException(id) }
        cafe.available = available
        return cafe.toDto()
    }
}
