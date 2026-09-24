package com.example.tradejournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Lang.load(this)
        if (savedInstanceState == null) LockState.lockNow(this)   // شروع برنامه
        setContent {
            val vm: TradeViewModel = viewModel()
            AppTheme(vm.mode, vm.accent, vm.mascot) { App(vm) }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) LockState.lockNow(this)    // وقتی برنامه به پس‌زمینه رفت
    }
}

@Composable
fun App(vm: TradeViewModel) {
    val ctx = LocalContext.current
    val trades by vm.trades.collectAsState()
    // 0 خانه، 1 معاملات، 2 ثبت، 3 تنظیمات، 4 تقویم، 5 امنیت
    var screen by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(!LockState.locked && screen != 0) { screen = 0 }

    if (LockState.locked) {
        LockOverlay(
            mode = LockState.mode(ctx), setup = false,
            onDone = { LockState.locked = false }, onCancel = null
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (screen != 2) Column {
                SessionStrip()
                NavigationBar(containerColor = ColCard) {
                    NavigationBarItem(screen == 0, { screen = 0 },
                        icon = { Icon(Icons.Filled.Home, null) }, label = { Text(tr("خانه")) })
                    NavigationBarItem(screen == 1, { screen = 1 },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, null) }, label = { Text(tr("معاملات")) })
                    NavigationBarItem(screen == 4, { screen = 4 },
                        icon = { Icon(Icons.Filled.DateRange, null) }, label = { Text(tr("تقویم")) })
                    NavigationBarItem(screen == 5, { screen = 5 },
                        icon = { Icon(Icons.Filled.Lock, null) }, label = { Text(tr("امنیت")) })
                    NavigationBarItem(screen == 3, { screen = 3 },
                        icon = { Icon(Icons.Filled.Settings, null) }, label = { Text(tr("تنظیمات")) })
                }
            }
        },
        floatingActionButton = {
            if (screen == 0 || screen == 1) FloatingActionButton(
                onClick = { screen = 2 },
                containerColor = ColSaffron, contentColor = ColOnSaffron
            ) { Icon(Icons.Filled.Add, tr("ثبت معامله")) }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (screen) {
                0 -> HomeScreen(trades, vm.userName)
                1 -> ListScreen(trades) { vm.delete(it) }
                2 -> AddScreen(
                    canAdd = vm.isPro || trades.size < TradeViewModel.FREE_LIMIT,
                    onSave = { vm.add(it); screen = 0 },
                    onBack = { screen = 0 }
                )
                4 -> CalendarScreen(trades)
                5 -> SecurityScreen()
                else -> SettingsScreen(vm)
            }
        }
    }
}
