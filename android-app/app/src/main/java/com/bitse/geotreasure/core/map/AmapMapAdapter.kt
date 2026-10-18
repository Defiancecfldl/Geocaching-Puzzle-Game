package com.bitse.geotreasure.core.map

import android.graphics.Color
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.Circle
import com.amap.api.maps.model.CircleOptions
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.bitse.geotreasure.core.model.GeoPoint
import com.bitse.geotreasure.core.model.Landmark
import com.bitse.geotreasure.core.model.LocationSample
import com.bitse.geotreasure.core.model.MapEvent
import com.bitse.geotreasure.core.model.MapRenderState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class AmapMapAdapter(
    private val map: AMap,
    private val locationSource: AmapRenderLocationSource,
) : MapAdapter {
    private val events = MutableSharedFlow<MapEvent>(extraBufferCapacity = 8)
    private val landmarkOverlays = mutableMapOf<String, LandmarkOverlay>()
    private val normalLandmarkIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
    private val selectedLandmarkIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
    private var cameraInitialized = false
    private var renderedLocation: LocationSample? = null

    init {
        map.setLocationSource(locationSource)
        map.myLocationStyle = MyLocationStyle()
            .myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
            .radiusFillColor(Color.TRANSPARENT)
            .strokeColor(Color.TRANSPARENT)
            .strokeWidth(0f)
            .interval(LOCATION_INTERVAL_MILLIS)
            .showMyLocation(true)
        map.isMyLocationEnabled = true
        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isCompassEnabled = true
        map.setOnMarkerClickListener { marker ->
            val landmarkId = marker.`object` as? String ?: return@setOnMarkerClickListener false
            focusOn(GeoPoint(marker.position.latitude, marker.position.longitude))
            events.tryEmit(MapEvent.LandmarkSelected(landmarkId))
            true
        }
        map.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
            override fun onCameraChange(position: com.amap.api.maps.model.CameraPosition?) = Unit

            override fun onCameraChangeFinish(position: com.amap.api.maps.model.CameraPosition?) {
                position?.target?.let { target ->
                    events.tryEmit(MapEvent.CameraMoved(GeoPoint(target.latitude, target.longitude)))
                }
            }
        })
    }

    override fun render(state: MapRenderState) {
        reconcileLandmarks(state.landmarks, state.selectedLandmarkId)
        if (state.userLocation != renderedLocation) {
            locationSource.update(state.userLocation)
            renderedLocation = state.userLocation
        }
        if (!cameraInitialized) {
            val focus = state.userLocation?.point ?: state.landmarks.firstOrNull()?.point
            focus?.let {
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), DEFAULT_ZOOM),
                )
                cameraInitialized = true
            }
        }
    }

    private fun reconcileLandmarks(
        landmarks: List<Landmark>,
        selectedLandmarkId: String?,
    ) {
        val incomingIds = landmarks.mapTo(mutableSetOf()) { it.id }
        landmarkOverlays.keys
            .filterNot(incomingIds::contains)
            .forEach { landmarkId ->
                landmarkOverlays.remove(landmarkId)?.remove()
            }

        landmarks.forEach { landmark ->
            val selected = landmark.id == selectedLandmarkId
            val existing = landmarkOverlays[landmark.id]
            if (existing == null) {
                landmarkOverlays[landmark.id] = createLandmarkOverlay(landmark, selected)
            } else if (existing.landmark != landmark || existing.selected != selected) {
                existing.update(landmark, selected)
            }
        }
    }

    private fun createLandmarkOverlay(
        landmark: Landmark,
        selected: Boolean,
    ): LandmarkOverlay {
        val marker = map.addMarker(
            MarkerOptions()
                .position(LatLng(landmark.point.latitude, landmark.point.longitude))
                .title(landmark.name)
                .icon(iconFor(selected)),
        ).apply {
            `object` = landmark.id
        }
        val circle = map.addCircle(circleOptionsFor(landmark, selected))
        return LandmarkOverlay(marker, circle, landmark, selected)
    }

    private fun circleOptionsFor(landmark: Landmark, selected: Boolean) = CircleOptions()
        .center(LatLng(landmark.point.latitude, landmark.point.longitude))
        .radius(landmark.checkInRadiusMeters)
        .strokeWidth(if (selected) 4f else 2f)
        .strokeColor(if (selected) SELECTED_STROKE_COLOR else DEFAULT_STROKE_COLOR)
        .fillColor(if (selected) SELECTED_FILL_COLOR else DEFAULT_FILL_COLOR)

    private fun iconFor(selected: Boolean) =
        if (selected) selectedLandmarkIcon else normalLandmarkIcon

    private fun LandmarkOverlay.update(landmark: Landmark, selected: Boolean) {
        val position = LatLng(landmark.point.latitude, landmark.point.longitude)
        marker.position = position
        marker.title = landmark.name
        marker.setIcon(iconFor(selected))
        circle.center = position
        circle.radius = landmark.checkInRadiusMeters
        circle.strokeWidth = if (selected) 4f else 2f
        circle.strokeColor = if (selected) SELECTED_STROKE_COLOR else DEFAULT_STROKE_COLOR
        circle.fillColor = if (selected) SELECTED_FILL_COLOR else DEFAULT_FILL_COLOR
        this.landmark = landmark
        this.selected = selected
    }

    override fun focusOn(point: GeoPoint) {
        map.stopAnimation()
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(point.latitude, point.longitude),
                DEFAULT_ZOOM,
            ),
            CAMERA_ANIMATION_MILLIS,
            null,
        )
        cameraInitialized = true
    }

    override fun observeEvents(): Flow<MapEvent> = events.asSharedFlow()

    override fun release() {
        map.isMyLocationEnabled = false
        locationSource.deactivate()
        landmarkOverlays.values.forEach(LandmarkOverlay::remove)
        landmarkOverlays.clear()
        map.setOnMarkerClickListener(null)
        map.setOnCameraChangeListener(null)
    }

    private data class LandmarkOverlay(
        val marker: Marker,
        val circle: Circle,
        var landmark: Landmark,
        var selected: Boolean,
    ) {
        fun remove() {
            marker.remove()
            circle.remove()
        }
    }

    companion object {
        private const val DEFAULT_ZOOM = 17f
        private const val CAMERA_ANIMATION_MILLIS = 220L
        private const val LOCATION_INTERVAL_MILLIS = 3_000L
        private val SELECTED_STROKE_COLOR = 0xFFE07A23.toInt()
        private val DEFAULT_STROKE_COLOR = 0xAA1C7C54.toInt()
        private const val SELECTED_FILL_COLOR = 0x22E07A23
        private const val DEFAULT_FILL_COLOR = 0x161C7C54
    }
}
