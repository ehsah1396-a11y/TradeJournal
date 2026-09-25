package com.example.tradejournal

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** نوار تبلیغاتی ثابت بالای اپ؛ فقط برای کاربر رایگان نشان داده می‌شود */
@Composable
fun AdBanner(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(ColCard2).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("📢", fontSize = 14.sp)
        Text(
            tr("جای تبلیغات — با ارتقا به پرو حذفش کن"),
            Modifier.weight(1f), color = ColMuted, fontSize = 12.sp, maxLines = 1
        )
        Text(tr("پرو"), color = ColSaffron, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun ProScreen(vm: TradeViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var stars by remember { mutableIntStateOf(vm.ratingValue.takeIf { it > 0 } ?: 5) }
    var comment by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("بازگشت")) }
            Text(tr("پرو"), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }

        if (vm.isPro) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ColCard).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("👑", fontSize = 32.sp)
                Text(tr("عضو پرو هستی"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (vm.proUntil != Long.MAX_VALUE) Text(
                    tr("اعتبار تا") + " " + java.text.SimpleDateFormat("yyyy/MM/dd").format(java.util.Date(vm.proUntil)),
                    color = ColMuted, fontSize = 12.sp
                )
            }
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ColCard).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(tr("پرو شامل چیست؟"), fontWeight = FontWeight.SemiBold)
            listOf(
                tr("ثبت نامحدود معامله"),
                tr("بدون تبلیغات"),
                tr("خروجی اکسل نامحدود")
            ).forEach { Text("• $it", fontSize = 13.sp, color = ColMuted) }
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ColCard).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(tr("خرید اشتراک پرو"), fontWeight = FontWeight.SemiBold)
            Text(tr("این بخش بعد از انتشار اپ در بازار و مایکت فعال می‌شود."), color = ColMuted, fontSize = 12.sp)
            Button(
                onClick = { },
                enabled = false,
                colors = ButtonDefaults.buttonColors(containerColor = ColSaffron, contentColor = ColOnSaffron),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text(tr("به‌زودی")) }
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ColCard).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                if (vm.ratedClaimed) tr("ممنون بابت نظرت") else tr("امتیاز بده، یک هفته پرو رایگان بگیر"),
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 1..5) {
                    Text(
                        if (i <= stars) "★" else "☆", fontSize = 26.sp, color = ColSaffron,
                        modifier = Modifier.clickable { stars = i }
                    )
                }
            }
            OutlinedTextField(
                comment, { comment = it }, label = { Text(tr("نظرت درباره ژورنال چیه؟")) },
                minLines = 2, modifier = Modifier.fillMaxWidth()
            )
            Text(
                tr("این نظر فعلاً فقط روی گوشی خودت ذخیره می‌شود."),
                color = ColMuted, fontSize = 11.sp
            )
            Button(
                onClick = {
                    vm.submitRating(stars, comment)
                    sent = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = ColSaffron, contentColor = ColOnSaffron),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (vm.ratedClaimed) tr("به‌روزرسانی نظر") else tr("ثبت و دریافت اعتبار")) }
            if (sent) Text(
                if (vm.ratedClaimed) tr("یک هفته پرو رایگان فعال شد!") else tr("نظرت ثبت شد."),
                color = ColUp, fontSize = 12.sp
            )

            Text(tr("همچنین می‌تونی از فروشگاهی که اپ رو نصب کردی امتیاز عمومی بدی:"), color = ColMuted, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { openStore(ctx, "com.farsitel.bazaar", "bazaar") }, modifier = Modifier.weight(1f)) {
                    Text(tr("امتیاز در بازار"))
                }
                OutlinedButton(onClick = { openStore(ctx, "ir.mservices.market", "myket") }, modifier = Modifier.weight(1f)) {
                    Text(tr("امتیاز در مایکت"))
                }
            }
        }
    }
}

private fun openStore(ctx: android.content.Context, pkg: String, kind: String) {
    val uri = if (kind == "bazaar") Uri.parse("bazaar://details?id=" + ctx.packageName)
    else Uri.parse("myket://comment?id=" + ctx.packageName)
    try {
        val intent = Intent(Intent.ACTION_VIEW, uri).setPackage(pkg)
        ctx.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // اپ روی بازار یا مایکت هنوز منتشر نشده؛ فعلاً کاری انجام نمی‌شود
    }
}
