package com.ccteacher.daisai.ui.dice

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.ccteacher.daisai.ui.theme.TableGreen

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

    // 대/소 텍스트·색상
    val daesoText = when {
        !showLabels || isTriple -> ""
        sum in 11..17 -> "대"
        else -> "소"
    }
    val daesoColor = when {
        sum in 11..17 -> Color(0xFFEF5350) // 빨강
        else -> Color(0xFF42A5F5)           // 파랑
    }

    // 홀/짝 텍스트·색상
    val holjjakText = when {
        !showLabels || isTriple -> ""
        sum % 2 == 1 -> "홀"
        else -> "짝"
    }
    val holjjakColor = when {
        sum % 2 == 1 -> Color(0xFFEF5350)  // 빨강
        else -> Color(0xFF42A5F5)           // 파랑
    }

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

        Spacer(Modifier.height(32.dp))

        // 주사위 + 양쪽 레이블
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // 왼쪽: 대/소
            Text(
                text = daesoText,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = daesoColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(56.dp)
            )

            Spacer(Modifier.width(8.dp))

            // 주사위 3개 (삼각 배치)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DiceRollAnimation(
                    targetValue = state.results[0],
                    rollKey = state.rollKey,
                    onDone = { vm.onDiceDone() }
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DiceRollAnimation(
                        targetValue = state.results[1],
                        rollKey = state.rollKey,
                        onDone = { vm.onDiceDone() }
                    )
                    DiceRollAnimation(
                        targetValue = state.results[2],
                        rollKey = state.rollKey,
                        onDone = { vm.onDiceDone() }
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // 오른쪽: 홀/짝
            Text(
                text = holjjakText,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = holjjakColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(56.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // 합계 표시
        Text(
            text = "합계: $sum",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(24.dp))

        // 버튼 행
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { vm.roll() },
                enabled = !state.isRolling
            ) {
                Text("🎲 굴리기")
            }
            OutlinedButton(
                onClick = { (context as? Activity)?.finish() }
            ) {
                Text("끝내기", color = Color.White)
            }
        }

        // 베팅 추천 패널
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
