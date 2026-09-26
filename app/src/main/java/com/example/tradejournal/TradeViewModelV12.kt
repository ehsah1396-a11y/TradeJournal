package com.example.tradejournal

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TradeViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()
    private val sp = app.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    val trades = dao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // تنظیمات (ذخیره روی خود گوشی)
    var mode by mutableIntStateOf(sp.getInt("mode", 1))       // 0 خودکار، 1 شب، 2 روز
    var mascot by mutableIntStateOf(sp.getInt("mascot", 1))   // 0 گاو، 1 گاو نر، 2 خرس، 3 خرس قطبی
    var accent by mutableIntStateOf(sp.getInt("accent", 0))
    var userName by mutableStateOf(sp.getString("name", "") ?: "")

    // شمارنده‌ی کل معاملات ثبت‌شده در طول عمر اپ؛ با حذف معامله کم نمی‌شود تا سهمیه‌ی رایگان دوباره باز نشود
    var totalAdded by mutableIntStateOf(sp.getInt("total_added", 0))
        private set
    private fun bump(n: Int) { totalAdded += n; sp.edit().putInt("total_added", totalAdded).apply() }

    @JvmName("changeMode")
    fun setMode(v: Int) { mode = v; sp.edit().putInt("mode", v).apply() }
    @JvmName("changeMascot")
    fun setMascot(v: Int) { mascot = v; sp.edit().putInt("mascot", v).apply() }
    @JvmName("changeAccent")
    fun setAccent(v: Int) { accent = v; sp.edit().putInt("accent", v).apply() }
    fun setName(v: String) { userName = v; sp.edit().putString("name", v).apply() }

    // ---------- پرو ----------
    // proUntil = 0 یعنی هیچ‌وقت پرو نبوده؛ یک زمان در آینده یعنی تا آن لحظه پرو است
    var proUntil by mutableLongStateOf(sp.getLong("proUntil", 0L))
        private set
    var ratedClaimed by mutableStateOf(sp.getBoolean("rated_claimed", false))
        private set
    var ratingValue by mutableIntStateOf(sp.getInt("rating_value", 0))
        private set

    val isPro: Boolean get() = proUntil == Long.MAX_VALUE || proUntil > System.currentTimeMillis()

    /** پرو همیشگی را فعال می‌کند (برای وقتی خرید واقعی از بازار یا مایکت اضافه شود) */
    fun grantProForever() {
        proUntil = Long.MAX_VALUE
        sp.edit().putLong("proUntil", Long.MAX_VALUE).apply()
    }

    /** بعد از ثبت امتیاز و نظر، یک هفته پرو رایگان می‌دهد (فقط یک بار) */
    fun submitRating(stars: Int, comment: String) {
        ratingValue = stars
        sp.edit().putInt("rating_value", stars).putString("rating_comment", comment).apply()
        if (!ratedClaimed) {
            val bonus = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000
            proUntil = maxOf(proUntil, bonus)
            ratedClaimed = true
            sp.edit().putLong("proUntil", proUntil).putBoolean("rated_claimed", true).apply()
        }
    }

    fun add(t: Trade) { bump(1); viewModelScope.launch { dao.insert(t) } }
    fun insertAll(list: List<Trade>) { bump(list.size); viewModelScope.launch { dao.insertAll(list) } }
    fun update(t: Trade) { viewModelScope.launch { dao.update(t) } }
    fun delete(t: Trade) { viewModelScope.launch { dao.delete(t) } }

    companion object { const val FREE_LIMIT = 30 }
}
