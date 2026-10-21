package com.bitse.geotreasure.feature.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.bitse.geotreasure.core.model.CampusZone
import com.bitse.geotreasure.core.model.Landmark
import com.bitse.geotreasure.core.model.MapRenderState

@Composable
fun DemoMapContent(
    state: MapRenderState,
    onLandmarkSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tertiary = MaterialTheme.colorScheme.tertiary
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE9EEE7)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val gridColor = Color(0xFFC8D2C8)
            val blockColor = Color(0xFFF7F7F1)
            val roadColor = Color(0xFFFFFFFF)
            val step = size.minDimension / 7f
            for (index in 0..8) {
                drawLine(
                    color = gridColor,
                    start = Offset(index * step, 0f),
                    end = Offset(index * step, size.height),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 12f)),
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, index * step),
                    end = Offset(size.width, index * step),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 12f)),
                )
            }
            drawRoundRect(
                color = blockColor,
                topLeft = Offset(size.width * 0.08f, size.height * 0.16f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.34f, size.height * 0.19f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f),
            )
            drawLine(
                color = roadColor,
                start = Offset(0f, size.height * 0.55f),
                end = Offset(size.width, size.height * 0.42f),
                strokeWidth = 34f,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = tertiary.copy(alpha = 0.14f),
                radius = 72f,
                center = center,
            )
            drawCircle(
                color = tertiary,
                radius = 18f,
                center = center,
                style = Stroke(width = 8f),
            )
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            state.landmarks.forEachIndexed { index, landmark ->
                val position = demoMarkerPositions[index % demoMarkerPositions.size]
                MapMarkerButton(
                    landmark = landmark,
                    selected = landmark.id == state.selectedLandmarkId,
                    color = landmark.campusZone.markerColor(),
                    onClick = { onLandmarkSelected(landmark.id) },
                    modifier = Modifier.offset(
                        x = maxWidth * position.xFraction,
                        y = maxHeight * position.yFraction,
                    ),
                )
            }
        }
        if (state.userLocation != null) {
            Icon(
                imageVector = Icons.Rounded.MyLocation,
                contentDescription = "当前位置",
                tint = tertiary,
                modifier = Modifier.align(Alignment.Center).size(30.dp),
            )
        }
    }
}

@Composable
private fun MapMarkerButton(
    landmark: Landmark,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(if (selected) 52.dp else 44.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondary else color,
            contentColor = Color.White,
        ),
    ) {
        Icon(Icons.Rounded.LocationOn, contentDescription = "选择${landmark.name}")
    }
}

private data class DemoMarkerPosition(
    val xFraction: Float,
    val yFraction: Float,
)

private val demoMarkerPositions = listOf(
    DemoMarkerPosition(0.12f, 0.14f),
    DemoMarkerPosition(0.31f, 0.18f),
    DemoMarkerPosition(0.14f, 0.30f),
    DemoMarkerPosition(0.34f, 0.34f),
    DemoMarkerPosition(0.61f, 0.14f),
    DemoMarkerPosition(0.80f, 0.20f),
    DemoMarkerPosition(0.63f, 0.31f),
    DemoMarkerPosition(0.82f, 0.37f),
    DemoMarkerPosition(0.13f, 0.48f),
    DemoMarkerPosition(0.33f, 0.54f),
    DemoMarkerPosition(0.58f, 0.49f),
    DemoMarkerPosition(0.77f, 0.55f),
)

@Composable
private fun CampusZone.markerColor(): Color = when (this) {
    CampusZone.NORTH -> MaterialTheme.colorScheme.primary
    CampusZone.EAST -> MaterialTheme.colorScheme.tertiary
    CampusZone.SOUTH -> MaterialTheme.colorScheme.secondary
}
