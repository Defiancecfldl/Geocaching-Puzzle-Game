package com.bitse.geotreasure.feature.puzzle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PuzzleUiState(val puzzleId: String, val ownedPieceIndexes: Set<Int> = setOf(0, 3, 7))
sealed interface PuzzleAction { data class SelectPiece(val pieceIndex: Int) : PuzzleAction }
sealed interface PuzzleEffect

class PuzzleViewModel(puzzleId: String) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PuzzleUiState(puzzleId))
    val uiState: StateFlow<PuzzleUiState> = mutableUiState.asStateFlow()

    fun onAction(action: PuzzleAction) {
        if (action is PuzzleAction.SelectPiece && action.pieceIndex !in mutableUiState.value.ownedPieceIndexes) return
    }

    companion object {
        fun factory(puzzleId: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = PuzzleViewModel(puzzleId) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleScreen(viewModel: PuzzleViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("校园拼图") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("已收集 ${state.ownedPieceIndexes.size} / 9", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("C 将在此加入拖拽、吸附和 Room 恢复")
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(9) { index ->
                    val owned = index in state.ownedPieceIndexes
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(
                                if (owned) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.shapes.small,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (owned) {
                            Text("${index + 1}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Icon(Icons.Rounded.Lock, contentDescription = "未获得")
                        }
                    }
                }
            }
        }
    }
}
