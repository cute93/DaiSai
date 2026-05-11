package com.ccteacher.daisai.ui.dice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ccteacher.daisai.ui.theme.GoldAccent

data class BettingRecommendation(
    val name: String,
    val payout: Int,
    val description: String
)

fun evaluateBets(d1: Int, d2: Int, d3: Int): List<BettingRecommendation> {
    val sum = d1 + d2 + d3
    val dice = listOf(d1, d2, d3)
    val isTriple = d1 == d2 && d2 == d3
    val results = mutableListOf<BettingRecommendation>()

    // 스페시픽 트리플 150:1
    if (isTriple) {
        results += BettingRecommendation("트리플 $d1", 150, "주사위 3개 모두 $d1")
    }

    // 애니 트리플 24:1
    if (isTriple) {
        results += BettingRecommendation("애니 트리플", 24, "3개 모두 동일 숫자")
    }

    // 합계 베팅
    val totalPayout = mapOf(
        4 to 50, 5 to 18, 6 to 14, 7 to 12, 8 to 8,
        9 to 6, 10 to 6, 11 to 6, 12 to 6, 13 to 8,
        14 to 12, 15 to 14, 16 to 18, 17 to 50
    )
    totalPayout[sum]?.let { payout ->
        results += BettingRecommendation("합계 $sum", payout, "세 주사위 합이 $sum")
    }

    // 더블 8:1
    val counts = dice.groupingBy { it }.eachCount()
    counts.filter { it.value >= 2 }.forEach { (num, _) ->
        results += BettingRecommendation("더블 $num", 8, "주사위 2개가 $num")
    }

    // 도미노 5:1
    listOf(d1 to d2, d1 to d3, d2 to d3)
        .filter { (a, b) -> a != b }
        .distinctBy { (a, b) -> setOf(a, b) }
        .forEach { (a, b) ->
            results += BettingRecommendation("도미노 $a-$b", 5, "$a 와 $b 조합")
        }

    // 싱글 (등장 횟수별 배당)
    counts.forEach { (num, cnt) ->
        results += BettingRecommendation("싱글 $num (${cnt}개)", cnt, "$num 이 ${cnt}개 등장")
    }

    // 대/소, 홀/짝 — 트리플 제외
    if (!isTriple) {
        if (sum in 11..17) results += BettingRecommendation("대 (Big)", 1, "합계 11~17")
        if (sum in 4..10)  results += BettingRecommendation("소 (Small)", 1, "합계 4~10")
        if (sum % 2 == 1)  results += BettingRecommendation("홀 (Odd)", 1, "합계 홀수")
        if (sum % 2 == 0)  results += BettingRecommendation("짝 (Even)", 1, "합계 짝수")
    }

    return results.sortedByDescending { it.payout }.take(3)
}

@Composable
fun BettingSuggestionPanel(
    suggestions: List<BettingRecommendation>,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) +
                scaleIn(initialScale = 0.85f, animationSpec = tween(400)) +
                slideInVertically(tween(400)) { it / 3 },
        exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it / 3 },
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Text(
                text = "★ 추천 베팅 TOP 3",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GoldAccent,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            suggestions.forEachIndexed { i, rec ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.92f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${i + 1}위",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(40.dp)
                        )
                        Column {
                            Text(
                                text = rec.name,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "배당 ${rec.payout}:1  •  ${rec.description}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
