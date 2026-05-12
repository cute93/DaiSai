package com.ccteacher.daisai.ui.dice

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ccteacher.daisai.ui.theme.GoldAccent
import com.ccteacher.daisai.ui.theme.TableGreen

private val SumPayouts = mapOf(
    4 to 50, 5 to 18, 6 to 14, 7 to 12,
    8 to 8, 9 to 6, 10 to 6, 11 to 6, 12 to 6, 13 to 8,
    14 to 12, 15 to 14, 16 to 18, 17 to 50
)

@Composable
fun ThreeDiceBoard(
    modifier: Modifier = Modifier,
    vm: DiceRollViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    val sum = state.results.sum()
    val isTriple = state.results.let { it[0] == it[1] && it[1] == it[2] }
    val showLabels = state.rollKey > 0 && !state.isRolling

    val daesoText = when {
        !showLabels || isTriple -> ""
        sum in 11..17 -> "대"
        else -> "소"
    }
    val daesoColor = if (sum in 11..17) Color(0xFFEF5350) else Color(0xFF42A5F5)

    val holjjakText = when {
        !showLabels || isTriple -> ""
        sum % 2 == 1 -> "홀"
        else -> "짝"
    }
    val holjjakColor = if (sum % 2 == 1) Color(0xFFEF5350) else Color(0xFF42A5F5)

    Column(
        modifier = modifier
            .background(TableGreen)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 타이틀
        Text(
            text = "다이사이",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(8.dp))

        // 잔액 / 배팅 표시
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("잔액: \$${state.balance}", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("배팅: \$${state.currentBet}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(24.dp))

        // 주사위 + 양쪽 레이블
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = daesoText,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = daesoColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(56.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DiceRollAnimation(targetValue = state.results[0], rollKey = state.rollKey, onDone = { vm.onDiceDone() })
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    DiceRollAnimation(targetValue = state.results[1], rollKey = state.rollKey, onDone = { vm.onDiceDone() })
                    DiceRollAnimation(targetValue = state.results[2], rollKey = state.rollKey, onDone = { vm.onDiceDone() })
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = holjjakText,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = holjjakColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(56.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (state.rollKey > 0) "합계: $sum" else "합계: --",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
        Spacer(Modifier.height(16.dp))

        when (state.phase) {
            GamePhase.BETTING -> {
                if (state.balance < 10) {
                    GameOverSection(onRestart = { vm.resetGame() })
                } else {
                    BettingSection(state = state, vm = vm)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { vm.roll() },
                            enabled = state.selectedBetType != null && state.currentBet > 0
                        ) {
                            Text("🎲 굴리기")
                        }
                        OutlinedButton(onClick = { (context as? Activity)?.finish() }) {
                            Text("끝내기", color = Color.White)
                        }
                    }
                }
            }

            GamePhase.ROLLING -> {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = {}, enabled = false) { Text("🎲 굴리는 중...") }
                    OutlinedButton(onClick = { (context as? Activity)?.finish() }) {
                        Text("끝내기", color = Color.White)
                    }
                }
            }

            GamePhase.RESULT -> {
                ResultSection(state = state, onNextRound = { vm.nextRound() }, onFinish = { (context as? Activity)?.finish() })
                if (state.suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                    Spacer(Modifier.height(8.dp))
                }
                BettingSuggestionPanel(
                    suggestions = state.suggestions,
                    visible = state.suggestions.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun BettingSection(state: DiceUiState, vm: DiceRollViewModel) {
    // ── 대/소/홀/짝 ──
    BetSectionLabel("대 / 소 / 홀 / 짝  (1:1)")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(BetType.Big to "대", BetType.Small to "소", BetType.Odd to "홀", BetType.Even to "짝")
            .forEach { (type, label) ->
                BetChipButton(
                    label = label,
                    subLabel = null,
                    selected = state.selectedBetType == type,
                    onClick = { vm.selectBetType(type) },
                    modifier = Modifier.weight(1f)
                )
            }
    }

    Spacer(Modifier.height(12.dp))

    // ── 합계 배팅 ──
    BetSectionLabel("합계 배팅")
    listOf(4..10, 11..17).forEach { range ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            range.forEach { n ->
                BetChipButton(
                    label = "$n",
                    subLabel = "${SumPayouts[n]}:1",
                    selected = state.selectedBetType == BetType.Total(n),
                    onClick = { vm.selectBetType(BetType.Total(n)) },
                    modifier = Modifier.weight(1f),
                    compact = true
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }

    Spacer(Modifier.height(8.dp))

    // ── 단일 숫자 ──
    BetSectionLabel("단일 숫자  (1개 1:1  /  2개 2:1  /  3개 3:1)")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..6).forEach { n ->
            BetChipButton(
                label = "$n",
                subLabel = null,
                selected = state.selectedBetType == BetType.Single(n),
                onClick = { vm.selectBetType(BetType.Single(n)) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
    Spacer(Modifier.height(12.dp))

    // 칩 버튼
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(10, 50, 100).forEach { chip ->
            Button(
                onClick = { vm.addChip(chip) },
                enabled = state.balance - state.currentBet >= chip
            ) {
                Text("\$$chip")
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("현재 배팅: \$${state.currentBet}", color = Color.White, fontWeight = FontWeight.SemiBold)
        OutlinedButton(onClick = { vm.clearBet() }, enabled = state.currentBet > 0) {
            Text("초기화", color = Color.White)
        }
    }
}

@Composable
private fun BetChipButton(
    label: String,
    subLabel: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val bg = if (selected) GoldAccent else Color.White.copy(alpha = 0.2f)
    val textColor = if (selected) Color.Black else Color.White
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = bg),
        contentPadding = if (compact) PaddingValues(horizontal = 2.dp, vertical = 6.dp) else PaddingValues(vertical = 8.dp)
    ) {
        if (subLabel != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text(subLabel, fontSize = 9.sp, color = textColor.copy(alpha = 0.85f))
            }
        } else {
            Text(label, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
private fun ResultSection(state: DiceUiState, onNextRound: () -> Unit, onFinish: () -> Unit) {
    val won = state.isWin == true
    val winAmount = state.lastWinAmount
    val betLabel = when (val bt = state.selectedBetType) {
        is BetType.Big    -> "대 (Big)"
        is BetType.Small  -> "소 (Small)"
        is BetType.Odd    -> "홀 (Odd)"
        is BetType.Even   -> "짝 (Even)"
        is BetType.Total  -> "합계 ${bt.sum}  (${SumPayouts[bt.sum]}:1)"
        is BetType.Single -> "단일 숫자 ${bt.num}"
        null              -> ""
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (won) Color(0xFF1B5E20) else Color(0xFFB71C1C)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (won) "🎉 승리!" else "😢 패배", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(betLabel, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (winAmount >= 0) "+\$$winAmount" else "-\$${-winAmount}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(onClick = onNextRound) { Text("다시 배팅") }
        OutlinedButton(onClick = onFinish) { Text("끝내기", color = Color.White) }
    }
}

@Composable
private fun GameOverSection(onRestart: () -> Unit) {
    Text("게임 오버", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFEF5350))
    Spacer(Modifier.height(8.dp))
    Text("잔액이 부족합니다", color = Color.White)
    Spacer(Modifier.height(16.dp))
    Button(onClick = onRestart) { Text("재시작 (\$1,000)") }
}

@Composable
private fun BetSectionLabel(text: String) {
    Text(
        text = text,
        color = GoldAccent,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    )
}
