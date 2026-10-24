package com.bitse.geotreasure.feature.map

import com.bitse.geotreasure.core.model.GeoPoint
import com.bitse.geotreasure.core.model.LiangxiangTreasureCatalog
import com.bitse.geotreasure.core.model.LocalOffsetMeters
import com.bitse.geotreasure.core.model.LocationSample
import com.bitse.geotreasure.core.model.PolygonCheckInArea
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapFogRegressionTest {
    @Test
    fun `poor accuracy does not open fog and earlier centers remain`() {
        val first = GeoPoint(39.7366, 116.1666)
        val history = updatedExploredCenters(emptyList(), sample(first, 18f))
        val poorSample = sample(GeoPoint(39.7339, 116.1680), 80f)

        assertEquals(listOf(first), updatedExploredCenters(history, poorSample))
        assertEquals(emptyList<GeoPoint>(), updatedExploredCenters(emptyList(), poorSample))
        assertEquals(history, updatedExploredCenters(history, sample(first, 18f)))
    }

    @Test
    fun `demo projection gives historical centers their own map positions`() {
        val projection = DemoMapProjection(LiangxiangTreasureCatalog.landmarks)
        val first = LiangxiangTreasureCatalog.landmarks.first().point
        val second = LiangxiangTreasureCatalog.landmarks.last().point

        assertNotEquals(projection.position(first), projection.position(second))
        assertTrue(projection.radiusFractions(120.0).xFraction > 0f)
        assertTrue(projection.radiusFractions(120.0).yFraction > 0f)
    }

    @Test
    fun `rectangle radius decreases by ten meters on each adjustment`() {
        val original = PolygonCheckInArea(
            listOf(
                LocalOffsetMeters(-100.0, -65.0),
                LocalOffsetMeters(100.0, -65.0),
                LocalOffsetMeters(100.0, 65.0),
                LocalOffsetMeters(-100.0, 65.0),
            ),
        )
        val first = original.withDebugRadius(original.debugRadiusMeters() - 10.0)
        val second = first.withDebugRadius(first.debugRadiusMeters() - 10.0)

        assertEquals(100.0, original.debugRadiusMeters(), 0.001)
        assertEquals(90.0, first.debugRadiusMeters(), 0.001)
        assertEquals(80.0, second.debugRadiusMeters(), 0.001)
    }

    private fun sample(point: GeoPoint, accuracyMeters: Float) = LocationSample(
        point = point,
        accuracyMeters = accuracyMeters,
        capturedAt = Instant.parse("2026-10-23T00:00:00Z"),
    )
}
