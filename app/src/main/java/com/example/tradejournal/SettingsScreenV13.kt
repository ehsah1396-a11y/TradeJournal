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
import androidx.compose.runtime.mutableIntStateOf
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

/**
 * تنظیمات به‌صورت پیش‌نویس نگه‌داری می‌شود، اما برخلاف نسخه‌ی قبل، پیش‌نویس فقط «کور» نیست:
 * کارت «پیش‌نمایش» با همون AppTheme ولی روی مقادیر پیش‌نویس رندر می‌شود، پس رنگ/حالت شب-روز/
 * شخصیت انتخابی را همین الان می‌بینی. چیزی که واقعاً روی کل اپ اثر می‌گذارد، فقط با زدن
 * «ذخیره تغییرات» اعمال می‌شود؛ با «رها کردن تغییرات» هم می‌شود بی‌خیال پیش‌نویس شد.
 */
@Composable
fun SettingsScreen(vm: TradeViewModel, onOpenPro: () -> Unit, onOpenSecurity: () -> Unit) {
    val ctx = LocalContext.current
    var draftMode by remember { mutableIntStateOf(vm.mode) }
    var draftAccent by remember { mutableIntStateOf(vm.accent) }
    var draftMascot by remember { mutableIntStateOf(vm.mascot) }
    var draftName by remember { mutableStateOf(vm.userName) }
    var draftEn by remember { mutableStateOf(Lang.en) }
    var confirmed by remember { mutableStateOf(false) }
    val lockNames = listOf(tr("بدون قفل"), tr("پین"), tr("الگو"), tr("اثر انگشت"))
    val dirty = draftMode != vm.mode || draftAccent != vm.accent || draftMascot != vm.mascot ||
            draftName != vm.userName || draftEn != Lang.en
    val appearanceDirty = draftMode != vm.mode || draftAccent != vm.accent || draftMascot != vm.mascot

    fun discard() {
        draftMode = vm.mode; draftAccent = vm.accent; draftMascot = vm.mascot
        draftName = vm.userName; draftEn = Lang.en
    }

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
                    FilterChip(draftMode == i, { draftMode = i }, { Text(t) })
                }
            }
            Text(tr("رنگ اصلی"), color = ColMuted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ACCENTS.forEachIndexed { i, c ->
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(c)
                            .border(if (draftAccent == i) 3.dp else 0.dp, ColText, CircleShape)
                            .clickable { draftAccent = i }
                    )
                }
            }
            Text(tr("شخصیت‌های احساسی"), color = ColMuted, fontSize = 12.sp)
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
        }

        // ---------- پیش‌نمایش زنده‌ی ظاهر (روی مقادیر پیش‌نویس، نه چیزی که هنوز ذخیره شده) ----------
        if (appearanceDirty) AppTheme(draftMode, draftAccent, draftMascot) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ColCard)
                    .border(1.dp, ColSaffron.copy(alpha = 0.5f), RoundedCornerShape(18.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(tr("پیش‌نمایش") + " 👁", color = ColMuted, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Mascot(draftMascot, 0, 44.dp)
                    Column {
                        Text(tr("سلام، ") + draftName.ifBlank { tr("تریدر") }, fontWeight = FontWeight.Bold, color = ColText)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(tr("خرید"), color = ColUp, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(tr("فروش"), color = ColDn, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("●", color = ColSaffron, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // ---------- زبان (پیش‌نویس) ----------
        Section(tr("زبان")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!draftEn, { draftEn = false }, { Text(tr("فارسی")) })
                FilterChip(draftEn, { draftEn = true }, { Text("English") })
            }
        }

        // ---------- شخصی‌سازی (پیش‌نویس) ----------
        Section(tr("شخصی‌سازی")) {
            OutlinedTextField(
                draftName, { draftName = it }, label = { Text(tr("نام شما")) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }

        // ---------- امنیت (فوری، چون خودش صفحه‌ی جدا دارد) ----------
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
        if (dirty) Text(tr("تغییراتی داری که هنوز ذخیره نشده."), color = ColSaffron, fontSize = 12.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    vm.setMode(draftMode); vm.setAccent(draftAccent); vm.setMascot(draftMascot); vm.setName(draftName)
                    if (draftEn != Lang.en) Lang.set(ctx, draftEn)
                    confirmed = true
                },
                enabled = dirty,
                colors = ButtonDefaults.buttonColors(containerColor = ColSaffron, contentColor = ColOnSaffron),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) { Text(tr("ذخیره تغییرات"), fontWeight = FontWeight.ExtraBold) }

            if (dirty) TextButton(onClick = { discard() }, modifier = Modifier.height(50.dp)) {
                Text(tr("رها کردن تغییرات"))
            }
        }
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
