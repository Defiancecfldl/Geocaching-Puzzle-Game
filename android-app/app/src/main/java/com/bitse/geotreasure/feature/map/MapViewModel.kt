package com.bitse.geotreasure.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bitse.geotreasure.core.common.AppResult
import com.bitse.geotreasure.core.location.LocationPrivacyManager
import com.bitse.geotreasure.core.location.LocationProvider
import com.bitse.geotreasure.core.location.distanceMeters
import com.bitse.geotreasure.data.repository.LandmarkRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapViewModel(
    private val landmarkRepository: LandmarkRepository,
    private val locationProvider: LocationProvider,
    private val privacyManager: LocationPrivacyManager,
    hasAmapKey: Boolean,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        MapUiState(
            privacyAccepted = privacyManager.isAccepted.value,
            usingDemoMap = !hasAmapKey,
        ),
    )
    val uiState: StateFlow<MapUiState> = mutableUiState.asStateFlow()

    private val effectChannel = Channel<MapEffect>(capacity = Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    private var locationJob: Job? = null

    init {
        loadLandmarks()
    }

    fun onAction(action: MapAction) {
        when (action) {
            MapAction.AcceptPrivacy -> {
                privacyManager.accept()
                mutableUiState.update { it.copy(privacyAccepted = true) }
                startLocationIfReady()
            }

            MapAction.RequestLocationPermission -> {
                mutableUiState.update { it.copy(permissionRequestCount = it.permissionRequestCount + 1) }
                effectChannel.trySend(MapEffect.RequestLocationPermission)
            }

            MapAction.OpenAppSettings -> effectChannel.trySend(MapEffect.OpenAppSettings)
            MapAction.Retry -> {
                loadLandmarks()
                startLocationIfReady()
            }

            is MapAction.PermissionSnapshot -> {
                mutableUiState.update {
                    it.copy(
                        permissionState = if (action.granted) {
                            LocationPermissionState.GRANTED
                        } else {
                            when (it.permissionState) {
                                LocationPermissionState.GRANTED -> LocationPermissionState.DENIED
                                else -> it.permissionState
                            }
                        },
                    )
                }
                if (action.granted) {
                    startLocationIfReady()
                } else {
                    stopLocation()
                }
            }

            is MapAction.PermissionResult -> {
                val requestCount = mutableUiState.value.permissionRequestCount
                val permissionState = when {
                    action.granted -> LocationPermissionState.GRANTED
                    requestCount >= 2 && !action.canAskAgain -> LocationPermissionState.PERMANENTLY_DENIED
                    else -> LocationPermissionState.DENIED
                }
                mutableUiState.update { it.copy(permissionState = permissionState) }
                if (action.granted) {
                    startLocationIfReady()
                } else {
                    stopLocation()
                }
            }

            is MapAction.SelectLandmark -> selectLandmark(action.landmarkId)
            is MapAction.OpenLandmark -> effectChannel.trySend(MapEffect.NavigateToLandmark(action.landmarkId))
        }
    }

    private fun loadLandmarks() {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, message = null) }
            when (val result = landmarkRepository.listLandmarks(center = null, radiusMeters = null)) {
                is AppResult.Success -> mutableUiState.update { state ->
                    val selectedId = state.selectedLandmarkId ?: result.data.firstOrNull()?.id
                    state.copy(
                        isLoading = false,
                        landmarks = result.data,
                        selectedLandmarkId = selectedId,
                    ).withUpdatedDistance()
                }

                is AppResult.Failure -> mutableUiState.update {
                    it.copy(isLoading = false, message = result.error.message)
                }
            }
        }
    }

    private fun selectLandmark(landmarkId: String) {
        mutableUiState.update {
            it.copy(selectedLandmarkId = landmarkId).withUpdatedDistance()
        }
    }

    private fun startLocationIfReady() {
        val state = mutableUiState.value
        if (!state.privacyAccepted || state.permissionState != LocationPermissionState.GRANTED) return
        if (locationJob?.isActive == true) return

        locationJob = viewModelScope.launch {
            locationProvider.observeLocations()
                .catch { throwable ->
                    mutableUiState.update {
                        it.copy(message = throwable.message ?: "定位暂时不可用，请稍后重试。")
                    }
                }
                .collect { location ->
                    mutableUiState.update {
                        it.copy(userLocation = location, message = null).withUpdatedDistance()
                    }
                }
        }
    }

    private fun stopLocation() {
        locationJob?.cancel()
        locationJob = null
        locationProvider.stopLocationUpdates()
        mutableUiState.update { it.copy(userLocation = null, distanceToSelectedMeters = null) }
    }

    private fun MapUiState.withUpdatedDistance(): MapUiState {
        val location = userLocation ?: return copy(distanceToSelectedMeters = null)
        val landmark = selectedLandmark ?: return copy(distanceToSelectedMeters = null)
        return copy(distanceToSelectedMeters = distanceMeters(location.point, landmark.point))
    }

    override fun onCleared() {
        locationProvider.stopLocationUpdates()
        super.onCleared()
    }

    companion object {
        fun factory(
            landmarkRepository: LandmarkRepository,
            locationProvider: LocationProvider,
            privacyManager: LocationPrivacyManager,
            hasAmapKey: Boolean,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MapViewModel(
                landmarkRepository = landmarkRepository,
                locationProvider = locationProvider,
                privacyManager = privacyManager,
                hasAmapKey = hasAmapKey,
            ) as T
        }
    }
}
