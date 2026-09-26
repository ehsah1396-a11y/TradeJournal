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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** آیا راهنمای اولین اجرا قبلاً دیده شده */
object Onboarding {
    private fun sp(c: Context) = c.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    fun done(c: Context): Boolean = sp(c).getBoolean("onboarding_done", false)
    fun setDone(c: Context) { sp(c).edit().putBoolean("onboarding_done", true).apply() }
}

data class OnbStep(val screen: Int, val emoji: String, val title: String, val desc: String)

val ONB_STEPS = listOf(
    OnbStep(0, "👋", "خانه", "خلاصه سود، وین‌ریت و بازدهی هر استراتژی رو همین‌جا می‌بینی."),
    OnbStep(1, "📒", "معاملات", "هر معامله رو با عکس چارت، ویس و احساست ثبت کن، یا از متاتریدر وارد کن. با لمس هر معامله می‌تونی ویرایشش کنی."),
    OnbStep(4, "📅", "تقویم", "سود و زیان هر روز رو روی تقویم شمسی می‌بینی."),
    OnbStep(3, "⚙️", "تنظیمات", "ظاهر، زبان، شخصیت‌ها و قفل امنیتی همه از همین‌جا تنظیم می‌شن.")
)

/** کارت راهنما که روی صفحه‌ی واقعی می‌شینه و کاربر رو مرحله‌به‌مرحله بین بخش‌ها می‌بره */
@Composable
fun OnboardingOverlay(step: Int, onNext: () -> Unit, onSkip: () -> Unit) {
    val page = ONB_STEPS[step]
    Column(
        Modifier.fillMaxWidth().padding(14.dp)
            .clip(RoundedCornerShape(20.dp)).background(ColCard)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(page.emoji, fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Text(tr(page.title), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            TextButton(onSkip) { Text(tr("رد شدن"), color = ColMuted, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(6.dp))
        Text(tr(page.desc), color = ColMuted, fontSize = 13.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ONB_STEPS.indices.forEach { i ->
                    Box(
                        Modifier.size(if (i == step) 9.dp else 7.dp).clip(CircleShape)
                            .background(if (i == step) ColSaffron else ColCard2)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = ColSaffron, contentColor = ColOnSaffron),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (step < ONB_STEPS.lastIndex) tr("بعدی") else tr("شروع کن"), fontWeight = FontWeight.Bold) }
        }
    }
}
