package com.ccteacher.daisai.ui.dice

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ccteacher.daisai.ui.theme.TableGreen

@Composable
fun ThreeDiceBoard(
    modifier: Modifier = Modifier,
    vm: DiceRollViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .background(TableGreen)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 타이틀
        Text(
            text = "大細  다이사이",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(32.dp))

        // 주사위 1 — 상단 중앙
        DiceRollAnimation(
            targetValue = state.results[0],
            rollKey = state.rollKey,
            onDone = { vm.onDiceDone() }
        )

        Spacer(Modifier.height(16.dp))

        // 주사위 2, 3 — 하단 좌우
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

        Spacer(Modifier.height(16.dp))

        // 합계 표시
        Text(
            text = "합계: ${state.results.sum()}",
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
