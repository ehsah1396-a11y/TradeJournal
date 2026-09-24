package com.example.tradejournal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun <T> rememberSaveable(init: () -> T): T = remember { init() }
