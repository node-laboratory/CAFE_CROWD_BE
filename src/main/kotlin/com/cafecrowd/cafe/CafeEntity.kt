package com.cafecrowd.cafe

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(name = "cafes")
class CafeEntity(
    @Id
    @Column(name = "id")
    val id: String,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "available", nullable = false)
    var available: Boolean,

    @Column(name = "lat", nullable = false)
    var lat: Double,

    @Column(name = "lng", nullable = false)
    var lng: Double,

    @Column(name = "intro", nullable = false)
    var intro: String,

    @Column(name = "address", nullable = false)
    var address: String,

    @Column(name = "thumbnail_url", nullable = false)
    var thumbnailUrl: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "photos", nullable = false, columnDefinition = "jsonb")
    var photos: List<String>,

    @Column(name = "naver_place_url", nullable = false)
    var naverPlaceUrl: String,

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    var createdAt: OffsetDateTime? = null,

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    var updatedAt: OffsetDateTime? = null,
)
