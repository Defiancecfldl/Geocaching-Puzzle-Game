package com.bitse.geotreasure.core.model

import kotlinx.datetime.Instant

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

data class LocationSample(
    val point: GeoPoint,
    val accuracyMeters: Float,
    val capturedAt: Instant,
    val source: LocationSourceKind = LocationSourceKind.UNKNOWN,
)

enum class LocationSourceKind {
    GPS,
    WIFI,
    CELL,
    NETWORK,
    CACHE,
    DEMO,
    UNKNOWN,
}

data class Landmark(
    val id: String,
    val name: String,
    val point: GeoPoint,
    val checkInRadiusMeters: Double,
    val difficulty: Int,
    val terrain: Int,
    val hint: String?,
)

data class CheckInDraft(
    val id: String,
    val landmarkId: String,
    val localPhoto: LocalPhotoRef?,
    val uploadState: UploadState,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class LocalPhotoRef(
    val absolutePath: String,
    val mimeType: String,
    val sizeBytes: Long,
)

data class CheckInChallenge(
    val id: String,
    val landmarkId: String,
    val expiresAt: Instant,
)

data class FragmentGrant(
    val fragmentId: String,
    val puzzleId: String,
    val pieceIndex: Int,
    val grantedAt: Instant,
)

data class CheckInReceipt(
    val checkInId: String,
    val status: CheckInStatus,
    val fragmentGrant: FragmentGrant,
)

data class PuzzlePlacement(
    val fragmentId: String,
    val slotIndex: Int?,
    val offsetX: Float,
    val offsetY: Float,
    val updatedAt: Instant,
)

enum class UploadState {
    EMPTY,
    CAPTURED,
    UPLOADING,
    UPLOADED,
    FAILED,
    CONFIRMED,
}

enum class CheckInStatus {
    COMPLETED,
    ALREADY_COMPLETED,
}

data class UploadedPhoto(
    val photoId: String,
    val objectKey: String,
    val uploadedAt: Instant,
)

data class SubmitCheckInRequest(
    val landmarkId: String,
    val challengeId: String,
    val photoId: String,
    val location: LocationSample,
)

data class MapRenderState(
    val userLocation: LocationSample?,
    val landmarks: List<Landmark>,
    val selectedLandmarkId: String?,
)

sealed interface MapEvent {
    data class LandmarkSelected(val landmarkId: String) : MapEvent

    data class CameraMoved(val center: GeoPoint) : MapEvent
}

data class PuzzleSlot(
    val index: Int,
    val centerX: Float,
    val centerY: Float,
)

data class PuzzleState(
    val puzzleId: String,
    val ownedFragmentIds: Set<String>,
    val placements: List<PuzzlePlacement>,
    val completed: Boolean,
)

const val MAX_PHOTO_BYTES = 5_242_880L
