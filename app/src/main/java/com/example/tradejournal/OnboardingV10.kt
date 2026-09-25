package com.example.tradejournal

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** آیا راهنمای اولین اجرا قبلاً دیده شده */
object Onboarding {
    private fun sp(c: Context) = c.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    fun done(c: Context): Boolean = sp(c).getBoolean("onboarding_done", false)
    fun setDone(c: Context) { sp(c).edit().putBoolean("onboarding_done", true).apply() }
}

private data class OnbPage(val emoji: String, val title: String, val desc: String, val mood: Int)

private val PAGES = listOf(
    OnbPage("👋", "به ژورنال ترید خوش اومدی", "همراهت برای ثبت و بررسی معاملاتت.", 0),
    OnbPage("🏠", "خانه", "خلاصه سود، وین‌ریت و بازدهی هر استراتژی رو اینجا می‌بینی.", 0),
    OnbPage("📒", "معاملات", "هر معامله رو با عکس چارت، ویس و احساست ثبت کن، یا از متاتریدر وارد کن.", 1),
    OnbPage("📅", "تقویم", "سود و زیان هر روز رو روی تقویم شمسی می‌بینی.", 0),
    OnbPage("⚙️", "تنظیمات", "ظاهر، زبان، شخصیت‌ها و قفل امنیتی همه از همین‌جا.", 2)
)

@Composable
fun OnboardingScreen(mascot: Int, onFinish: () -> Unit) {
    var idx by remember { mutableIntStateOf(0) }
    val page = PAGES[idx]

    Column(
        Modifier.fillMaxSize().background(ColBg).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onFinish) { Text(tr("رد شدن"), color = ColMuted) }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (idx == 0) Mascot(mascot, page.mood, 96.dp) else Text(page.emoji, fontSize = 56.sp)
            Spacer(Modifier.height(18.dp))
            Text(tr(page.title), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(tr(page.desc), color = ColMuted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 20.dp)) {
            PAGES.indices.forEach { i ->
                Box(
                    Modifier.size(if (i == idx) 10.dp else 8.dp).clip(CircleShape)
                        .background(if (i == idx) ColSaffron else ColCard2)
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (idx > 0) TextButton({ idx-- }) { Text(tr("قبلی")) } else Spacer(Modifier.width(1.dp))
            Button(
                onClick = { if (idx < PAGES.lastIndex) idx++ else onFinish() },
                colors = ButtonDefaults.buttonColors(containerColor = ColSaffron, contentColor = ColOnSaffron),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (idx < PAGES.lastIndex) tr("بعدی") else tr("شروع کن"), fontWeight = FontWeight.Bold) }
        }
    }
}
