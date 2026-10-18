package com.bitse.geotreasure.feature.map

import com.bitse.geotreasure.core.model.GeoPoint
import com.bitse.geotreasure.core.model.Landmark
import com.bitse.geotreasure.core.model.LocationSample
import com.bitse.geotreasure.core.model.MapRenderState

enum class LocationPermissionState {
    UNKNOWN,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
}

data class MapFocusRequest(
    val sequence: Int,
    val point: GeoPoint,
)

data class MapUiState(
    val isLoading: Boolean = true,
    val landmarks: List<Landmark> = emptyList(),
    val selectedLandmarkId: String? = null,
    val userLocation: LocationSample? = null,
    val distanceToSelectedMeters: Double? = null,
    val privacyAccepted: Boolean = false,
    val permissionState: LocationPermissionState = LocationPermissionState.UNKNOWN,
    val permissionRequestCount: Int = 0,
    val usingDemoMap: Boolean = true,
    val message: String? = null,
) {
    val selectedLandmark: Landmark?
        get() = landmarks.firstOrNull { it.id == selectedLandmarkId }

    val renderState: MapRenderState
        get() = MapRenderState(
            userLocation = userLocation,
            landmarks = landmarks,
            selectedLandmarkId = selectedLandmarkId,
        )
}

sealed interface MapAction {
    data object AcceptPrivacy : MapAction
    data object RequestLocationPermission : MapAction
    data object OpenAppSettings : MapAction
    data object Retry : MapAction
    data class PermissionSnapshot(val granted: Boolean) : MapAction
    data class PermissionResult(val granted: Boolean, val canAskAgain: Boolean) : MapAction
    data class SelectLandmark(val landmarkId: String) : MapAction
    data class OpenLandmark(val landmarkId: String) : MapAction
}

sealed interface MapEffect {
    data object RequestLocationPermission : MapEffect
    data object OpenAppSettings : MapEffect
    data class NavigateToLandmark(val landmarkId: String) : MapEffect
    data class ShowMessage(val message: String) : MapEffect
}
