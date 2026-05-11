package com.ccteacher.daisai.ui.dice

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DiceUiState(
    val results: List<Int> = listOf(1, 1, 1),
    val rollKey: Int = 0,
    val isRolling: Boolean = false,
    val doneCount: Int = 0,
    val suggestions: List<BettingRecommendation> = emptyList()
)

class DiceRollViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DiceUiState())
    val uiState: StateFlow<DiceUiState> = _uiState.asStateFlow()

    fun roll() {
        _uiState.update {
            it.copy(
                results = List(3) { (1..6).random() },
                rollKey = it.rollKey + 1,
                isRolling = true,
                doneCount = 0,
                suggestions = emptyList()
            )
        }
    }

    fun onDiceDone() {
        _uiState.update { state ->
            val newCount = state.doneCount + 1
            val finished = newCount >= 3
            state.copy(
                doneCount = newCount,
                isRolling = !finished,
                suggestions = if (finished) {
                    evaluateBets(state.results[0], state.results[1], state.results[2])
                } else {
                    state.suggestions
                }
            )
        }
    }
}
