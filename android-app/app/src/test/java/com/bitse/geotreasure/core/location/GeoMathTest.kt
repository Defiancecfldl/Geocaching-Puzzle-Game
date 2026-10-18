package com.bitse.geotreasure.core.location

import com.bitse.geotreasure.core.model.GeoPoint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {
    @Test
    fun distanceMeters_samePoint_returnsZero() {
        val point = GeoPoint(39.9594, 116.3162)

        assertTrue(distanceMeters(point, point) < 0.01)
    }

    @Test
    fun isWithinRadius_rejectsPoorAccuracy() {
        assertFalse(isWithinRadius(distanceMeters = 10.0, radiusMeters = 30.0, accuracyMeters = 70f))
    }

    @Test
    fun isWithinRadius_acceptsCloseAccurateLocation() {
        assertTrue(isWithinRadius(distanceMeters = 10.0, radiusMeters = 30.0, accuracyMeters = 12f))
    }
}
