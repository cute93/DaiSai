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
    val bets: Map<BetType, Int> = emptyMap(),
    val activeBetType: BetType? = null,
    val phase: GamePhase = GamePhase.BETTING,
    val lastWinAmount: Int = 0,
    val isWin: Boolean? = null
)

class DiceRollViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DiceUiState())
    val uiState: StateFlow<DiceUiState> = _uiState.asStateFlow()

    fun selectBetType(type: BetType) {
        _uiState.update {
            it.copy(activeBetType = if (it.activeBetType == type) null else type)
        }
    }

    fun addChip(amount: Int) {
        _uiState.update { state ->
            val active = state.activeBetType ?: return@update state
            val canAdd = minOf(amount, state.balance - state.bets.values.sum())
            if (canAdd <= 0) return@update state
            val newBets = state.bets.toMutableMap()
            newBets[active] = (newBets[active] ?: 0) + canAdd
            state.copy(bets = newBets)
        }
    }

    fun clearBet() {
        _uiState.update { it.copy(bets = emptyMap(), activeBetType = null) }
    }

    fun roll() {
        val s = _uiState.value
        if (s.bets.isEmpty() || s.bets.values.all { it == 0 }) return
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
            if (state.phase != GamePhase.ROLLING) return@update state
            val newCount = state.doneCount + 1
            if (newCount < 3) return@update state.copy(doneCount = newCount)

            val suggestions = evaluateBets(state.results[0], state.results[1], state.results[2])
            val totalWin = state.bets.entries.sumOf { (betType, betAmount) ->
                val (won, multiplier) = resolveBet(betType, state.results)
                if (won) betAmount * multiplier else -betAmount
            }
            state.copy(
                doneCount = newCount,
                isRolling = false,
                suggestions = suggestions,
                phase = GamePhase.RESULT,
                balance = state.balance + totalWin,
                lastWinAmount = totalWin,
                isWin = totalWin > 0
            )
        }
    }

    fun nextRound() {
        _uiState.update {
            it.copy(
                bets = emptyMap(),
                activeBetType = null,
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
