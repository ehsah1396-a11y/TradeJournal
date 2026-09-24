package com.example.tradejournal

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

    fun setMode(v: Int) { mode = v; sp.edit().putInt("mode", v).apply() }
    fun setMascot(v: Int) { mascot = v; sp.edit().putInt("mascot", v).apply() }
    fun setAccent(v: Int) { accent = v; sp.edit().putInt("accent", v).apply() }
    fun setName(v: String) { userName = v; sp.edit().putString("name", v).apply() }

    val isPro = false

    fun add(t: Trade) { viewModelScope.launch { dao.insert(t) } }
    fun delete(t: Trade) { viewModelScope.launch { dao.delete(t) } }

    companion object { const val FREE_LIMIT = 30 }
}
