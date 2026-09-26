package com.example.tradejournal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * صفحه‌ی «قفل و امنیت». قبلاً این تابع فقط از SettingsScreen و MainScreen صدا زده می‌شد
 * ولی خودِ کامپوننتش جایی تعریف نشده بود (خطای کامپایل) — همان چیزی که باعث می‌شد این
 * بخش اصلاً کار نکند. همون الگوی «انتخاب پیش‌نویس، بعد ذخیره» که در تنظیمات هست اینجا هم
 * پیاده شده: روش قفل را انتخاب می‌کنی (پیش‌نمایش لحظه‌ای همون‌جا روی کارت نشان داده می‌شود)،
 * و با زدن «ذخیره تغییرات» ویزارد ساخت پین/الگو/اثرانگشت باز می‌شود؛ تا آن لحظه قفل واقعی
 * دست‌نخورده می‌ماند.
 */
@Composable
fun SecurityScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var currentMode by remember { mutableIntStateOf(LockState.mode(ctx)) }
    var draftMode by remember { mutableIntStateOf(currentMode) }
    var showWizard by remember { mutableStateOf(false) }
    var showTest by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val dirty = draftMode != currentMode

    val options = listOf(
        0 to tr("بدون قفل"),
        1 to tr("پین"),
        2 to tr("الگو"),
        3 to tr("اثر انگشت (با پین پشتیبان)")
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("بازگشت")) }
            Text(tr("قفل و امنیت"), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }

        // ---------- پیش‌نمایش لحظه‌ای انتخاب فعلی ----------
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ColCard).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Mascot(LocalMascot.current, if (draftMode == 0) 1 else 0, 56.dp)
            Text(tr("قفل برنامه"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(tr("خودت انتخاب کن که ژورنالت با چه روشی قفل بشه."), color = ColMuted, fontSize = 12.sp)
        }

        // ---------- انتخاب روش قفل (پیش‌نویس) ----------
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ColCard).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                tr("وضعیت فعلی: ") + (options.firstOrNull { it.first == currentMode }?.second ?: options[0].second),
                color = ColMuted, fontSize = 12.sp
            )
            options.forEach { (idx, label) ->
                val on = draftMode == idx
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (on) ColSaffron.copy(alpha = 0.16f) else ColCard2)
                        .clickable { draftMode = idx }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RadioButton(
                        selected = on, onClick = { draftMode = idx },
                        colors = RadioButtonDefaults.colors(selectedColor = ColSaffron)
                    )
                    Text(label, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            if (draftMode != 0) Text(
                tr("توجه: پین و الگو فقط روی همین گوشی و به‌صورت رمزنگاری‌شده ذخیره می‌شن. ") +
                    tr("اگه فراموششون کنی راه بازیابی نیست و باید اطلاعات اپ از تنظیمات گوشی پاک بشه."),
                color = ColMuted, fontSize = 11.sp
            )
        }

        if (dirty) Text(tr("تغییراتی داری که هنوز ذخیره نشده."), color = ColSaffron, fontSize = 12.sp)

        Button(
            onClick = {
                if (draftMode == 0) {
                    LockState.disable(ctx)
                    currentMode = 0
                    saved = true
                } else {
                    showWizard = true
                }
            },
            enabled = dirty,
            colors = ButtonDefaults.buttonColors(containerColor = ColSaffron, contentColor = ColOnSaffron),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text(tr("ذخیره تغییرات"), fontWeight = FontWeight.ExtraBold) }

        if (currentMode != 0 && !dirty) OutlinedButton(
            onClick = { showTest = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text(tr("همین الان قفل کن و امتحان کن")) }

        Spacer(Modifier.height(8.dp))
    }

    // ساخت/تایید رمز جدید؛ فقط با زدن «ذخیره تغییرات» باز می‌شود، پس تا اینجا هیچ اثری
    // روی قفل واقعی گذاشته نشده است.
    if (showWizard) LockOverlay(
        mode = draftMode, setup = true,
        onDone = { secret ->
            LockState.save(ctx, draftMode, secret)
            currentMode = draftMode
            showWizard = false
            saved = true
        },
        onCancel = { showWizard = false }
    )

    if (showTest) LockOverlay(
        mode = currentMode, setup = false,
        onDone = { showTest = false },
        onCancel = { showTest = false }
    )

    if (saved) AlertDialog(
        onDismissRequest = { saved = false },
        confirmButton = { TextButton({ saved = false }) { Text(tr("باشه")) } },
        title = { Text(tr("ذخیره شد")) },
        text = { Text(tr("تغییرات با موفقیت اعمال شد.")) }
    )
}
