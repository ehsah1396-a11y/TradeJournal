package com.example.tradejournal

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import androidx.compose.runtime.mutableStateOf

object BillingProducts {
    const val MONTHLY_SKU = "pro_monthly"
    const val LIFETIME_SKU = "pro_lifetime"

    // این دو عدد فقط «پیش‌فرضِ نمایشی» هستن، برای وقتی که هنوز نتونستیم قیمت واقعی رو از خود
    // مارکت بخونیم (مثلاً محصول تازه در پنل ثبت شده و کش نشده). قیمتِ واقعی و رسمی همیشه همونیه
    // که در پنل بازار/مایکت (به تومان) ثبت می‌کنی؛ این دو رو فقط هماهنگ با همون عدد نگه‌دار.
    //
    // منطق قیمت‌گذاری (روان‌شناسی قیمت):
    // • ماهانه ۷۹,۰۰۰ تومان — زیر مرز روانیِ ۸۰,۰۰۰ (چارم پرایسینگ)، ورودی کم‌ریسک برای تصمیم سریع.
    // • همیشگی ۴۹۰,۰۰۰ تومان — زیر مرز روانیِ ۵۰۰,۰۰۰، و عمداً طوری چیده شده که تقریباً معادل
    //   ۶ ماه اشتراک ماهانه باشه (۷۹,۰۰۰ × ۶ ≈ ۴۷۴,۰۰۰). وقتی این دو گزینه کنار هم دیده می‌شن،
    //   ماهانه نقش «لنگر قیمتی» رو بازی می‌کنه و همیشگی به‌وضوح «معامله‌ی بهتر» به نظر می‌رسه —
    //   بدون اینکه ماهانه رو حذف یا بی‌ارزش کنیم (کسی که مطمئن نیست باز هم می‌تونه ماهانه بخره).
    const val MONTHLY_FALLBACK_PRICE = "۷۹,۰۰۰ تومان"
    const val LIFETIME_FALLBACK_PRICE = "۴۹۰,۰۰۰ تومان"
}

private val BAZAAR_CONFIG = MarketConfig(
    label = "بازار",
    packageName = "com.farsitel.bazaar",
    bindAction = "ir.cafebazaar.pardakht.InAppBillingService.BIND",
    rsaPublicKeyBase64 = "" // از پنل توسعه‌دهنده‌ی بازار → برنامه‌ات → «کلید عمومی RSA» کپی و اینجا بچسبون
)

private val MYKET_CONFIG = MarketConfig(
    label = "مایکت",
    packageName = "ir.mservices.market",
    bindAction = "ir.mservices.market.InAppBillingService.BIND",
    rsaPublicKeyBase64 = "" // از پنل توسعه‌دهنده‌ی مایکت → برنامه‌ات → بخش RSA کپی و اینجا بچسبون
)

/**
 * چون نمی‌دونیم اپ نهایی از بازار نصب شده یا مایکت، هر دو کلاینت رو نگه می‌داریم و بر اساس
 * «از کدوم مارکت نصب شده» (یا هر کدوم که روی گوشی موجود بود) یکی رو در زمان اجرا انتخاب می‌کنیم.
 * محصولات (pro_monthly با نوع «اشتراک» و pro_lifetime با نوع «خرید یک‌باره») باید با همین دو
 * شناسه، در پنل هر دو مارکت به‌صورت جداگانه ساخته و قیمت‌گذاری بشن.
 */
class BillingManager(ctx: Context) {
    private val app = ctx.applicationContext
    private val bazaar = MarketBilling(app, BAZAAR_CONFIG)
    private val myket = MarketBilling(app, MYKET_CONFIG)

    var skuPrices = mutableStateOf(
        mapOf(
            BillingProducts.MONTHLY_SKU to BillingProducts.MONTHLY_FALLBACK_PRICE,
            BillingProducts.LIFETIME_SKU to BillingProducts.LIFETIME_FALLBACK_PRICE
        )
    )
        private set

    private fun activeMarket(): MarketBilling? {
        val installer = try {
            if (Build.VERSION.SDK_INT >= 30)
                app.packageManager.getInstallSourceInfo(app.packageName).installingPackageName
            else @Suppress("DEPRECATION") app.packageManager.getInstallerPackageName(app.packageName)
        } catch (e: Exception) { null }
        return when {
            installer == BAZAAR_CONFIG.packageName -> bazaar
            installer == MYKET_CONFIG.packageName -> myket
            bazaar.isAvailable -> bazaar
            myket.isAvailable -> myket
            else -> null
        }
    }

    /** موقع باز شدن صفحه‌ی پرو صدا زده می‌شود: وصل می‌شود و قیمت واقعیِ ثبت‌شده در پنل را می‌خواند */
    fun refreshPrices() {
        val m = activeMarket() ?: return
        m.connect(onReady = {
            m.querySkuDetails(listOf(BillingProducts.MONTHLY_SKU), "subs") { subs ->
                m.querySkuDetails(listOf(BillingProducts.LIFETIME_SKU), "inapp") { inapp ->
                    val cur = skuPrices.value.toMutableMap()
                    subs[BillingProducts.MONTHLY_SKU]?.let { if (it.price.isNotBlank()) cur[BillingProducts.MONTHLY_SKU] = it.price }
                    inapp[BillingProducts.LIFETIME_SKU]?.let { if (it.price.isNotBlank()) cur[BillingProducts.LIFETIME_SKU] = it.price }
                    skuPrices.value = cur
                }
            }
        }, onFail = { })
    }

    fun purchaseMonthly(onLaunch: (IntentSender) -> Unit, onFail: (String) -> Unit) =
        purchase(BillingProducts.MONTHLY_SKU, "subs", onLaunch, onFail)

    fun purchaseLifetime(onLaunch: (IntentSender) -> Unit, onFail: (String) -> Unit) =
        purchase(BillingProducts.LIFETIME_SKU, "inapp", onLaunch, onFail)

    private fun purchase(sku: String, type: String, onLaunch: (IntentSender) -> Unit, onFail: (String) -> Unit) {
        val m = activeMarket() ?: run { onFail(tr("اپ از بازار یا مایکت نصب نشده، پس خرید ممکن نیست.")); return }
        m.connect(onReady = {
            m.getBuyIntentSender(sku, type) { sender ->
                if (sender != null) onLaunch(sender)
                else onFail(tr("شروع خرید ممکن نشد. مطمئن شو این محصول در پنل فروشگاه ساخته و منتشر شده."))
            }
        }, onFail = { onFail(tr("اتصال به فروشگاه برقرار نشد.")) })
    }

    /** نتیجه‌ی startIntentSenderForResult را با همان مارکتی که خرید از آن شروع شده پردازش می‌کند */
    fun handleResult(resultCode: Int, data: Intent?, onDone: (BuyResult) -> Unit) {
        val m = activeMarket() ?: run { onDone(BuyResult.Failed(tr("فروشگاه فعال پیدا نشد"))); return }
        onDone(m.handlePurchaseResult(resultCode, data))
    }

    /** موقع باز شدن صفحه‌ی پرو صدا زده می‌شود تا خرید همیشگی/اشتراک روی گوشی جدید یا بعد از حذف‌نصب برگردد */
    fun restore(onFound: (List<Purchase>) -> Unit) {
        val m = activeMarket() ?: run { onFound(emptyList()); return }
        m.connect(onReady = {
            m.queryPurchases("inapp") { inapp ->
                m.queryPurchases("subs") { subs -> onFound(inapp + subs) }
            }
        }, onFail = { onFound(emptyList()) })
    }
}
