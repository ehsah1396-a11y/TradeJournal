package com.example.tradejournal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.TimeZone

private class Sess(val name: String, val startUtc: Int, val endUtc: Int, val color: Color)

private val SESSIONS = listOf(
    Sess("سیدنی", 22, 7, Color(0xFF8B93FF)),
    Sess("توکیو", 0, 9, Color(0xFFFF7AA8)),
    Sess("لندن", 8, 17, Color(0xFF3FD0C9)),
    Sess("نیویورک", 13, 22, Color(0xFFF2A93B))
)

private fun mod24(x: Double): Double { val r = x % 24.0; return if (r < 0) r + 24.0 else r }

private fun segs(start: Double, len: Double): List<Pair<Double, Double>> =
    if (start + len <= 24.0) listOf(start to start + len)
    else listOf(0.0 to start + len - 24.0, start to 24.0)

fun faDigits(s: String): String = s.map { if (it in '0'..'9') '۰' + (it - '0') else it }.joinToString("")

@Composable
fun SessionStrip() {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(20_000); now = System.currentTimeMillis() } }
    var open by remember { mutableStateOf(true) }

    val tz = TimeZone.getDefault()
    val off = tz.getOffset(now) / 3600000.0
    val local = Calendar.getInstance(tz).apply { timeInMillis = now }
    val hh = local.get(Calendar.HOUR_OF_DAY)
    val mm = local.get(Calendar.MINUTE)
    val nowH = hh + mm / 60.0

    val info = SESSIONS.map { s ->
        Triple(s, mod24(s.startUtc + off), mod24((s.endUtc - s.startUtc).toDouble()))
    }
    val active = info.filter { (_, ls, len) -> mod24(nowH - ls) < len }

    val ov = ArrayList<Pair<Double, Double>>()
    var t = 0
    while (t < 96) {
        if (info.count { (_, ls, len) -> mod24(t / 4.0 - ls) < len } >= 2) {
            var e = t
            while (e < 96 && info.count { (_, ls, len) -> mod24(e / 4.0 - ls) < len } >= 2) e++
            ov.add(t / 4.0 to e / 4.0)
            t = e
        } else t++
    }
    val isOverlap = active.size >= 2

    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = now }
    val dow = utc.get(Calendar.DAY_OF_WEEK)
    val uh = utc.get(Calendar.HOUR_OF_DAY)
    val closed = dow == Calendar.SATURDAY ||
            (dow == Calendar.FRIDAY && uh >= 22) || (dow == Calendar.SUNDAY && uh < 22)

    val names = active.joinToString(" + ") { it.first.name }
    val label = if (closed) "بازار بسته است (آخر هفته)"
    else if (active.isEmpty()) "بازار آرام"
    else if (isOverlap) "$names · همپوشانی"
    else names

    Column(
        Modifier.fillMaxWidth().background(ColCard).clickable { open = !open }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(faDigits("%02d:%02d".format(hh, mm)), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                label, Modifier.weight(1f), fontSize = 12.sp,
                color = if (isOverlap && !closed) ColSaffron else ColText,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(tz.id, fontSize = 10.sp, color = ColMuted, maxLines = 1)
        }
        if (open) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        info.forEach { (s, ls, len) -> Lane(segs(ls, len), s.color, s.name) }
                        Lane(ov, ColSaffron, "همپوشانی")
                    }
                    val nh = nowH.coerceIn(0.05, 23.95).toFloat()
                    Row(Modifier.matchParentSize()) {
                        Spacer(Modifier.weight(nh))
                        Box(Modifier.width(2.dp).fillMaxHeight().background(ColText))
                        Spacer(Modifier.weight(24f - nh))
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    for (h in 0 until 24 step 3) {
                        Text(faDigits("%02d".format(h)), Modifier.weight(1f), fontSize = 8.sp, color = ColMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun Lane(segs: List<Pair<Double, Double>>, color: Color, label: String) {
    Row(Modifier.fillMaxWidth().height(11.dp)) {
        var cur = 0.0
        segs.sortedBy { it.first }.forEach { (a, b) ->
            if (a - cur > 0.001) Spacer(Modifier.weight((a - cur).toFloat()))
            if (b - a > 0.001) {
                Box(
                    Modifier.weight((b - a).toFloat()).fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        label, Modifier.padding(start = 3.dp), fontSize = 8.sp, color = Color.White,
                        maxLines = 1, softWrap = false, overflow = TextOverflow.Clip
                    )
                }
            }
            cur = b
        }
        if (24.0 - cur > 0.001) Spacer(Modifier.weight((24.0 - cur).toFloat()))
    }
}
