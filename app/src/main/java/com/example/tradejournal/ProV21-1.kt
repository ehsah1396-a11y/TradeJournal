package com.example.tradejournal

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
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
    val billing = remember { BillingManager(ctx) }
    var buying by remember { mutableStateOf<String?>(null) } // sku در حال خرید، برای نمایش لودینگ
    var buyError by remember { mutableStateOf<String?>(null) }
    var justPurchased by remember { mutableStateOf(false) }
    val prices by billing.skuPrices

    // بعد از رفتن به فروشگاه برای امتیازدهی، وقتی کاربر برگرده اپ، ازش می‌پرسیم که ثبت کرد یا نه
    var awaitingRatingConfirm by rememberSaveable { mutableStateOf(false) }
    var showRatingConfirm by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && awaitingRatingConfirm) showRatingConfirm = true
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val sku = buying
        buying = null
        billing.handleResult(result.resultCode, result.data) { r ->
            when (r) {
                is BuyResult.Success -> {
                    if (r.purchase.sku == BillingProducts.LIFETIME_SKU) vm.grantProForever() else vm.grantProMonthly()
                    justPurchased = true
                }
                is BuyResult.Cancelled -> { }
                is BuyResult.Failed -> buyError = r.message
            }
        }
    }

    fun startPurchase(sku: String) {
        buyError = null
        buying = sku
        val onLaunch: (android.content.IntentSender) -> Unit = { sender ->
            launcher.launch(IntentSenderRequest.Builder(sender).build())
        }
        val onFail: (String) -> Unit = { msg -> buying = null; buyError = msg }
        if (sku == BillingProducts.MONTHLY_SKU) billing.purchaseMonthly(onLaunch, onFail)
        else billing.purchaseLifetime(onLaunch, onFail)
    }

    LaunchedEffect(Unit) {
        billing.refreshPrices()
        billing.restore { found ->
            if (found.any { it.sku == BillingProducts.LIFETIME_SKU }) vm.grantProForever()
            else if (found.any { it.sku == BillingProducts.MONTHLY_SKU }) vm.grantProMonthly()
        }
    }

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

        // ---------- خرید واقعی: ماهانه + همیشگی ----------
        if (!vm.isPro) Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ColCard).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(tr("خرید اشتراک پرو"), fontWeight = FontWeight.SemiBold)

            // همیشگی اول و با نشان «پیشنهاد ویژه» چون از نظر قیمتی لنگر خوبیه (کمتر از ۶ ماه، برای همیشه)
            PlanCard(
                title = tr("همیشگی"),
                price = prices[BillingProducts.LIFETIME_SKU] ?: BillingProducts.LIFETIME_FALLBACK_PRICE,
                badge = tr("پیشنهاد ویژه"),
                note = tr("یک‌بار پرداخت، برای همیشه — کمتر از ۶ ماه اشتراک ماهانه"),
                loading = buying == BillingProducts.LIFETIME_SKU,
                onClick = { startPurchase(BillingProducts.LIFETIME_SKU) }
            )
            PlanCard(
                title = tr("ماهانه"),
                price = prices[BillingProducts.MONTHLY_SKU] ?: BillingProducts.MONTHLY_FALLBACK_PRICE,
                badge = null,
                note = tr("هر ماه تمدید می‌شود، هر وقت خواستی لغوش کن"),
                loading = buying == BillingProducts.MONTHLY_SKU,
                onClick = { startPurchase(BillingProducts.MONTHLY_SKU) }
            )

            if (buyError != null) Text(buyError!!, color = ColDn, fontSize = 12.sp)
            if (justPurchased) Text(tr("خرید با موفقیت انجام شد، پرو فعال شد!"), color = ColUp, fontSize = 12.sp)
            Text(
                tr("پرداخت از طریق همون فروشگاهی انجام می‌شه که اپ رو ازش نصب کردی (بازار یا مایکت)."),
                color = ColMuted, fontSize = 11.sp
            )
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ColCard).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                if (vm.ratedClaimed) tr("ممنون بابت امتیازت") else tr("امتیاز بده، یک هفته پرو رایگان بگیر"),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                tr("برای دریافت اعتبار، از فروشگاهی که اپ رو نصب کردی امتیاز عمومی بده. نظرت رو همون‌جا برای بقیه‌ی کاربرها می‌نویسی."),
                color = ColMuted, fontSize = 12.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { awaitingRatingConfirm = true; openStore(ctx, "com.farsitel.bazaar", "bazaar") },
                    modifier = Modifier.weight(1f)
                ) { Text(tr("امتیاز در بازار")) }
                OutlinedButton(
                    onClick = { awaitingRatingConfirm = true; openStore(ctx, "ir.mservices.market", "myket") },
                    modifier = Modifier.weight(1f)
                ) { Text(tr("امتیاز در مایکت")) }
            }
            if (vm.ratedClaimed) Text(tr("یک هفته پرو رایگان فعال شد!"), color = ColUp, fontSize = 12.sp)
        }
    }

    if (showRatingConfirm) AlertDialog(
        onDismissRequest = { showRatingConfirm = false; awaitingRatingConfirm = false },
        title = { Text(tr("نظرت رو ثبت کردی؟")) },
        text = { Text(tr("اگه تو فروشگاه امتیاز و نظرت رو ثبت کردی، بزن تا یک هفته پرو رایگانت فعال بشه.")) },
        confirmButton = {
            TextButton({
                vm.submitRating(5, "")
                showRatingConfirm = false
                awaitingRatingConfirm = false
            }) { Text(tr("بله، ثبت کردم")) }
        },
        dismissButton = {
            TextButton({ showRatingConfirm = false; awaitingRatingConfirm = false }) { Text(tr("هنوز نه")) }
        }
    )
}

@Composable
private fun PlanCard(title: String, price: String, badge: String?, note: String, loading: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(ColCard2)
            .border(if (badge != null) 2.dp else 0.dp, ColSaffron, RoundedCornerShape(14.dp))
            .clickable(enabled = !loading, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (badge != null) Text(
                    badge, color = ColOnSaffron, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ColSaffron).padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = ColSaffron)
            else Text(price, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = ColSaffron)
        }
        Text(note, color = ColMuted, fontSize = 11.sp)
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
