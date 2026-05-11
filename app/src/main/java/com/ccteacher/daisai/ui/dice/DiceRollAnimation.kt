package com.ccteacher.daisai.ui.dice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
    val rotationX = remember { Animatable(0f) }
    val rotationY = remember { Animatable(0f) }
    val rotationZ = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(rollKey) {
        if (rollKey == 0) {
            displayValue = targetValue
            return@LaunchedEffect
        }

        // Phase 1: Casino Rolling (2000ms) — X/Y/Z 3축 회전 + 빠른 숫자 변경
        coroutineScope {
            launch {
                rotationX.animateTo(
                    targetValue = rotationX.value + 540f,
                    animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
                )
            }
            launch {
                rotationY.animateTo(
                    targetValue = rotationY.value + 720f,
                    animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
                )
            }
            launch {
                rotationZ.animateTo(
                    targetValue = rotationZ.value + 360f,
                    animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
                )
            }
            launch {
                val endTime = System.currentTimeMillis() + 1800L
                while (System.currentTimeMillis() < endTime) {
                    displayValue = (1..6).random()
                    delay(50)
                }
            }
        }

        // Phase 2: Landing — 앞면 복귀 + 임팩트 bounce
        displayValue = targetValue
        coroutineScope {
            launch {
                scale.animateTo(1.25f, tween(durationMillis = 100))
                scale.animateTo(1.0f, spring(dampingRatio = 0.35f, stiffness = 450f))
            }
            launch {
                rotationX.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 180f)
                )
            }
            launch {
                rotationY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 180f)
                )
            }
            launch {
                rotationZ.animateTo(
                    targetValue = rotationZ.value + 20f,
                    animationSpec = spring(dampingRatio = 0.4f, stiffness = 120f)
                )
            }
        }

        onDone()
    }

    DiceFace(
        value = displayValue,
        modifier = modifier.graphicsLayer {
            this.rotationX = rotationX.value % 360f
            this.rotationY = rotationY.value % 360f
            this.rotationZ = rotationZ.value % 360f
            scaleX = scale.value
            scaleY = scale.value
            cameraDistance = 12f * density
        }
    )
}
