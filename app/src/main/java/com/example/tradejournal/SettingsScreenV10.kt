package com.example.tradejournal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(vm: TradeViewModel, onOpenPro: () -> Unit, onOpenSecurity: () -> Unit) {
    val ctx = LocalContext.current
    var confirmed by remember { mutableStateOf(false) }
    val lockNames = listOf(tr("بدون قفل"), tr("پین"), tr("الگو"), tr("اثر انگشت"))

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

        // ---------- ظاهر و شخصیت ----------
        Section(tr("ظاهر و شخصیت")) {
            Text(tr("حالت نمایش"), color = ColMuted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(tr("خودکار"), tr("شب"), tr("روز")).forEachIndexed { i, t ->
                    FilterChip(vm.mode == i, { vm.setMode(i) }, { Text(t) })
                }
            }
            Text(tr("رنگ اصلی"), color = ColMuted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ACCENTS.forEachIndexed { i, c ->
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(c)
                            .border(if (vm.accent == i) 3.dp else 0.dp, ColText, CircleShape)
                            .clickable { vm.setAccent(i) }
                    )
                }
            }
            Text(tr("شخصیت‌های احساسی"), color = ColMuted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MASCOT_NAMES.forEachIndexed { k, name ->
                    val on = vm.mascot == k
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(ColCard2)
                            .border(if (on) 2.dp else 0.dp, ColSaffron, RoundedCornerShape(16.dp))
                            .clickable { vm.setMascot(k) }.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Mascot(k, 0, 52.dp)
                        Text(name, fontSize = 11.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                for (m in 0..3) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Mascot(vm.mascot, m, 46.dp)
                        Text(MOOD_NAMES[m], fontSize = 10.sp, color = ColMuted)
                    }
                }
            }
        }

        // ---------- زبان ----------
        Section(tr("زبان")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!Lang.en, { Lang.set(ctx, false) }, { Text(tr("فارسی")) })
                FilterChip(Lang.en, { Lang.set(ctx, true) }, { Text("English") })
            }
        }

        // ---------- شخصی‌سازی ----------
        Section(tr("شخصی‌سازی")) {
            OutlinedTextField(
                vm.userName, { vm.setName(it) }, label = { Text(tr("نام شما")) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }

        // ---------- امنیت ----------
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ColCard)
                .clickable { onOpenSecurity() }.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr("قفل و امنیت"), fontWeight = FontWeight.SemiBold)
                Text("›", color = ColMuted, fontSize = 16.sp)
            }
            Text(
                tr("وضعیت فعلی: ") + lockNames.getOrElse(LockState.mode(ctx)) { lockNames[0] },
                color = ColMuted, fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { confirmed = true },
            colors = ButtonDefaults.buttonColors(containerColor = ColSaffron, contentColor = ColOnSaffron),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text(tr("ذخیره تغییرات"), fontWeight = FontWeight.ExtraBold) }
        Spacer(Modifier.height(8.dp))
    }

    if (confirmed) AlertDialog(
        onDismissRequest = { confirmed = false },
        confirmButton = { TextButton({ confirmed = false }) { Text(tr("باشه")) } },
        title = { Text(tr("ذخیره شد")) },
        text = { Text(tr("تغییرات با موفقیت اعمال شد.")) }
    )
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ColCard).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        content()
    }
}
