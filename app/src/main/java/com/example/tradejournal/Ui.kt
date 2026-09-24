package com.example.tradejournal

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

val ColBg = Color(0xFF0E1330)
val ColCard = Color(0xFF171D45)
val ColCard2 = Color(0xFF202862)
val ColSaffron = Color(0xFFF2A93B)
val ColOnSaffron = Color(0xFF1A1200)
val ColUp = Color(0xFF3FD0C9)
val ColDn = Color(0xFFFF6B6B)
val ColMuted = Color(0xFF98A0C8)
val ColText = Color(0xFFF3F1EA)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = ColSaffron, onPrimary = ColOnSaffron,
            background = ColBg, onBackground = ColText,
            surface = ColCard, onSurface = ColText,
            surfaceVariant = ColCard2, onSurfaceVariant = ColMuted,
            secondaryContainer = Color(0x33F2A93B), onSecondaryContainer = ColSaffron
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
    }
}

private val nf = NumberFormat.getInstance(Locale("fa")).apply { maximumFractionDigits = 2 }
fun fmt(v: Double) = "\u200E" + (if (v > 0) "+" else if (v < 0) "−" else "") + nf.format(abs(v)) + "$"
fun fa(n: Int): String = nf.format(n)

fun String.toNum(): Double? = map {
    when (it) {
        in '۰'..'۹' -> '0' + (it - '۰')
        in '٠'..'٩' -> '0' + (it - '٠')
        '٫', ',' -> '.'
        else -> it
    }
}.joinToString("").toDoubleOrNull()

val MOODS = listOf("😎", "😐", "😰", "😡")

@Composable
fun HomeScreen(trades: List<Trade>) {
    val net = trades.sumOf { it.pl }
    val wr = if (trades.isEmpty()) 0 else trades.count { it.pl > 0 } * 100 / trades.size
    val best = trades.maxOfOrNull { it.pl } ?: 0.0
    val curve = trades.reversed().runningFold(0.0) { a, t -> a + t.pl }

    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("سلام، تریدر", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("خلاصه عملکرد", color = ColMuted, fontSize = 12.sp)
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(ColCard).padding(18.dp)
            ) {
                Text("سود خالص", color = ColMuted, fontSize = 12.sp)
                Text(fmt(net), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (net >= 0) ColUp else ColDn)
                Spacer(Modifier.height(8.dp))
                EquityChart(curve, Modifier.fillMaxWidth().height(110.dp))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("وین‌ریت", "${fa(wr)}٪", Modifier.weight(1f))
                Stat("معاملات", fa(trades.size), Modifier.weight(1f))
                Stat("بهترین", fmt(best), Modifier.weight(1f), ColUp)
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ColCard).padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("سهم رایگان شما", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("${fa(trades.size)} از ${fa(TradeViewModel.FREE_LIMIT)}", color = ColMuted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (trades.size / TradeViewModel.FREE_LIMIT.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
                    color = ColSaffron, trackColor = ColCard2
                )
            }
        }
        item { Text("آخرین معاملات", fontWeight = FontWeight.SemiBold) }
        if (trades.isEmpty()) item { Text("هنوز معامله‌ای ثبت نکردی. با دکمه + شروع کن.", color = ColMuted) }
        items(trades.take(3), key = { it.id }) { TradeRow(it, null) }
    }
}

@Composable
fun Stat(label: String, value: String, m: Modifier, color: Color = ColText) {
    Column(m.clip(RoundedCornerShape(16.dp)).background(ColCard).padding(12.dp)) {
        Text(label, color = ColMuted, fontSize = 12.sp)
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun EquityChart(pts: List<Double>, m: Modifier) {
    if (pts.size < 2) { Box(m); return }
    val mx = pts.max(); val mn = pts.min()
    val r = if (mx - mn > 0) mx - mn else 1.0
    Canvas(m) {
        val p = Path()
        pts.forEachIndexed { i, v ->
            val x = size.width * (i / (pts.size - 1f))
            val y = size.height * 0.92f - ((v - mn) / r).toFloat() * size.height * 0.84f
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        drawPath(p, ColUp, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ListScreen(trades: List<Trade>, onDelete: (Trade) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Text("معاملات", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }
        if (trades.isEmpty()) item { Text("لیست خالی است.", color = ColMuted) }
        items(trades, key = { it.id }) { TradeRow(it) { onDelete(it) } }
    }
}

@Composable
fun TradeRow(t: Trade, onDelete: (() -> Unit)?) {
    val c = if (t.isBuy) ColUp else ColDn
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ColCard).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.copy(alpha = .16f)),
            contentAlignment = Alignment.Center
        ) { Text(if (t.isBuy) "خرید" else "فروش", color = c, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f)) {
            Text(t.symbol, fontWeight = FontWeight.Bold)
            Text(t.note.ifBlank { "بدون یادداشت" }, color = ColMuted, fontSize = 12.sp, maxLines = 1)
        }
        Text(MOODS.getOrElse(t.mood) { "" }, fontSize = 22.sp)
        Text(fmt(t.pl), fontWeight = FontWeight.ExtraBold, color = if (t.pl >= 0) ColUp else ColDn)
        if (onDelete != null) IconButton(onDelete) { Icon(Icons.Filled.Delete, "حذف", tint = ColMuted) }
    }
}

@Composable
fun AddScreen(canAdd: Boolean, onSave: (Trade) -> Unit, onBack: () -> Unit) {
    val symbols = listOf("XAUUSD", "EURUSD", "GBPUSD", "BTCUSD")
    var sym by rememberSaveable { mutableStateOf("XAUUSD") }
    var custom by rememberSaveable { mutableStateOf("") }
    var isBuy by rememberSaveable { mutableStateOf(true) }
    var isLoss by rememberSaveable { mutableStateOf(false) }
    var entry by rememberSaveable { mutableStateOf("") }
    var exit by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var mood by rememberSaveable { mutableIntStateOf(0) }
    val num = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") }
            Text("ثبت معامله", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            symbols.forEach { s ->
                FilterChip(selected = custom.isBlank() && sym == s,
                    onClick = { sym = s; custom = "" }, label = { Text(s) })
            }
        }
        OutlinedTextField(custom, { custom = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '/' } },
            label = { Text("نماد دیگر (مثلاً USDJPY)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(isBuy, { isBuy = true }, { Text("خرید") })
            FilterChip(!isBuy, { isBuy = false }, { Text("فروش") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(entry, { entry = it }, label = { Text("قیمت ورود") },
                keyboardOptions = num, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(exit, { exit = it }, label = { Text("قیمت خروج") },
                keyboardOptions = num, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(!isLoss, { isLoss = false }, { Text("سود") })
            FilterChip(isLoss, { isLoss = true }, { Text("زیان") })
            OutlinedTextField(amount, { amount = it }, label = { Text("مبلغ (دلار)") },
                keyboardOptions = num, singleLine = true, modifier = Modifier.weight(1f))
        }
        Text("احساست هنگام این معامله", color = ColMuted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MOODS.forEachIndexed { i, e ->
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(if (mood == i) ColCard2 else ColCard)
                        .border(if (mood == i) 2.dp else 0.dp, ColSaffron, RoundedCornerShape(14.dp))
                        .clickable { mood = i }.padding(10.dp),
                    contentAlignment = Alignment.Center
                ) { Text(e, fontSize = 26.sp) }
            }
        }
        OutlinedTextField(note, { note = it }, label = { Text("یادداشت") },
            minLines = 3, modifier = Modifier.fillMaxWidth())

        if (!canAdd) Text("به سقف ${fa(TradeViewModel.FREE_LIMIT)} معامله رایگان رسیدی. (ارتقا به پرو در مرحله بعد اضافه می‌شود)",
            color = ColDn, fontSize = 13.sp)
        val a = amount.toNum()
        Button(
            onClick = {
                val v = abs(a ?: 0.0) * (if (isLoss) -1 else 1)
                onSave(Trade(symbol = custom.ifBlank { sym }, isBuy = isBuy, entry = entry.toNum(),
                    exit = exit.toNum(), pl = v, note = note.trim(), mood = mood))
            },
            enabled = canAdd && a != null,
            colors = ButtonDefaults.buttonColors(containerColor = ColSaffron, contentColor = ColOnSaffron),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("ذخیره معامله", fontWeight = FontWeight.ExtraBold) }
    }
}
