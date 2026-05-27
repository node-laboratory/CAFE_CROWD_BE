package com.cafecrowd.cafe

import org.springframework.data.jpa.repository.JpaRepository

interface CafeRepository : JpaRepository<CafeEntity, String>
