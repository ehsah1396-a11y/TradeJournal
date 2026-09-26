// این همون اینترفیس نسخه‌ی ۳ درون‌خرید گوگل‌پلی است که هم بازار (روش کلاسیک، بدون Poolakey)
// و هم مایکت به‌صورت سازگار پیاده‌سازی کرده‌اند (سرویس هر دو از همین متدها جواب می‌دن، فقط
// پکیج و آدرس بایند فرق داره) — به همین دلیل یک کد واحد برای هر دو مارکت کار می‌کنه.

package com.android.vending.billing;

interface IInAppBillingService {
    int isBillingSupported(int apiVersion, String packageName, String type);

    Bundle getSkuDetails(int apiVersion, String packageName, String type, in Bundle skusBundle);

    Bundle getBuyIntent(int apiVersion, String packageName, String sku, String type, String developerPayload);

    Bundle getPurchases(int apiVersion, String packageName, String type, String continuationToken);

    int consumePurchase(int apiVersion, String packageName, String purchaseToken);
}
