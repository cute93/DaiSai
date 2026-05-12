package com.ccteacher.daisai.ui.dice

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ccteacher.daisai.ui.theme.DiceEdge
import com.ccteacher.daisai.ui.theme.DiceIvory
import com.ccteacher.daisai.ui.theme.PipRed
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    fun normalize(): Vec3 {
        val len = sqrt(x * x + y * y + z * z)
        return if (len == 0f) this else Vec3(x / len, y / len, z / len)
    }
}

private fun rotatePoint(p: Vec3, rx: Float, ry: Float, rz: Float): Vec3 {
    val radX = Math.toRadians(rx.toDouble()).toFloat()
    val radY = Math.toRadians(ry.toDouble()).toFloat()
    val radZ = Math.toRadians(rz.toDouble()).toFloat()

    // Rx
    var x = p.x
    var y = p.y * cos(radX) - p.z * sin(radX)
    var z = p.y * sin(radX) + p.z * cos(radX)

    // Ry
    val x2 = x * cos(radY) + z * sin(radY)
    val z2 = -x * sin(radY) + z * cos(radY)
    x = x2; z = z2

    // Rz
    val x3 = x * cos(radZ) - y * sin(radZ)
    val y3 = x * sin(radZ) + y * cos(radZ)
    return Vec3(x3, y3, z)
}

private fun project(p: Vec3, cx: Float, cy: Float, fov: Float): Offset {
    val scale = fov / (fov + p.z)
    return Offset(cx + p.x * scale, cy + p.y * scale)
}

private val lightDir = Vec3(-0.5f, -0.8f, 1.0f).normalize()

private fun shadeFace(rotatedNormal: Vec3): Color {
    val dot = maxOf(0f, rotatedNormal.dot(lightDir))
    val brightness = 0.30f + 0.70f * dot
    return Color(
        red   = (DiceIvory.red   * brightness).coerceIn(0f, 1f),
        green = (DiceIvory.green * brightness).coerceIn(0f, 1f),
        blue  = (DiceIvory.blue  * brightness).coerceIn(0f, 1f),
    )
}

// 표준 주사위 배치: [front, back, top, bottom, right, left]
// 마주 보는 면 합 = 7, 1-2-3 반시계 방향 (서양 ISO 기준)
private val STANDARD_FACES = mapOf(
    1 to listOf(1, 6, 2, 5, 3, 4),
    2 to listOf(2, 5, 6, 1, 3, 4),
    3 to listOf(3, 4, 2, 5, 6, 1),
    4 to listOf(4, 3, 2, 5, 1, 6),
    5 to listOf(5, 2, 6, 1, 4, 3),
    6 to listOf(6, 1, 5, 2, 4, 3),
)

// pip 정규화 좌표 (u=가로 0~1, v=세로 0~1)
private fun pipPositions(value: Int): List<Pair<Float, Float>> = when (value) {
    1 -> listOf(0.50f to 0.50f)
    2 -> listOf(0.28f to 0.28f, 0.72f to 0.72f)
    3 -> listOf(0.28f to 0.28f, 0.50f to 0.50f, 0.72f to 0.72f)
    4 -> listOf(0.28f to 0.28f, 0.72f to 0.28f, 0.28f to 0.72f, 0.72f to 0.72f)
    5 -> listOf(0.28f to 0.28f, 0.72f to 0.28f, 0.50f to 0.50f, 0.28f to 0.72f, 0.72f to 0.72f)
    6 -> listOf(0.28f to 0.28f, 0.72f to 0.28f, 0.28f to 0.50f, 0.72f to 0.50f, 0.28f to 0.72f, 0.72f to 0.72f)
    else -> emptyList()
}

private fun DrawScope.drawPipsOnFace(verts: List<Offset>, faceValue: Int) {
    val p0 = verts[0]  // 좌상
    val uAxis = verts[1] - p0  // 가로축 (우상 - 좌상)
    val vAxis = verts[3] - p0  // 세로축 (좌하 - 좌상)
    val faceSize = Offset(uAxis.x, uAxis.y).getDistance()
    val radius = faceSize * 0.10f
    val pipColor = if (faceValue == 1) PipRed else DiceEdge

    pipPositions(faceValue).forEach { (nu, nv) ->
        val center = Offset(
            p0.x + uAxis.x * nu + vAxis.x * nv,
            p0.y + uAxis.y * nu + vAxis.y * nv
        )
        drawCircle(pipColor, radius, center)
    }
}

private data class FaceDef(val vIdx: IntArray, val normal: Vec3, val value: Int)

@Composable
fun DiceFace3D(
    targetValue: Int,
    rotX: Float,
    rotY: Float,
    rotZ: Float,
    modifier: Modifier = Modifier,
    diceSize: Dp = 48.dp
) {
    val faces = STANDARD_FACES[targetValue] ?: STANDARD_FACES[1]!!

    Canvas(modifier = modifier.size(diceSize)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r  = size.width * 0.38f
        val fov = size.width * 2.8f

        val rawVerts = listOf(
            Vec3(-r, -r, -r), Vec3(+r, -r, -r), Vec3(+r, +r, -r), Vec3(-r, +r, -r),
            Vec3(-r, -r, +r), Vec3(+r, -r, +r), Vec3(+r, +r, +r), Vec3(-r, +r, +r)
        )
        val rotated = rawVerts.map { rotatePoint(it, rotX, rotY, rotZ) }

        val faceDefs = listOf(
            FaceDef(intArrayOf(4, 5, 6, 7), Vec3(0f, 0f, +1f), faces[0]),  // Front
            FaceDef(intArrayOf(1, 0, 3, 2), Vec3(0f, 0f, -1f), faces[1]),  // Back
            FaceDef(intArrayOf(0, 1, 5, 4), Vec3(0f, -1f, 0f), faces[2]),  // Top
            FaceDef(intArrayOf(7, 6, 2, 3), Vec3(0f, +1f, 0f), faces[3]),  // Bottom
            FaceDef(intArrayOf(5, 1, 2, 6), Vec3(+1f, 0f, 0f), faces[4]),  // Right
            FaceDef(intArrayOf(0, 4, 7, 3), Vec3(-1f, 0f, 0f), faces[5]),  // Left
        )

        faceDefs
            .mapNotNull { face ->
                val rotNormal = rotatePoint(face.normal, rotX, rotY, rotZ)
                if (rotNormal.z <= 0f) return@mapNotNull null  // back-face culling
                val rotVerts = face.vIdx.map { rotated[it] }
                val avgZ = rotVerts.map { it.z }.average().toFloat()
                Triple(face, rotNormal, Pair(rotVerts, avgZ))
            }
            .sortedBy { (_, _, p) -> p.second }
            .forEach { (face, rotNormal, p) ->
                val projVerts = p.first.map { project(it, cx, cy, fov) }

                val path = Path().apply {
                    moveTo(projVerts[0].x, projVerts[0].y)
                    for (i in 1..3) lineTo(projVerts[i].x, projVerts[i].y)
                    close()
                }
                drawPath(path, shadeFace(rotNormal))
                drawPath(path, DiceEdge, style = Stroke(width = size.width * 0.025f))
                drawPipsOnFace(projVerts, face.value)
            }
    }
}
