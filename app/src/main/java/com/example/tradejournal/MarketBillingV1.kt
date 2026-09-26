package com.example.tradejournal

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import com.android.vending.billing.IInAppBillingService
import org.json.JSONObject
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import android.util.Base64

data class MarketConfig(
    val label: String,          // فقط برای لاگ/دیباگ، مثلاً "بازار" یا "مایکت"
    val packageName: String,    // پکیج اپلیکیشنِ خودِ مارکت روی گوشی
    val bindAction: String,     // اکشن سرویس درون‌خرید همون مارکت
    val rsaPublicKeyBase64: String  // کلید عمومی RSA از پنل توسعه‌دهنده‌ی همون مارکت (برای تایید امضای خرید)
)

data class SkuInfo(val sku: String, val title: String, val price: String)
data class Purchase(val sku: String, val purchaseToken: String, val orderId: String, val raw: String, val signature: String)

sealed class BuyResult {
    data class Success(val purchase: Purchase) : BuyResult()
    object Cancelled : BuyResult()
    data class Failed(val message: String) : BuyResult()
}

/**
 * کلاینت درون‌خرید برای یک مارکت مشخص (بازار یا مایکت)، بر پایه‌ی همون AIDL نسخه‌ی ۳ کلاسیک
 * که هر دو مارکت پیاده‌سازی کرده‌اند. هیچ وابستگی خارجی (Poolakey و…) لازم ندارد.
 */
class MarketBilling(private val ctx: Context, private val config: MarketConfig) {
    private var service: IInAppBillingService? = null
    private var connecting = false
    private val pendingOnConnect = ArrayDeque<() -> Unit>()

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = IInAppBillingService.Stub.asInterface(binder)
            connecting = false
            while (pendingOnConnect.isNotEmpty()) pendingOnConnect.removeFirst().invoke()
        }
        override fun onServiceDisconnected(name: ComponentName) { service = null }
    }

    val isAvailable: Boolean get() = isMarketInstalled(ctx, config.packageName)

    fun connect(onReady: () -> Unit, onFail: () -> Unit) {
        if (!isAvailable) { onFail(); return }
        if (service != null) { onReady(); return }
        pendingOnConnect.addLast(onReady)
        if (connecting) return
        connecting = true
        try {
            val intent = Intent(config.bindAction).apply { `package` = config.packageName }
            val bound = ctx.applicationContext.bindService(intent, conn, Context.BIND_AUTO_CREATE)
            if (!bound) { connecting = false; onFail() }
        } catch (e: Exception) {
            connecting = false
            onFail()
        }
    }

    fun disconnect() {
        if (service != null) { try { ctx.applicationContext.unbindService(conn) } catch (e: Exception) { } }
        service = null
    }

    /** جزئیات قیمت واقعی محصول را از خود مارکت می‌خواند (اگر محصول در پنل ثبت شده باشد) */
    fun querySkuDetails(skus: List<String>, type: String, onResult: (Map<String, SkuInfo>) -> Unit) {
        val svc = service ?: run { onResult(emptyMap()); return }
        try {
            val skuBundle = Bundle().apply { putStringArrayList("ITEM_ID_LIST", ArrayList(skus)) }
            val result = svc.getSkuDetails(3, ctx.packageName, type, skuBundle)
            val list = result?.getStringArrayList("DETAILS_LIST") ?: ArrayList()
            val map = HashMap<String, SkuInfo>()
            for (raw in list) {
                try {
                    val o = JSONObject(raw)
                    val id = o.getString("productId")
                    map[id] = SkuInfo(id, o.optString("title", id), o.optString("price", ""))
                } catch (e: Exception) { }
            }
            onResult(map)
        } catch (e: Exception) { onResult(emptyMap()) }
    }

    /** خرید را شروع می‌کند؛ intentSender را برای startIntentSenderForResult به caller برمی‌گرداند */
    fun getBuyIntentSender(sku: String, type: String, onResult: (android.content.IntentSender?) -> Unit) {
        val svc = service ?: run { onResult(null); return }
        try {
            val bundle = svc.getBuyIntent(3, ctx.packageName, sku, type, "tj_" + System.currentTimeMillis())
            val response = bundle?.getInt("RESPONSE_CODE") ?: -1
            if (response != 0) { onResult(null); return }
            val pendingIntent = bundle.getParcelable<android.app.PendingIntent>("BUY_INTENT")
            onResult(pendingIntent?.intentSender)
        } catch (e: Exception) { onResult(null) }
    }

    /** نتیجه‌ی startIntentSenderForResult را پردازش می‌کند */
    fun handlePurchaseResult(resultCode: Int, data: Intent?): BuyResult {
        if (data == null) return BuyResult.Cancelled
        val responseCode = data.getIntExtra("RESPONSE_CODE", -1)
        if (resultCode != Activity.RESULT_OK) return BuyResult.Cancelled
        if (responseCode != 0) return BuyResult.Failed("کد خطا: $responseCode")
        val purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA") ?: return BuyResult.Failed("داده‌ی خرید خالی بود")
        val signature = data.getStringExtra("INAPP_DATA_SIGNATURE") ?: ""
        if (!verifySignature(purchaseData, signature)) return BuyResult.Failed("امضای خرید معتبر نبود")
        return try {
            val o = JSONObject(purchaseData)
            BuyResult.Success(
                Purchase(
                    sku = o.getString("productId"),
                    purchaseToken = o.getString("purchaseToken"),
                    orderId = o.optString("orderId", ""),
                    raw = purchaseData, signature = signature
                )
            )
        } catch (e: Exception) { BuyResult.Failed("پردازش پاسخ خرید شکست خورد") }
    }

    /** خریدهای فعال کاربر را برمی‌گرداند (برای بازیابی خرید همیشگی روی گوشی جدید) */
    fun queryPurchases(type: String, onResult: (List<Purchase>) -> Unit) {
        val svc = service ?: run { onResult(emptyList()); return }
        try {
            val result = svc.getPurchases(3, ctx.packageName, type, null)
            val dataList = result?.getStringArrayList("INAPP_PURCHASE_DATA_LIST") ?: ArrayList()
            val sigList = result?.getStringArrayList("INAPP_DATA_SIGNATURE_LIST") ?: ArrayList()
            val out = ArrayList<Purchase>()
            for (i in dataList.indices) {
                val raw = dataList[i]
                val sig = sigList.getOrNull(i) ?: ""
                if (!verifySignature(raw, sig)) continue
                try {
                    val o = JSONObject(raw)
                    out.add(Purchase(o.getString("productId"), o.getString("purchaseToken"), o.optString("orderId", ""), raw, sig))
                } catch (e: Exception) { }
            }
            onResult(out)
        } catch (e: Exception) { onResult(emptyList()) }
    }

    /** فقط برای محصولات مصرفی لازم است؛ اشتراک و خرید همیشگی ما مصرف نمی‌شوند */
    fun consume(purchaseToken: String) {
        try { service?.consumePurchase(3, ctx.packageName, purchaseToken) } catch (e: Exception) { }
    }

    private fun verifySignature(data: String, signature: String): Boolean {
        if (config.rsaPublicKeyBase64.isBlank()) return true // کلید هنوز تنظیم نشده؛ در پنل مارکت پر کن
        return try {
            val keyBytes = Base64.decode(config.rsaPublicKeyBase64, Base64.DEFAULT)
            val key: PublicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))
            val sig = Signature.getInstance("SHA1withRSA")
            sig.initVerify(key)
            sig.update(data.toByteArray())
            sig.verify(Base64.decode(signature, Base64.DEFAULT))
        } catch (e: Exception) { false }
    }

    companion object {
        fun isMarketInstalled(ctx: Context, pkg: String): Boolean = try {
            ctx.packageManager.getPackageInfo(pkg, 0); true
        } catch (e: Exception) { false }
    }
}
