package com.bitse.geotreasure.navigation

sealed interface AppDestination {
    data object Map : AppDestination
    data class LandmarkDetail(val landmarkId: String) : AppDestination
    data class Camera(val landmarkId: String, val draftId: String) : AppDestination
    data class CheckInResult(val checkInId: String) : AppDestination
    data class Puzzle(val puzzleId: String) : AppDestination
}

object AppRoutes {
    const val MAP = "map"
    const val LANDMARK_DETAIL = "landmarks/{landmarkId}"
    const val CAMERA = "camera/{landmarkId}/{draftId}"
    const val CHECK_IN_RESULT = "checkins/{checkInId}/result"
    const val PUZZLE = "puzzles/{puzzleId}"

    fun landmarkDetail(landmarkId: String) = "landmarks/$landmarkId"
    fun camera(landmarkId: String, draftId: String) = "camera/$landmarkId/$draftId"
    fun checkInResult(checkInId: String) = "checkins/$checkInId/result"
    fun puzzle(puzzleId: String) = "puzzles/$puzzleId"
}
