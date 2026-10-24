package com.bitse.geotreasure.feature.map

import com.bitse.geotreasure.core.location.distanceMeters
import com.bitse.geotreasure.core.model.GeoPoint
import com.bitse.geotreasure.core.model.LocationSample

internal fun updatedExploredCenters(centers: List<GeoPoint>, location: LocationSample): List<GeoPoint> {
    if (!location.accuracyMeters.isFinite() || location.accuracyMeters > MAX_EXPLORATION_ACCURACY_METERS) {
        return centers
    }
    if (centers.any { distanceMeters(it, location.point) < EXPLORATION_RECORD_DISTANCE_METERS }) return centers
    return centers + location.point
}

private const val MAX_EXPLORATION_ACCURACY_METERS = 50f
private const val EXPLORATION_RECORD_DISTANCE_METERS = 60.0
