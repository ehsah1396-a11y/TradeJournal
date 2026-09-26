package com.example.tradejournal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * تنظیمات الان فقط یک منوست: هر ردیف (ظاهر و شخصیت، زبان، شخصی‌سازی، قفل و امنیت) دقیقاً
 * مثل ردیف امنیت قبلی، سربرگ و صفحه‌ی مستقل خودش را دارد و با لمس، وارد همان صفحه می‌شوی.
 * ویرایش و «ذخیره تغییرات» داخل هر صفحه‌ی مستقل انجام می‌شود، نه اینجا.
 */
@Composable
fun SettingsScreen(
    vm: TradeViewModel,
    onOpenPro: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenPersonalization: () -> Unit
) {
    val ctx = LocalContext.current
    val lockNames = listOf(tr("بدون قفل"), tr("پین"), tr("الگو"), tr("اثر انگشت"))
    val modeNames = listOf(tr("خودکار"), tr("شب"), tr("روز"))

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(tr("تنظیمات"), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)

        // ---------- وضعیت پرو ----------
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(if (vm.isPro) ColCard else ColSaffron.copy(alpha = 0.16f))
                .clickable { onOpenPro() }.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(if (vm.isPro) tr("عضو پرو هستی") else tr("پرو و امتیازدهی"), fontWeight = FontWeight.SemiBold)
            Text(
                if (vm.isPro) tr("همه‌ی امکانات باز است، ممنون که همراه ما هستی")
                else tr("امتیاز بده و یک هفته پرو رایگان بگیر"),
                color = ColMuted, fontSize = 12.sp
            )
        }

        MenuRow(
            icon = "🎨", title = tr("ظاهر و شخصیت"),
            subtitle = modeNames.getOrElse(vm.mode) { modeNames[0] } + " · " + MASCOT_NAMES.getOrElse(vm.mascot) { "" },
            onClick = onOpenAppearance
        )
        MenuRow(
            icon = "🌐", title = tr("زبان"),
            subtitle = if (Lang.en) "English" else tr("فارسی"),
            onClick = onOpenLanguage
        )
        MenuRow(
            icon = "✍️", title = tr("شخصی‌سازی"),
            subtitle = vm.userName.ifBlank { tr("تریدر") },
            onClick = onOpenPersonalization
        )
        MenuRow(
            icon = "🔒", title = tr("قفل و امنیت"),
            subtitle = tr("وضعیت فعلی: ") + lockNames.getOrElse(LockState.mode(ctx)) { lockNames[0] },
            onClick = onOpenSecurity
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MenuRow(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ColCard)
            .clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(icon, fontSize = 15.sp)
                Text(title, fontWeight = FontWeight.SemiBold)
            }
            Text("›", color = ColMuted, fontSize = 16.sp)
        }
        Text(subtitle, color = ColMuted, fontSize = 12.sp)
    }
}
