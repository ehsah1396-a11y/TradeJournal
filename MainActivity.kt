package com.example.tradejournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppTheme { App() } }
    }
}

@Composable
fun App(vm: TradeViewModel = viewModel()) {
    val trades by vm.trades.collectAsState()
    var screen by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(screen != 0) { screen = 0 }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (screen < 2) NavigationBar(containerColor = ColCard) {
                NavigationBarItem(screen == 0, { screen = 0 },
                    icon = { Icon(Icons.Filled.Home, null) }, label = { Text("خانه") })
                NavigationBarItem(screen == 1, { screen = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, null) }, label = { Text("معاملات") })
            }
        },
        floatingActionButton = {
            if (screen < 2) FloatingActionButton(
                onClick = { screen = 2 },
                containerColor = ColSaffron, contentColor = ColOnSaffron
            ) { Icon(Icons.Filled.Add, "ثبت معامله") }
        }
    ) { pad ->
        androidx.compose.foundation.layout.Box(Modifier.padding(pad)) {
            when (screen) {
                0 -> HomeScreen(trades)
                1 -> ListScreen(trades) { vm.delete(it) }
                else -> AddScreen(
                    canAdd = vm.isPro || trades.size < TradeViewModel.FREE_LIMIT,
                    onSave = { vm.add(it); screen = 0 },
                    onBack = { screen = 0 }
                )
            }
        }
    }
}
