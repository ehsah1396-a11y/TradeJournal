package com.example.tradejournal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(vm: TradeViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(tr("تنظیمات"), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)

        Section(tr("شخصیت‌های احساسی")) {
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

        Section(tr("ظاهر")) {
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
        }

        Section(tr("زبان")) {
            val ctx = LocalContext.current
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!Lang.en, { Lang.set(ctx, false) }, { Text(tr("فارسی")) })
                FilterChip(Lang.en, { Lang.set(ctx, true) }, { Text("English") })
            }
        }

        Section(tr("شخصی‌سازی")) {
            OutlinedTextField(
                vm.userName, { vm.setName(it) }, label = { Text(tr("نام شما")) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(8.dp))
    }
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
