package com.ccteacher.daisai.ui.dice

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class BetType {
    object Big : BetType()
    object Small : BetType()
    object Odd : BetType()
    object Even : BetType()
    data class Total(val sum: Int) : BetType()
    data class Single(val num: Int) : BetType()
}

enum class GamePhase { BETTING, ROLLING, RESULT }

data class DiceUiState(
    val results: List<Int> = listOf(1, 1, 1),
    val rollKey: Int = 0,
    val isRolling: Boolean = false,
    val doneCount: Int = 0,
    val suggestions: List<BettingRecommendation> = emptyList(),
    val balance: Int = 1000,
    val currentBet: Int = 0,
    val selectedBetType: BetType? = null,
    val phase: GamePhase = GamePhase.BETTING,
    val lastWinAmount: Int = 0,
    val isWin: Boolean? = null
)

class DiceRollViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DiceUiState())
    val uiState: StateFlow<DiceUiState> = _uiState.asStateFlow()

    fun selectBetType(type: BetType) {
        _uiState.update {
            it.copy(selectedBetType = if (it.selectedBetType == type) null else type)
        }
    }

    fun addChip(amount: Int) {
        _uiState.update {
            val canAdd = minOf(amount, it.balance - it.currentBet)
            if (canAdd > 0) it.copy(currentBet = it.currentBet + canAdd) else it
        }
    }

    fun clearBet() {
        _uiState.update { it.copy(currentBet = 0, selectedBetType = null) }
    }

    fun roll() {
        val s = _uiState.value
        if (s.selectedBetType == null || s.currentBet <= 0) return
        _uiState.update {
            it.copy(
                results = List(3) { (1..6).random() },
                rollKey = it.rollKey + 1,
                isRolling = true,
                doneCount = 0,
                suggestions = emptyList(),
                phase = GamePhase.ROLLING,
                isWin = null,
                lastWinAmount = 0
            )
        }
    }

    fun onDiceDone() {
        _uiState.update { state ->
            val newCount = state.doneCount + 1
            if (newCount < 3) return@update state.copy(doneCount = newCount)

            val suggestions = evaluateBets(state.results[0], state.results[1], state.results[2])
            val betType = state.selectedBetType ?: return@update state.copy(
                doneCount = newCount, isRolling = false,
                suggestions = suggestions, phase = GamePhase.RESULT
            )

            val (won, multiplier) = resolveBet(betType, state.results)
            val winAmount = if (won) state.currentBet * multiplier else -state.currentBet
            state.copy(
                doneCount = newCount,
                isRolling = false,
                suggestions = suggestions,
                phase = GamePhase.RESULT,
                balance = state.balance + winAmount,
                lastWinAmount = winAmount,
                isWin = won
            )
        }
    }

    fun nextRound() {
        _uiState.update {
            it.copy(
                currentBet = 0,
                selectedBetType = null,
                phase = GamePhase.BETTING,
                isWin = null,
                lastWinAmount = 0,
                suggestions = emptyList()
            )
        }
    }

    fun resetGame() {
        _uiState.update { DiceUiState() }
    }

    private fun resolveBet(betType: BetType, results: List<Int>): Pair<Boolean, Int> {
        val sum = results.sum()
        val isTriple = results.toSet().size == 1
        val counts = results.groupingBy { it }.eachCount()
        val payoutTable = mapOf(
            4 to 50, 5 to 18, 6 to 14, 7 to 12,
            8 to 8, 9 to 6, 10 to 6, 11 to 6, 12 to 6, 13 to 8,
            14 to 12, 15 to 14, 16 to 18, 17 to 50
        )
        return when (betType) {
            is BetType.Big    -> Pair(!isTriple && sum in 11..17, 1)
            is BetType.Small  -> Pair(!isTriple && sum in 4..10, 1)
            is BetType.Odd    -> Pair(!isTriple && sum % 2 == 1, 1)
            is BetType.Even   -> Pair(!isTriple && sum % 2 == 0, 1)
            is BetType.Total  -> Pair(sum == betType.sum, payoutTable[betType.sum] ?: 0)
            is BetType.Single -> {
                val cnt = counts[betType.num] ?: 0
                Pair(cnt > 0, cnt)
            }
        }
    }
}
