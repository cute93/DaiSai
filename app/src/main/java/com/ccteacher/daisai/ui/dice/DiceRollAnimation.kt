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
import kotlin.random.Random

@Composable
fun DiceRollAnimation(
    targetValue: Int,
    rollKey: Int,
    rollDone: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displayValue by remember { mutableIntStateOf(targetValue) }
    val rotationX = remember { Animatable(0f) }
    val rotationY = remember { Animatable(0f) }
    val rotationZ = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    // 각 주사위 인스턴스마다 고유한 시드 (컴포지션마다 다름)
    val instanceSeed = remember { System.nanoTime().toInt() }

    LaunchedEffect(rollKey) {
        if (rollKey == 0 || rollDone) {
            displayValue = targetValue
            return@LaunchedEffect
        }

        // 이번 굴림의 개별 파라미터 (주사위마다, 굴림마다 다름)
        val rng = Random(instanceSeed.toLong() xor rollKey.toLong())
        val startDelay = rng.nextLong(0L, 250L)
        val duration   = rng.nextInt(1800, 2300)
        val addRotX    = 360f * rng.nextInt(2, 4)   // 720 또는 1080
        val addRotY    = 360f * rng.nextInt(2, 4)
        val addRotZ    = 360f * rng.nextInt(1, 3)   // 360 또는 720

        delay(startDelay)

        // Phase 1: Rolling — 각 주사위마다 다른 회전량·속도
        coroutineScope {
            launch {
                rotationX.animateTo(
                    targetValue = rotationX.value + addRotX,
                    animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
                )
            }
            launch {
                rotationY.animateTo(
                    targetValue = rotationY.value + addRotY,
                    animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
                )
            }
            launch {
                rotationZ.animateTo(
                    targetValue = rotationZ.value + addRotZ,
                    animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
                )
            }
            launch {
                val endTime = System.currentTimeMillis() + (duration - 200L)
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

    DiceFace3D(
        targetValue = displayValue,
        rotX = rotationX.value,
        rotY = rotationY.value,
        rotZ = rotationZ.value,
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
    )
}
