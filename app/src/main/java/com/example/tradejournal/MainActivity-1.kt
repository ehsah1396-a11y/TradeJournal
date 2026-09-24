package com.example.tradejournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: TradeViewModel = viewModel()
            AppTheme(vm.mode, vm.accent, vm.mascot) { App(vm) }
        }
    }
}

@Composable
fun App(vm: TradeViewModel) {
    val trades by vm.trades.collectAsState()
    var screen by rememberSaveable { mutableIntStateOf(0) } // 0 خانه، 1 معاملات، 2 ثبت، 3 تنظیمات
    BackHandler(screen != 0) { screen = 0 }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (screen != 2) NavigationBar(containerColor = ColCard) {
                NavigationBarItem(screen == 0, { screen = 0 },
                    icon = { Icon(Icons.Filled.Home, null) }, label = { Text("خانه") })
                NavigationBarItem(screen == 1, { screen = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, null) }, label = { Text("معاملات") })
                NavigationBarItem(screen == 3, { screen = 3 },
                    icon = { Icon(Icons.Filled.Settings, null) }, label = { Text("تنظیمات") })
            }
        },
        floatingActionButton = {
            if (screen == 0 || screen == 1) FloatingActionButton(
                onClick = { screen = 2 },
                containerColor = ColSaffron, contentColor = ColOnSaffron
            ) { Icon(Icons.Filled.Add, "ثبت معامله") }
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
                else -> SettingsScreen(vm)
            }
        }
    }
}
