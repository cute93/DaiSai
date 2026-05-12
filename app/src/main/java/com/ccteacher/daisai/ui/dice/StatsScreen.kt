package com.ccteacher.daisai.ui.dice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ccteacher.daisai.ui.theme.GoldAccent
import com.ccteacher.daisai.ui.theme.TableGreen

private data class RollStat(
    val d1: Int, val d2: Int, val d3: Int,
    val sum: Int,
    val isTriple: Boolean
) {
    val isBig get() = !isTriple && sum in 11..17
    val isOdd get() = !isTriple && sum % 2 == 1
}

@Composable
fun StatsScreen(modifier: Modifier = Modifier, onClose: () -> Unit) {
    val rolls = remember {
        (1..10).map {
            val d1 = (1..6).random()
            val d2 = (1..6).random()
            val d3 = (1..6).random()
            val sum = d1 + d2 + d3
            RollStat(d1, d2, d3, sum, d1 == d2 && d2 == d3)
        }
    }

    val bigCount    = rolls.count { it.isBig }
    val smallCount  = rolls.count { !it.isTriple && !it.isBig }
    val tripleCount = rolls.count { it.isTriple }
    val oddCount    = rolls.count { it.isOdd }
    val evenCount   = rolls.count { !it.isTriple && !it.isOdd }

    val allFaces   = rolls.flatMap { listOf(it.d1, it.d2, it.d3) }
    val faceCounts = (1..6).associateWith { f -> allFaces.count { it == f } }
    val maxFace    = faceCounts.values.max()
    val topFaces   = faceCounts.filter { it.value == maxFace }.keys.sorted()

    val sumCounts   = (4..17).associateWith { s -> rolls.count { it.sum == s } }
    val maxSumCount = sumCounts.values.max().takeIf { it > 0 } ?: 1

    Column(
        modifier = modifier
            .background(TableGreen)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("통계  (10회 시뮬레이션)", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            OutlinedButton(
                onClick = onClose,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("닫기", color = Color.White, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
        Spacer(Modifier.height(6.dp))

        // ── 2단 배열 ──
        Row(modifier = Modifier.fillMaxWidth()) {

            // 왼쪽: 굴림 결과
            Column(modifier = Modifier.weight(0.45f)) {
                StatLabel("굴림 결과")
                rolls.forEachIndexed { i, r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${i + 1}",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "${r.d1}·${r.d2}·${r.d3}",
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.width(38.dp)
                        )
                        Text(
                            text = when {
                                r.isTriple -> "T"
                                r.isBig    -> "대"
                                else       -> "소"
                            },
                            color = when {
                                r.isTriple -> Color(0xFFFFD700)
                                r.isBig    -> Color(0xFFEF5350)
                                else       -> Color(0xFF42A5F5)
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(14.dp)
                        )
                        if (!r.isTriple) {
                            Text(
                                text = if (r.isOdd) "홀" else "짝",
                                color = if (r.isOdd) Color(0xFFEF5350) else Color(0xFF42A5F5),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // 오른쪽: 요약 통계
            Column(modifier = Modifier.weight(0.55f)) {

                // 대/소
                StatLabel("대 / 소")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallBadge("대", bigCount, Color(0xFFEF5350))
                    SmallBadge("소", smallCount, Color(0xFF42A5F5))
                    if (tripleCount > 0) SmallBadge("T", tripleCount, Color(0xFFFFD700))
                }

                Spacer(Modifier.height(5.dp))

                // 홀/짝
                StatLabel("홀 / 짝")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallBadge("홀", oddCount, Color(0xFFEF5350))
                    SmallBadge("짝", evenCount, Color(0xFF42A5F5))
                }

                Spacer(Modifier.height(5.dp))

                // 가장 많이 나온 눈
                StatLabel("최다 출현")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        topFaces.joinToString(", "),
                        color = GoldAccent,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("(${maxFace}회)", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                }

                // 눈별 분포 (1~6)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (1..6).forEach { face ->
                        val c = faceCounts[face] ?: 0
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$face",
                                color = if (c == maxFace) GoldAccent else Color.White,
                                fontWeight = if (c == maxFace) FontWeight.ExtraBold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                            Text("${c}회", color = Color.White.copy(alpha = 0.55f), fontSize = 9.sp)
                        }
                    }
                }

                Spacer(Modifier.height(5.dp))

                // 합계 분포
                StatLabel("합계 분포")
                (4..17).forEach { s ->
                    val count = sumCounts[s] ?: 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "$s",
                            color = Color.White,
                            fontSize = 10.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(16.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Box(modifier = Modifier.weight(1f).height(9.dp)) {
                            if (count > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(count.toFloat() / maxSumCount)
                                        .height(9.dp)
                                        .background(if (s in 11..17) Color(0xFFEF5350) else Color(0xFF42A5F5))
                                )
                            }
                        }
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "$count",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            modifier = Modifier.width(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatLabel(text: String) {
    Text(
        text = text,
        color = GoldAccent,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        modifier = Modifier.padding(bottom = 3.dp)
    )
}

@Composable
private fun SmallBadge(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text("${count}회", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
