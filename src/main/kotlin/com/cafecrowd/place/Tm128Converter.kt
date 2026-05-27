package com.cafecrowd.place

import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate
import org.springframework.stereotype.Component

/**
 * Naver Local Search API returns mapx/mapy in Naver's TM128 (Bessel ellipsoid, lat0=38, lon0=128).
 * Values are integer strings encoded as coord*10^7 in newer responses, raw meters in older ones.
 */
@Component
class Tm128Converter {

    private val crsFactory = CRSFactory()
    private val transformFactory = CoordinateTransformFactory()
    private val tm128 = crsFactory.createFromParameters(
        "naver-tm128",
        "+proj=tmerc +lat_0=38 +lon_0=128 +k=0.9999 +x_0=400000 +y_0=600000 " +
            "+ellps=bessel +units=m +no_defs " +
            "+towgs84=-145.907,505.034,685.756,-1.162,2.347,1.592,6.342"
    )
    private val wgs84 = crsFactory.createFromParameters(
        "wgs84",
        "+proj=longlat +datum=WGS84 +no_defs"
    )
    private val transform = transformFactory.createTransform(tm128, wgs84)

    /**
     * @return (lat, lng) — null if input is invalid
     */
    fun toWgs84(mapx: String, mapy: String): Pair<Double, Double>? {
        val x = mapx.toDoubleOrNull() ?: return null
        val y = mapy.toDoubleOrNull() ?: return null
        // Naver responses scale by 1e7 when value looks like a longitude*1e7 (e.g., 1270696000).
        val (sx, sy) = if (x > 1_000_000_000 || y > 1_000_000_000) x / 1e7 to y / 1e7 else x to y
        val out = ProjCoordinate()
        transform.transform(ProjCoordinate(sx, sy), out)
        return out.y to out.x
    }
}
