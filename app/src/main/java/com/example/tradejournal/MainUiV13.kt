package com.example.tradejournal

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

private fun nfm(): NumberFormat =
    NumberFormat.getInstance(if (Lang.en) Locale.US else Locale("fa")).apply { maximumFractionDigits = 2 }
fun fmt(v: Double) = "\u200E" + (if (v > 0) "+" else if (v < 0) "−" else "") + nfm().format(abs(v)) + "$"
fun fa(n: Int): String = nfm().format(n)

fun fmtShort(v: Double): String {
    val r = Math.round(v)
    return (if (r > 0) "+" else if (r < 0) "−" else "") + faDigits(Math.abs(r).toString())
}

fun String.toNum(): Double? = map {
    when (it) {
        in '۰'..'۹' -> '0' + (it - '۰')
        in '٠'..'٩' -> '0' + (it - '٠')
        '٫', ',' -> '.'
        else -> it
    }
}.joinToString("").toDoubleOrNull()

// ---------- خانه ----------
@Composable
fun HomeScreen(trades: List<Trade>, name: String, usedQuota: Int) {
    val net = trades.sumOf { it.pl }
    val wr = if (trades.isEmpty()) 0 else trades.count { it.pl > 0 } * 100 / trades.size
    val best = trades.maxOfOrNull { it.pl } ?: 0.0
    val curve = trades.reversed().runningFold(0.0) { a, t -> a + t.pl }
    val mk = LocalMascot.current

    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("سلام، ") + name.ifBlank { tr("تریدر") }, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(tr("خلاصه عملکرد"), color = ColMuted, fontSize = 12.sp)
                }
                Mascot(mk, if (net >= 0) 0 else 2, 52.dp)
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(ColCard).padding(18.dp)
            ) {
                Text(tr("سود خالص"), color = ColMuted, fontSize = 12.sp)
                Text(fmt(net), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (net >= 0) ColUp else ColDn)
                Spacer(Modifier.height(8.dp))
                EquityChart(curve, Modifier.fillMaxWidth().height(110.dp))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat(tr("وین‌ریت"), fa(wr) + (if (Lang.en) "%" else tr("٪")), Modifier.weight(1f))
                Stat(tr("معاملات"), fa(trades.size), Modifier.weight(1f))
                Stat(tr("بهترین"), fmt(best), Modifier.weight(1f), ColUp)
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ColCard).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(tr("احساس تو و نتیجه معامله"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (trades.isEmpty()) {
                    Text(tr("با ثبت معامله‌ها، اینجا می‌بینی با کدوم احساس چقدر سود می‌کنی."),
                        color = ColMuted, fontSize = 12.sp)
                }
                for (m in 0..3) {
                    val a = trades.filter { it.mood == m }
                    if (a.isNotEmpty()) {
                        val avg = a.sumOf { it.pl } / a.size
                        val frac = (abs(avg) / 250.0).toFloat().coerceIn(0.05f, 1f)
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Mascot(mk, m, 34.dp)
                            Text(MOOD_NAMES[m], Modifier.width(52.dp), fontSize = 13.sp)
                            Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(6.dp)).background(ColCard2)) {
                                Box(Modifier.fillMaxWidth(frac).fillMaxHeight()
                                    .background(if (avg >= 0) ColUp else ColDn))
                            }
                            Text(fmt(avg), fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                color = if (avg >= 0) ColUp else ColDn)
                        }
                    }
                }
            }
        }
        val stratStats = trades.filter { it.strategy.isNotBlank() }
            .groupBy { it.strategy }
            .map { (name, list) -> Triple(name, list.sumOf { it.pl }, list.count { it.pl > 0 } * 100 / list.size) }
            .sortedByDescending { it.second }
        if (stratStats.isNotEmpty()) item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ColCard).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(tr("بازدهی استراتژی‌ها"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                val maxAbs = stratStats.maxOf { abs(it.second) }.coerceAtLeast(1.0)
                stratStats.take(6).forEach { (name, sum, wr2) ->
                    val frac = (abs(sum) / maxAbs).toFloat().coerceIn(0.05f, 1f)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(name, Modifier.width(84.dp), fontSize = 12.sp, maxLines = 1)
                        Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(6.dp)).background(ColCard2)) {
                            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(if (sum >= 0) ColUp else ColDn))
                        }
                        Text(fa(wr2) + "%", color = ColMuted, fontSize = 11.sp)
                        Text(fmt(sum), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (sum >= 0) ColUp else ColDn)
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ColCard).padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tr("سهم رایگان شما"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(fa(usedQuota.coerceAtMost(TradeViewModel.FREE_LIMIT)) + tr(" از ") + fa(TradeViewModel.FREE_LIMIT), color = ColMuted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (usedQuota / TradeViewModel.FREE_LIMIT.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
                    color = ColSaffron, trackColor = ColCard2
                )
            }
        }
        item { Text(tr("آخرین معاملات"), fontWeight = FontWeight.SemiBold) }
        if (trades.isEmpty()) item { Text(tr("هنوز معامله‌ای ثبت نکردی. با دکمه + شروع کن."), color = ColMuted) }
        items(trades.take(3), key = { it.id }) { TradeRow(it, onDelete = null) }
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
    val lineColor = ColUp
    val mx = pts.max(); val mn = pts.min()
    val r = if (mx - mn > 0) mx - mn else 1.0
    Canvas(m) {
        val p = Path()
        pts.forEachIndexed { i, v ->
            val x = size.width * (i / (pts.size - 1f))
            val y = size.height * 0.92f - ((v - mn) / r).toFloat() * size.height * 0.84f
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        drawPath(p, lineColor, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// ---------- لیست معاملات ----------
@Composable
fun ListScreen(vm: TradeViewModel, trades: List<Trade>, onDelete: (Trade) -> Unit, onEdit: (Trade) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var msg by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        LockState.grace(0L)
        if (uri != null) scope.launch(Dispatchers.IO) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { writeTradesXlsx(it, trades) }
                withContext(Dispatchers.Main) { msg = tr("فایل اکسل ذخیره شد") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { msg = tr("ذخیره فایل اکسل ممکن نشد") }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        LockState.grace(0L)
        if (uri != null) scope.launch(Dispatchers.IO) {
            val bytes = try { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } } catch (e: Exception) { null }
            val res = if (bytes != null) parseMt4Report(decodeReportBytes(bytes)) else null
            if (res != null && res.trades.isNotEmpty()) vm.insertAll(res.trades)
            withContext(Dispatchers.Main) {
                msg = if (res == null || res.trades.isEmpty()) tr("چیزی برای وارد کردن پیدا نشد. مطمئن شو فایل، گزارش HTML متاتریدر باشد.")
                else fa(res.trades.size) + tr(" معامله وارد شد. حالا هرکدوم رو باز کن و عکس، ویس و احساس اضافه کن.")
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("معاملات"), Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                IconButton({ LockState.grace(); importLauncher.launch("*/*") }) {
                    Text("📥", fontSize = 18.sp)
                }
                IconButton({ LockState.grace(); exportLauncher.launch("trades.xlsx") }) {
                    Text("📄", fontSize = 18.sp)
                }
            }
        }
        if (trades.isEmpty()) item { Text(tr("لیست خالی است."), color = ColMuted) }
        items(trades, key = { it.id }) { t ->
            TradeRow(t, onClick = { onEdit(t) }, onDelete = { deleteMedia(ctx, t); onDelete(t) })
        }
    }
    if (msg != null) AlertDialog(
        onDismissRequest = { msg = null },
        confirmButton = { TextButton({ msg = null }) { Text(tr("باشه")) } },
        text = { Text(msg!!) }
    )
}

@Composable
fun TradeRow(t: Trade, onClick: (() -> Unit)? = null, onDelete: (() -> Unit)?) {
    val c = if (t.isBuy) ColUp else ColDn
    val ctx = LocalContext.current
    val pf = remember(t.createdAt) { photoFile(ctx, t.createdAt.toString()) }
    val vf = remember(t.createdAt) { voiceFile(ctx, t.createdAt.toString()) }
    val hasP = remember(t.createdAt) { pf.exists() }
    val hasV = remember(t.createdAt) { vf.exists() }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ColCard)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.copy(alpha = .16f)),
            contentAlignment = Alignment.Center
        ) { Text(if (t.isBuy) tr("خرید") else tr("فروش"), color = c, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f)) {
            Text(t.symbol, fontWeight = FontWeight.Bold)
            Text(t.note.ifBlank { tr("بدون یادداشت") }, color = ColMuted, fontSize = 12.sp, maxLines = 1)
            if (hasP || hasV) {
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (hasP) PhotoThumb(pf.absolutePath, 44.dp)
                    if (hasV) PlayButton(vf.absolutePath)
                }
            }
        }
        Mascot(LocalMascot.current, t.mood.coerceIn(0, 3), 36.dp)
        Text(fmt(t.pl), fontWeight = FontWeight.ExtraBold, color = if (t.pl >= 0) ColUp else ColDn)
        if (onDelete != null) IconButton(onDelete) { Icon(Icons.Filled.Delete, tr("حذف"), tint = ColMuted) }
    }
}

// ---------- ثبت معامله ----------
@Composable
fun AddScreen(canAdd: Boolean, editing: Trade?, onSave: (Trade) -> Unit, onBack: () -> Unit) {
    val symbols = listOf("XAUUSD", "EURUSD", "GBPUSD", "BTCUSD")
    var sym by rememberSaveable(editing?.id) { mutableStateOf(if (editing != null && editing.symbol in symbols) editing.symbol else "XAUUSD") }
    var custom by rememberSaveable(editing?.id) { mutableStateOf(if (editing != null && editing.symbol !in symbols) editing.symbol else "") }
    var isBuy by rememberSaveable(editing?.id) { mutableStateOf(editing?.isBuy ?: true) }
    var isLoss by rememberSaveable(editing?.id) { mutableStateOf((editing?.pl ?: 0.0) < 0) }
    var entry by rememberSaveable(editing?.id) { mutableStateOf(editing?.entry?.toString() ?: "") }
    var exit by rememberSaveable(editing?.id) { mutableStateOf(editing?.exit?.toString() ?: "") }
    var amount by rememberSaveable(editing?.id) { mutableStateOf(editing?.pl?.let { Math.abs(it).toString() } ?: "") }
    var note by rememberSaveable(editing?.id) { mutableStateOf(editing?.note ?: "") }
    var mood by rememberSaveable(editing?.id) { mutableIntStateOf(editing?.mood ?: 0) }
    val strategies = listOf("Breakout", "Price Action", "Scalping", "Swing", "News")
    var strategy by rememberSaveable(editing?.id) { mutableStateOf(editing?.strategy ?: "") }
    var customStrategy by rememberSaveable(editing?.id) {
        mutableStateOf(if (editing != null && editing.strategy.isNotBlank() && editing.strategy !in strategies) editing.strategy else "")
    }
    val num = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    val mk = LocalMascot.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val mediaKey = editing?.createdAt?.toString() ?: "tmp"
    var hasPhoto by rememberSaveable(editing?.id) { mutableStateOf(photoFile(ctx, mediaKey).exists()) }
    var hasVoice by rememberSaveable(editing?.id) { mutableStateOf(voiceFile(ctx, mediaKey).exists()) }
    var stamp by rememberSaveable(editing?.id) { mutableLongStateOf(0L) }
    val recorder = remember { VoiceRecorder(ctx) }
    DisposableEffect(Unit) { onDispose { recorder.stop() } }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        LockState.grace(0L)
        if (uri != null) scope.launch(Dispatchers.IO) {
            if (saveImage(ctx, uri, photoFile(ctx, mediaKey))) {
                hasPhoto = true
                stamp = System.currentTimeMillis()
            }
        }
    }
    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        LockState.grace(0L)
        if (ok && recorder.start(voiceFile(ctx, mediaKey))) hasVoice = false
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton({ if (editing == null) { photoFile(ctx, "tmp").delete(); voiceFile(ctx, "tmp").delete() }; onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("بازگشت")) }
            Text(if (editing == null) tr("ثبت معامله") else tr("ویرایش معامله"), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            symbols.forEach { s ->
                FilterChip(selected = custom.isBlank() && sym == s,
                    onClick = { sym = s; custom = "" }, label = { Text(s) })
            }
        }
        OutlinedTextField(custom, { custom = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '/' } },
            label = { Text(tr("نماد دیگر (مثلاً USDJPY)")) }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(isBuy, { isBuy = true }, { Text(tr("خرید")) })
            FilterChip(!isBuy, { isBuy = false }, { Text(tr("فروش")) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(entry, { entry = it }, label = { Text(tr("قیمت ورود")) },
                keyboardOptions = num, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(exit, { exit = it }, label = { Text(tr("قیمت خروج")) },
                keyboardOptions = num, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(!isLoss, { isLoss = false }, { Text(tr("سود")) })
            FilterChip(isLoss, { isLoss = true }, { Text(tr("زیان")) })
            OutlinedTextField(amount, { amount = it }, label = { Text(tr("مبلغ (دلار)")) },
                keyboardOptions = num, singleLine = true, modifier = Modifier.weight(1f))
        }
        Text(tr("احساست هنگام این معامله"), color = ColMuted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0..3) {
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(if (mood == i) ColCard2 else ColCard)
                        .border(if (mood == i) 2.dp else 0.dp, ColSaffron, RoundedCornerShape(14.dp))
                        .clickable { mood = i }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Mascot(mk, i, 44.dp)
                    Text(MOOD_NAMES[i], fontSize = 11.sp, color = ColMuted)
                }
            }
        }
        Text(tr("استراتژی (اختیاری)"), color = ColMuted, fontSize = 12.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(strategy.isBlank(), { strategy = ""; customStrategy = "" }, { Text(tr("هیچ‌کدام")) })
            strategies.forEach { s2 ->
                FilterChip(strategy == s2, { strategy = s2; customStrategy = "" }, { Text(s2) })
            }
        }
        OutlinedTextField(
            customStrategy,
            { customStrategy = it; if (it.isNotBlank()) strategy = it },
            label = { Text(tr("استراتژی دیگر")) }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(note, { note = it }, label = { Text(tr("یادداشت")) },
            minLines = 3, modifier = Modifier.fillMaxWidth())

        Text(tr("پیوست"), color = ColMuted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { LockState.grace(); pickImage.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) { Text(if (hasPhoto) tr("📷 تغییر عکس") else tr("📷 عکس چارت")) }
            OutlinedButton(
                onClick = {
                    if (recorder.recording) {
                        recorder.stop(); hasVoice = true
                    } else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        if (recorder.start(voiceFile(ctx, mediaKey))) hasVoice = false
                    } else {
                        LockState.grace()
                        askMic.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (recorder.recording) tr("⏹ توقف ضبط") else if (hasVoice) tr("🎙 ضبط دوباره") else tr("🎙 ضبط ویس"))
            }
        }
        if (hasPhoto || (hasVoice && !recorder.recording)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (hasPhoto) PhotoThumb(photoFile(ctx, mediaKey).absolutePath, 90.dp, stamp)
                if (hasVoice && !recorder.recording) PlayButton(voiceFile(ctx, mediaKey).absolutePath)
            }
        }

        if (editing == null && !canAdd) Text(tr("به سقف ") + fa(TradeViewModel.FREE_LIMIT) + tr(" معامله رایگان رسیدی. با ارتقا به پرو این سقف برداشته می‌شود."),
            color = ColDn, fontSize = 13.sp)
        val a = amount.toNum()
        Button(
            onClick = {
                val v = abs(a ?: 0.0) * (if (isLoss) -1 else 1)
                if (recorder.recording) { recorder.stop(); hasVoice = true }
                if (editing == null) {
                    val ts = System.currentTimeMillis()
                    if (hasPhoto) photoFile(ctx, "tmp").renameTo(photoFile(ctx, ts.toString()))
                    if (hasVoice) voiceFile(ctx, "tmp").renameTo(voiceFile(ctx, ts.toString()))
                    onSave(Trade(symbol = custom.ifBlank { sym }, isBuy = isBuy, entry = entry.toNum(),
                        exit = exit.toNum(), pl = v, note = note.trim(), mood = mood, createdAt = ts,
                        strategy = strategy.trim()))
                } else {
                    onSave(editing.copy(symbol = custom.ifBlank { sym }, isBuy = isBuy, entry = entry.toNum(),
                        exit = exit.toNum(), pl = v, note = note.trim(), mood = mood, strategy = strategy.trim()))
                }
            },
            enabled = (editing != null || canAdd) && a != null,
            colors = ButtonDefaults.buttonColors(containerColor = ColSaffron, contentColor = ColOnSaffron),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (editing == null) tr("ذخیره معامله") else tr("ذخیره تغییرات"), fontWeight = FontWeight.ExtraBold) }
    }
}
