package com.example.tradejournal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SecurityScreen() {
    val ctx = LocalContext.current
    var mode by remember { mutableIntStateOf(LockState.mode(ctx)) }
    var setup by remember { mutableIntStateOf(0) }

    if (setup != 0) {
        LockOverlay(
            mode = setup, setup = true,
            onDone = { secret -> LockState.save(ctx, setup, secret); mode = setup; setup = 0 },
            onCancel = { setup = 0 }
        )
    } else {
        val opts = listOf(
            0 to tr("بدون قفل"),
            1 to tr("پین ۴ رقمی"),
            2 to tr("الگو"),
            3 to tr("اثر انگشت (با پین پشتیبان)")
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(tr("قفل برنامه"), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(tr("خودت انتخاب کن که ژورنالت با چه روشی قفل بشه."), color = ColMuted, fontSize = 12.sp)
            opts.forEach { (m, name) ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(if (mode == m) ColCard2 else ColCard)
                        .border(if (mode == m) 2.dp else 0.dp, ColSaffron, RoundedCornerShape(16.dp))
                        .clickable { if (m == 0) { LockState.disable(ctx); mode = 0 } else setup = m }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    if (mode == m) Text("✓", color = ColSaffron, fontSize = 18.sp)
                }
            }
            if (mode != 0) {
                Button(
                    onClick = { LockState.locked = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ColSaffron, contentColor = ColOnSaffron),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text(tr("همین الان قفل کن و امتحان کن"), fontWeight = FontWeight.ExtraBold) }
            }
            Text(
                tr("توجه: پین و الگو فقط روی همین گوشی و به‌صورت رمزنگاری‌شده ذخیره می‌شن. ") +
                        tr("اگه فراموششون کنی راه بازیابی نیست و باید اطلاعات اپ از تنظیمات گوشی پاک بشه."),
                color = ColMuted, fontSize = 11.sp
            )
        }
    }
}
