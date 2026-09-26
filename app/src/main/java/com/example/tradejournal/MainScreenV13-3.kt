package com.example.tradejournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashReporter.install(this)
        val crash = CrashReporter.read(this)
        if (crash != null) {
            // اگه دفعه‌ی قبل اپ خطا داده بود، متن خطا را نشان بده
            setContent { CrashScreen(crash) { CrashReporter.clear(this@MainActivity); recreate() } }
            return
        }
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
    // 0 خانه، 1 معاملات، 2 ثبت/ویرایش، 3 تنظیمات، 4 تقویم، 5 امنیت (از تنظیمات)، 6 پرو
    var screen by rememberSaveable { mutableIntStateOf(0) }
    var onboardStep by rememberSaveable { mutableStateOf(if (Onboarding.done(ctx)) -1 else 0) }
    val onboarding = onboardStep >= 0
    val effScreen = if (onboarding) ONB_STEPS[onboardStep].screen else screen
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editingTrade = trades.find { it.id == editingId }
    BackHandler(!LockState.locked && !onboarding && screen != 0) { screen = 0; editingId = null }

    if (LockState.locked) {
        LockOverlay(
            mode = LockState.mode(ctx), setup = false,
            onDone = { LockState.locked = false }, onCancel = null
        )
        return
    }

    val finishOnboarding = { Onboarding.setDone(ctx); onboardStep = -1; screen = 0 }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (screen != 2) Column {
                        SessionStrip()
                        NavigationBar(containerColor = ColCard) {
                            NavigationBarItem(effScreen == 0, { if (!onboarding) screen = 0 },
                                icon = { Icon(Icons.Filled.Home, null) }, label = { Text(tr("خانه")) })
                            NavigationBarItem(effScreen == 1, { if (!onboarding) screen = 1 },
                                icon = { Icon(Icons.AutoMirrored.Filled.List, null) }, label = { Text(tr("معاملات")) })
                            NavigationBarItem(effScreen == 4, { if (!onboarding) screen = 4 },
                                icon = { Icon(Icons.Filled.DateRange, null) }, label = { Text(tr("تقویم")) })
                            NavigationBarItem(effScreen == 3 || effScreen == 5, { if (!onboarding) screen = 3 },
                                icon = { Icon(Icons.Filled.Settings, null) }, label = { Text(tr("تنظیمات")) })
                        }
                    }
                },
                floatingActionButton = {
                    if (!onboarding && (screen == 0 || screen == 1)) FloatingActionButton(
                        onClick = { editingId = null; screen = 2 },
                        containerColor = ColSaffron, contentColor = ColOnSaffron
                    ) { Icon(Icons.Filled.Add, tr("ثبت معامله")) }
                }
            ) { pad ->
                Box(Modifier.padding(pad)) {
                    when (effScreen) {
                        0 -> HomeScreen(trades, vm.userName, vm.totalAdded)
                        1 -> ListScreen(
                            vm, trades,
                            onDelete = { vm.delete(it) },
                            onEdit = { if (!onboarding) { editingId = it.id; screen = 2 } }
                        )
                        2 -> AddScreen(
                            canAdd = vm.isPro || vm.totalAdded < TradeViewModel.FREE_LIMIT,
                            editing = editingTrade,
                            onSave = {
                                if (editingTrade != null) vm.update(it) else vm.add(it)
                                editingId = null
                                screen = 1
                            },
                            onBack = { editingId = null; screen = if (editingTrade != null) 1 else 0 }
                        )
                        4 -> CalendarScreen(trades)
                        5 -> SecurityScreen(onBack = { screen = 3 })
                        6 -> ProScreen(vm, onBack = { screen = 0 })
                        else -> SettingsScreen(vm, onOpenPro = { screen = 6 }, onOpenSecurity = { screen = 5 })
                    }
                }
            }
        }
    }
    if (onboarding) Box(Modifier.fillMaxSize().padding(top = 36.dp), contentAlignment = Alignment.TopCenter) {
        OnboardingOverlay(
            step = onboardStep,
            onNext = { if (onboardStep < ONB_STEPS.lastIndex) onboardStep++ else finishOnboarding() },
            onSkip = { finishOnboarding() }
        )
    }
    }
}
