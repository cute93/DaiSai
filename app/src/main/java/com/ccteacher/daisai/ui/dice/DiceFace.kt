package com.ccteacher.daisai.ui.dice

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ccteacher.daisai.ui.theme.DiceBorder
import com.ccteacher.daisai.ui.theme.DicePip
import com.ccteacher.daisai.ui.theme.DiceWhite

@Composable
fun DiceFace(
    value: Int,
    modifier: Modifier = Modifier,
    diceSize: Dp = 80.dp
) {
    Canvas(modifier = modifier.size(diceSize)) {
        val cornerRadius = size.width * 0.15f

        drawRoundRect(
            color = DiceWhite,
            cornerRadius = CornerRadius(cornerRadius)
        )
        drawRoundRect(
            color = DiceBorder,
            cornerRadius = CornerRadius(cornerRadius),
            style = Stroke(width = size.width * 0.025f)
        )
        pipPositions(value, size).forEach { offset ->
            drawCircle(
                color = DicePip,
                radius = size.width * 0.08f,
                center = offset
            )
        }
    }
}

internal fun pipPositions(value: Int, size: Size): List<Offset> {
    val w = size.width
    val pos = mapOf(
        "TL" to Offset(w * 0.28f, w * 0.28f),
        "TR" to Offset(w * 0.72f, w * 0.28f),
        "ML" to Offset(w * 0.28f, w * 0.50f),
        "CT" to Offset(w * 0.50f, w * 0.50f),
        "MR" to Offset(w * 0.72f, w * 0.50f),
        "BL" to Offset(w * 0.28f, w * 0.72f),
        "BR" to Offset(w * 0.72f, w * 0.72f),
    )
    val layout = mapOf(
        1 to listOf("CT"),
        2 to listOf("TL", "BR"),
        3 to listOf("TL", "CT", "BR"),
        4 to listOf("TL", "TR", "BL", "BR"),
        5 to listOf("TL", "TR", "CT", "BL", "BR"),
        6 to listOf("TL", "TR", "ML", "MR", "BL", "BR"),
    )
    return layout[value.coerceIn(1, 6)]!!.map { pos[it]!! }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B5E20)
@Composable
private fun DiceFacePreview() {
    Row {
        (1..6).forEach { v -> DiceFace(value = v, diceSize = 56.dp) }
    }
}
