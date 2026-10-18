package com.bitse.geotreasure.core.location

import com.bitse.geotreasure.core.model.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

fun distanceMeters(from: GeoPoint, to: GeoPoint): Double {
    val latitudeDelta = Math.toRadians(to.latitude - from.latitude)
    val longitudeDelta = Math.toRadians(to.longitude - from.longitude)
    val fromLatitude = Math.toRadians(from.latitude)
    val toLatitude = Math.toRadians(to.latitude)
    val haversine = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
        cos(fromLatitude) * cos(toLatitude) *
        sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
    val angularDistance = 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
    return EARTH_RADIUS_METERS * angularDistance
}

fun isWithinRadius(
    distanceMeters: Double,
    radiusMeters: Double,
    accuracyMeters: Float,
): Boolean = distanceMeters <= radiusMeters && accuracyMeters <= 50f
