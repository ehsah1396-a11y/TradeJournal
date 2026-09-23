package com.example.tradejournal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TradeViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()

    val trades = dao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isPro = false

    fun add(t: Trade) { viewModelScope.launch { dao.insert(t) } }
    fun delete(t: Trade) { viewModelScope.launch { dao.delete(t) } }

    companion object { const val FREE_LIMIT = 30 }
}
