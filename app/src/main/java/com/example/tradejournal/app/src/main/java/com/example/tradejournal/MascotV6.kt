package com.example.tradejournal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp

val MOOD_NAMES: List<String>
    get() = listOf(tr("مطمئن"), tr("عادی"), tr("مضطرب"), tr("عصبی")).map { tr(it) }
val MASCOT_NAMES: List<String>
    get() = listOf(tr("گاو"), tr("گاو نر"), tr("خرس"), tr("خرس قطبی")).map { tr(it) }

private val Ink = Color(0xFF2A1A12)
private val Cream = Color(0xFFF3E5C0)

/** kind: 0 گاو، 1 گاو نر، 2 خرس، 3 خرس قطبی — mood: 0 مطمئن، 1 عادی، 2 مضطرب، 3 عصبی */
@Composable
fun Mascot(kind: Int, mood: Int, sz: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(sz)) {
        val s = size.width / 100f
        fun o(x: Float, y: Float) = Offset(x * s, y * s)
        fun sq(w: Float, h: Float) = Size(w * s, h * s)
        val st = Stroke(width = 3f * s, cap = StrokeCap.Round)
        val outline = Color(0x2E000000)
        val cow = kind <= 1
        val body = when (kind) { 0 -> Color.White; 1 -> Color(0xFFA65E34); 2 -> Color(0xFF8B5A3C); else -> Color(0xFFEEF3FF) }
        val muzzle = when (kind) { 0 -> Color(0xFFF6B3C0); 1 -> Color(0xFFE8B98E); 2 -> Color(0xFFDDB892); else -> Color(0xFFCBD8F2) }
        val mouthY: Float

        if (cow) {
            val ear = if (kind == 0) Color(0xFF2C2C36) else body
            rotate(-20f, o(13f, 38f)) { drawOval(ear, o(0f, 31f), sq(26f, 14f)) }
            rotate(20f, o(87f, 38f)) { drawOval(ear, o(74f, 31f), sq(26f, 14f)) }
            val horn = Path().apply {
                if (kind == 0) {
                    moveTo(30f * s, 24f * s); quadraticBezierTo(22f * s, 12f * s, 30f * s, 6f * s)
                    quadraticBezierTo(34f * s, 16f * s, 38f * s, 22f * s); close()
                    moveTo(70f * s, 24f * s); quadraticBezierTo(78f * s, 12f * s, 70f * s, 6f * s)
                    quadraticBezierTo(66f * s, 16f * s, 62f * s, 22f * s); close()
                } else {
                    moveTo(30f * s, 28f * s); quadraticBezierTo(6f * s, 30f * s, 7f * s, 8f * s)
                    quadraticBezierTo(20f * s, 20f * s, 40f * s, 22f * s); close()
                    moveTo(70f * s, 28f * s); quadraticBezierTo(94f * s, 30f * s, 93f * s, 8f * s)
                    quadraticBezierTo(80f * s, 20f * s, 60f * s, 22f * s); close()
                }
            }
            drawPath(horn, Cream)
            val head = Path().apply {
                moveTo(22f * s, 34f * s)
                quadraticBezierTo(22f * s, 18f * s, 50f * s, 18f * s)
                quadraticBezierTo(78f * s, 18f * s, 78f * s, 34f * s)
                lineTo(75f * s, 64f * s)
                quadraticBezierTo(72f * s, 92f * s, 50f * s, 92f * s)
                quadraticBezierTo(28f * s, 92f * s, 25f * s, 64f * s)
                close()
            }
            drawPath(head, body)
            drawPath(head, outline, style = Stroke(width = 1.5f * s))
            if (kind == 0) {
                val patch = Path().apply {
                    moveTo(22f * s, 34f * s); quadraticBezierTo(22f * s, 20f * s, 38f * s, 19f * s)
                    quadraticBezierTo(41f * s, 31f * s, 31f * s, 40f * s)
                    quadraticBezierTo(24f * s, 42f * s, 22f * s, 34f * s); close()
                }
                drawPath(patch, Color(0xFF2C2C36))
            } else {
                val blaze = Path().apply {
                    moveTo(45f * s, 21f * s); quadraticBezierTo(50f * s, 42f * s, 55f * s, 21f * s); close()
                }
                drawPath(blaze, Cream.copy(alpha = 0.85f))
            }
            drawOval(muzzle, o(30f, 61f), sq(40f, 30f))
            drawOval(Color(0xB35B2A2A), o(39f, 69f), sq(6f, 8f))
            drawOval(Color(0xB35B2A2A), o(55f, 69f), sq(6f, 8f))
            if (kind == 1) drawCircle(
                color = Color(0xFFE7B93B), radius = 4.6f * s, center = o(50f, 80f),
                style = Stroke(width = 2.4f * s)
            )
            mouthY = 84f
        } else {
            drawCircle(color = body, radius = 13f * s, center = o(22f, 24f))
            drawCircle(color = body, radius = 13f * s, center = o(78f, 24f))
            drawCircle(color = muzzle, radius = 6f * s, center = o(22f, 24f))
            drawCircle(color = muzzle, radius = 6f * s, center = o(78f, 24f))
            drawCircle(color = body, radius = 34f * s, center = o(50f, 54f))
            drawCircle(color = outline, radius = 34f * s, center = o(50f, 54f), style = Stroke(width = 1.5f * s))
            drawOval(muzzle, o(33f, 55f), sq(34f, 26f))
            drawOval(Ink, o(44f, 59f), sq(12f, 8f))
            mouthY = 71f
        }

        // چهره
        val dots = {
            drawCircle(color = Ink, radius = 3.6f * s, center = o(37f, 50f))
            drawCircle(color = Ink, radius = 3.6f * s, center = o(63f, 50f))
        }
        when (mood) {
            0 -> {
                drawArc(color = Ink, startAngle = 200f, sweepAngle = 140f, useCenter = false,
                    topLeft = o(31f, 44f), size = sq(12f, 12f), style = st)
                drawArc(color = Ink, startAngle = 200f, sweepAngle = 140f, useCenter = false,
                    topLeft = o(57f, 44f), size = sq(12f, 12f), style = st)
                drawCircle(color = Color(0x99FF8FA3), radius = 5f * s, center = o(27f, 60f))
                drawCircle(color = Color(0x99FF8FA3), radius = 5f * s, center = o(73f, 60f))
                drawArc(color = Ink, startAngle = 20f, sweepAngle = 140f, useCenter = false,
                    topLeft = o(42f, mouthY - 6f), size = sq(16f, 12f), style = st)
            }
            1 -> {
                dots()
                drawLine(color = Ink, start = o(45f, mouthY + 3f), end = o(55f, mouthY + 3f),
                    strokeWidth = 3f * s, cap = StrokeCap.Round)
            }
            2 -> {
                dots()
                drawLine(color = Ink, start = o(30f, 44f), end = o(43f, 40f), strokeWidth = 3f * s, cap = StrokeCap.Round)
                drawLine(color = Ink, start = o(70f, 44f), end = o(57f, 40f), strokeWidth = 3f * s, cap = StrokeCap.Round)
                drawArc(color = Ink, startAngle = 200f, sweepAngle = 140f, useCenter = false,
                    topLeft = o(43f, mouthY), size = sq(14f, 10f), style = st)
                drawOval(Color(0xFF7CC7FF), o(78f, 30f), sq(8f, 11f))
            }
            else -> {
                dots()
                drawLine(color = Ink, start = o(29f, 40f), end = o(44f, 46f), strokeWidth = 3f * s, cap = StrokeCap.Round)
                drawLine(color = Ink, start = o(71f, 40f), end = o(56f, 46f), strokeWidth = 3f * s, cap = StrokeCap.Round)
                drawCircle(color = Color(0x73FF5C5C), radius = 5f * s, center = o(27f, 61f))
                drawCircle(color = Color(0x73FF5C5C), radius = 5f * s, center = o(73f, 61f))
                drawArc(color = Ink, startAngle = 200f, sweepAngle = 140f, useCenter = false,
                    topLeft = o(43f, mouthY), size = sq(14f, 10f), style = st)
            }
        }
    }
}
