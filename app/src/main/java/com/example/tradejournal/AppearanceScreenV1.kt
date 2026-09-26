package com.example.tradejournal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * سربرگ مستقل «ظاهر و شخصیت» (حالت نمایش، رنگ اصلی، شخصیت احساسی).
 * دقیقاً همون الگوی SecurityScreen: پیش‌نویس جدا، کل صفحه با تم پیش‌نویس رندر می‌شود
 * (پیش‌نمایش کاملاً زنده)، و فقط با «ذخیره تغییرات» روی کل اپ اعمال می‌شود.
 */
@Composable
fun AppearanceScreen(vm: TradeViewModel, onBack: () -> Unit) {
    var draftMode by remember { mutableIntStateOf(vm.mode) }
    var draftAccent by remember { mutableIntStateOf(vm.accent) }
    var draftMascot by remember { mutableIntStateOf(vm.mascot) }
    var confirmed by remember { mutableStateOf(false) }
    val dirty = draftMode != vm.mode || draftAccent != vm.accent || draftMascot != vm.mascot

    AppTheme(draftMode, draftAccent, draftMascot) {
        Box(Modifier.fillMaxSize().background(ColBg)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("بازگشت")) }
                    Text(tr("ظاهر و شخصیت"), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }

                Category("🌗", tr("حالت نمایش")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(tr("خودکار"), tr("شب"), tr("روز")).forEachIndexed { i, t ->
                            FilterChip(draftMode == i, { draftMode = i }, { Text(t) })
                        }
                    }
                }

                Category("🎨", tr("رنگ اصلی")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ACCENTS.forEachIndexed { i, c ->
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).background(c)
                                    .border(if (draftAccent == i) 3.dp else 0.dp, ColText, CircleShape)
                                    .clickable { draftAccent = i }
                            )
                        }
                    }
                }

                Category("🐻", tr("شخصیت‌های احساسی")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MASCOT_NAMES.forEachIndexed { k, name ->
                            val on = draftMascot == k
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(ColCard2)
                                    .border(if (on) 2.dp else 0.dp, ColSaffron, RoundedCornerShape(16.dp))
                                    .clickable { draftMascot = k }.padding(vertical = 8.dp),
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
                                Mascot(draftMascot, m, 46.dp)
                                Text(MOOD_NAMES[m], fontSize = 10.sp, color = ColMuted)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                if (dirty) Text(tr("تغییراتی داری که هنوز ذخیره نشده."), color = ColSaffron, fontSize = 12.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            vm.setMode(draftMode); vm.setAccent(draftAccent); vm.setMascot(draftMascot)
                            confirmed = true
                        },
                        enabled = dirty,
                        colors = ButtonDefaults.buttonColors(containerColor = ColSaffron, contentColor = ColOnSaffron),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) { Text(tr("ذخیره تغییرات"), fontWeight = FontWeight.ExtraBold) }

                    if (dirty) TextButton(onClick = {
                        draftMode = vm.mode; draftAccent = vm.accent; draftMascot = vm.mascot
                    }, modifier = Modifier.height(50.dp)) { Text(tr("رها کردن تغییرات")) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (confirmed) AlertDialog(
        onDismissRequest = { confirmed = false },
        confirmButton = { TextButton({ confirmed = false }) { Text(tr("باشه")) } },
        title = { Text(tr("ذخیره شد")) },
        text = { Text(tr("تغییرات با موفقیت اعمال شد.")) }
    )
}

/** کارت دسته‌بندی‌شده با آیکون و عنوان کوچک. */
@Composable
private fun Category(icon: String, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ColCard).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(icon, fontSize = 15.sp)
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        content()
    }
}
