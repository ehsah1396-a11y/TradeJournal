package com.example.tradejournal

import android.icu.util.Calendar
import android.icu.util.PersianCalendar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MONTHS = listOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
)
private val WEEK = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

private fun dayKey(ms: Long): Int {
    val c = PersianCalendar()
    c.timeInMillis = ms
    return c.get(Calendar.YEAR) * 10000 + c.get(Calendar.MONTH) * 100 + c.get(Calendar.DAY_OF_MONTH)
}

@Composable
fun CalendarScreen(trades: List<Trade>) {
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val cal = PersianCalendar()
    cal.add(Calendar.MONTH, offset)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val first = cal.get(Calendar.DAY_OF_WEEK) % 7   // شنبه = ۰
    val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val byDay = trades.groupBy { dayKey(it.createdAt) }.mapValues { e -> e.value.sumOf { it.pl } }
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
        Text("تقویم سود و زیان", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { offset -= 1 }) { Text("›", fontSize = 26.sp) }
            Text("${MONTHS[month]} ${faDigits(year.toString())}", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            TextButton(onClick = { offset += 1 }) { Text("‹", fontSize = 26.sp) }
        }
        Row(Modifier.fillMaxWidth()) {
            WEEK.forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = ColMuted, fontSize = 12.sp)
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
            Text("جمع این ماه", color = ColMuted, fontSize = 12.sp)
            Text(fmt(monthTotal), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                color = if (monthTotal >= 0) ColUp else ColDn)
            Text("${fa(winDays)} روز سودده، ${fa(lossDays)} روز زیان‌ده", color = ColMuted, fontSize = 12.sp)
        }
    }
}
