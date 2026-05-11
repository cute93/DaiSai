package com.ccteacher.daisai.ui.dice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DiceRollAnimation(
    targetValue: Int,
    rollKey: Int,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displayValue by remember { mutableIntStateOf(targetValue) }
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(rollKey) {
        if (rollKey == 0) {
            displayValue = targetValue
            return@LaunchedEffect
        }

        // Phase 1: Rolling (1500ms) — 회전과 눈금 변경 병렬 실행
        coroutineScope {
            launch {
                rotation.animateTo(
                    targetValue = rotation.value + 720f,
                    animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
                )
            }
            launch {
                val endTime = System.currentTimeMillis() + 1400L
                while (System.currentTimeMillis() < endTime) {
                    displayValue = (1..6).random()
                    delay(80)
                }
            }
        }

        // Phase 2: Settling (700ms) — 최종 눈금 + bounce 착지
        displayValue = targetValue
        coroutineScope {
            launch {
                scale.animateTo(1.18f, tween(durationMillis = 130))
                scale.animateTo(1.0f, spring(dampingRatio = 0.4f, stiffness = 300f))
            }
            launch {
                rotation.animateTo(
                    targetValue = rotation.value + 25f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 120f)
                )
            }
        }

        onDone()
    }

    DiceFace(
        value = displayValue,
        modifier = modifier.graphicsLayer {
            rotationZ = rotation.value % 360f
            scaleX = scale.value
            scaleY = scale.value
        }
    )
}
