package com.example.tradejournal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

private val MONTHS = listOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
)
private val WEEK = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

/** تبدیل تاریخ میلادی به شمسی؛ خروجی: [سال، ماه (۱ تا ۱۲)، روز] */
private fun toJalali(gy: Int, gm: Int, gd: Int): IntArray {
    val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    val gy2 = if (gm > 2) gy + 1 else gy
    var days = 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 + gd + gdm[gm - 1]
    var jy = -1595 + 33 * (days / 12053)
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) {
        jy += (days - 1) / 365
        days = (days - 1) % 365
    }
    val jm: Int
    val jd: Int
    if (days < 186) {
        jm = 1 + days / 31
        jd = 1 + days % 31
    } else {
        jm = 7 + (days - 186) / 30
        jd = 1 + (days - 186) % 30
    }
    return intArrayOf(jy, jm, jd)
}

private fun isLeap(jy: Int): Boolean = (jy % 33) in intArrayOf(1, 5, 9, 13, 17, 22, 26, 30)

private fun monthLen(jy: Int, jm: Int): Int =
    if (jm <= 6) 31 else if (jm <= 11) 30 else if (isLeap(jy)) 30 else 29

private fun jalaliOf(ms: Long): IntArray {
    val c = Calendar.getInstance()
    c.timeInMillis = ms
    return toJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

private fun mod7(x: Int): Int = ((x % 7) + 7) % 7

@Composable
fun CalendarScreen(trades: List<Trade>) {
    var offset by rememberSaveable { mutableIntStateOf(0) }

    // ماه نمایش‌داده‌شده را با جابه‌جایی از ماه جاری پیدا می‌کنیم
    val today = jalaliOf(System.currentTimeMillis())
    val todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) % 7   // شنبه = ۰
    var y = today[0]
    var m = today[1]
    var firstDay = mod7(todayDow - (today[2] - 1))
    var k = offset
    while (k > 0) {
        firstDay = mod7(firstDay + monthLen(y, m))
        m++
        if (m > 12) { m = 1; y++ }
        k--
    }
    while (k < 0) {
        m--
        if (m < 1) { m = 12; y-- }
        firstDay = mod7(firstDay - monthLen(y, m))
        k++
    }
    val year = y
    val month = m
    val first = firstDay
    val days = monthLen(year, month)

    val byDay = trades
        .groupBy { t -> val j = jalaliOf(t.createdAt); j[0] * 10000 + j[1] * 100 + j[2] }
        .mapValues { e -> e.value.sumOf { it.pl } }
    val monthKey = year * 100 + month
    val monthVals = byDay.filterKeys { it / 100 == monthKey }.values
    val monthTotal = monthVals.sum()
    val winDays = monthVals.count { it > 0 }
    val lossDays = monthVals.count { it < 0 }
    val rows = (first + days + 6) / 7

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(tr("تقویم سود و زیان"), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Row(
            Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { offset -= 1 }) { Text(if (Lang.en) "‹" else "›", fontSize = 26.sp) }
            Text(
                tr(MONTHS[month - 1]) + " " + faDigits(year.toString()),
                fontWeight = FontWeight.SemiBold, fontSize = 16.sp
            )
            TextButton(onClick = { offset += 1 }) { Text(if (Lang.en) "›" else "‹", fontSize = 26.sp) }
        }
        Row(Modifier.fillMaxWidth()) {
            WEEK.forEach {
                Text(tr(it), Modifier.weight(1f), textAlign = TextAlign.Center, color = ColMuted, fontSize = 12.sp)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (r in 0 until rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (c in 0..6) {
                        val d = r * 7 + c - first + 1
                        if (d < 1 || d > days) {
                            Spacer(Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val v = byDay[year * 10000 + month * 100 + d] ?: 0.0
                            val bg = if (v > 0) ColUp.copy(alpha = 0.35f)
                            else if (v < 0) ColDn.copy(alpha = 0.35f) else ColCard
                            Box(
                                Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(bg),
                                contentAlignment = Alignment.Center
                            ) { Text(faDigits(d.toString()), fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ColCard).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(tr("جمع این ماه"), color = ColMuted, fontSize = 12.sp)
            Text(
                fmt(monthTotal), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                color = if (monthTotal >= 0) ColUp else ColDn
            )
            Text(
                fa(winDays) + tr(" روز سودده، ") + fa(lossDays) + tr(" روز زیان‌ده"),
                color = ColMuted, fontSize = 12.sp
            )
        }
    }
}
