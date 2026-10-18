package com.bitse.geotreasure.data.repository

import com.bitse.geotreasure.core.common.AppError
import com.bitse.geotreasure.core.common.AppResult
import com.bitse.geotreasure.core.location.distanceMeters
import com.bitse.geotreasure.core.model.GeoPoint
import com.bitse.geotreasure.core.model.Landmark

class FakeLandmarkRepository : LandmarkRepository {
    private val landmarks = listOf(
        Landmark(
            id = "lmk_library",
            name = "图书馆北门",
            point = GeoPoint(latitude = 39.9598, longitude = 116.3165),
            checkInRadiusMeters = 35.0,
            difficulty = 2,
            terrain = 1,
            hint = "寻找入口附近最醒目的校史标识。",
        ),
        Landmark(
            id = "lmk_stadium",
            name = "中心体育场",
            point = GeoPoint(latitude = 39.9578, longitude = 116.3152),
            checkInRadiusMeters = 45.0,
            difficulty = 1,
            terrain = 1,
            hint = "沿环形跑道外侧寻找任务点。",
        ),
        Landmark(
            id = "lmk_gate",
            name = "校园主门",
            point = GeoPoint(latitude = 39.9607, longitude = 116.3139),
            checkInRadiusMeters = 30.0,
            difficulty = 2,
            terrain = 1,
            hint = "观察校名石旁的建筑细节。",
        ),
    )

    override suspend fun listLandmarks(
        center: GeoPoint?,
        radiusMeters: Int?,
    ): AppResult<List<Landmark>> {
        val filtered = if (center != null && radiusMeters != null) {
            landmarks.filter { distanceMeters(center, it.point) <= radiusMeters }
        } else {
            landmarks
        }
        return AppResult.Success(filtered)
    }

    override suspend fun getLandmark(landmarkId: String): AppResult<Landmark> {
        val landmark = landmarks.firstOrNull { it.id == landmarkId }
        return if (landmark != null) {
            AppResult.Success(landmark)
        } else {
            AppResult.Failure(
                AppError(
                    code = "LANDMARK_NOT_FOUND",
                    message = "没有找到这个地标。",
                    retryable = false,
                ),
            )
        }
    }
}
