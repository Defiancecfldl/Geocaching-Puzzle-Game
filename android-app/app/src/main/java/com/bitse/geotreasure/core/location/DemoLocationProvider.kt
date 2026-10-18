package com.bitse.geotreasure.core.location

import com.bitse.geotreasure.core.common.AppResult
import com.bitse.geotreasure.core.model.GeoPoint
import com.bitse.geotreasure.core.model.LocationSample
import com.bitse.geotreasure.core.model.LocationSourceKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock

class DemoLocationProvider : LocationProvider {
    private val location = MutableStateFlow(createSample())

    override fun observeLocations(): Flow<LocationSample> = location

    override suspend fun getCurrentLocation(): AppResult<LocationSample> =
        AppResult.Success(location.value)

    override fun stopLocationUpdates() = Unit

    private fun createSample() = LocationSample(
        point = GeoPoint(latitude = 39.9594, longitude = 116.3162),
        accuracyMeters = 18f,
        capturedAt = Clock.System.now(),
        source = LocationSourceKind.DEMO,
    )
}
