package com.example.tradejournal

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.MessageDigest

/** وضعیت قفل برنامه. mode: 0 بدون قفل، 1 پین، 2 الگو، 3 اثر انگشت (با پین پشتیبان) */
object LockState {
    var locked by mutableStateOf(false)

    private fun sp(c: Context) = c.getSharedPreferences("lock", Context.MODE_PRIVATE)
    fun mode(c: Context): Int = sp(c).getInt("mode", 0)

    private fun hash(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(("tj|" + s).toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun save(c: Context, mode: Int, secret: String) {
        sp(c).edit().putInt("mode", mode).putString("h", hash(secret)).apply()
    }

    fun disable(c: Context) {
        sp(c).edit().putInt("mode", 0).remove("h").apply()
        locked = false
    }

    fun check(c: Context, secret: String): Boolean = hash(secret) == sp(c).getString("h", null)

    private var graceUntil = 0L

    /** مهلت کوتاه: وقتی برنامه‌ی دیگری (گالری، دیالوگ مجوز) باز می‌شود، قفل نشو. ms = 0 یعنی پایان مهلت */
    fun grace(ms: Long = 60_000L) { graceUntil = System.currentTimeMillis() + ms }

    fun lockNow(c: Context) {
        if (System.currentTimeMillis() < graceUntil) return
        if (mode(c) != 0) locked = true
    }
}

private fun runBiometric(c: Context, title: String, onOk: () -> Unit, onErr: (String) -> Unit) {
    if (Build.VERSION.SDK_INT < 28 || !c.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
        onErr("این گوشی اثر انگشت ندارد؛ از پین پشتیبان استفاده کن")
        return
    }
    try {
        val ex = c.mainExecutor
        val prompt = BiometricPrompt.Builder(c)
            .setTitle(title)
            .setNegativeButton("انصراف", ex) { _, _ -> }
            .build()
        prompt.authenticate(CancellationSignal(), ex, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) { onOk() }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                onErr(errString?.toString() ?: "خطا در اثر انگشت")
            }
        })
    } catch (e: Exception) {
        onErr("اثر انگشت در دسترس نیست")
    }
}

/**
 * صفحه‌ی تمام‌صفحه‌ی قفل.
 * setup = true یعنی ساخت قفل جدید (با تکرار)، setup = false یعنی باز کردن قفل.
 */
@Composable
fun LockOverlay(mode: Int, setup: Boolean, onDone: (String) -> Unit, onCancel: (() -> Unit)?) {
    val ctx = LocalContext.current
    var stage by remember { mutableIntStateOf(0) }
    var first by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var tick by remember { mutableIntStateOf(0) }
    var fpOk by remember { mutableStateOf(false) }
    var usePin by remember { mutableStateOf(false) }

    // مرحله‌ای که الان باید نشان داده شود: 1 پین، 2 الگو، 3 اثر انگشت
    val ui = if (mode == 3) {
        if (setup) (if (fpOk) 1 else 3) else (if (usePin) 1 else 3)
    } else mode

    val submit: (String) -> Unit = { v ->
        if (!setup) {
            if (LockState.check(ctx, v)) onDone(v) else { msg = "رمز اشتباه است"; tick += 1 }
        } else if (stage == 0) {
            first = v; stage = 1; msg = ""; tick += 1
        } else if (v == first) {
            onDone(v)
        } else {
            stage = 0; first = ""; msg = "دو بار یکسان نبود، دوباره امتحان کن"; tick += 1
        }
    }

    val title = if (!setup) "برنامه قفل است"
    else if (ui == 3) "اثر انگشت را تایید کن"
    else if (stage == 1) "برای تایید، دوباره وارد کن"
    else if (mode == 3) "یک پین پشتیبان بساز"
    else if (ui == 2) "یک الگو بکش"
    else "یک پین ۴ رقمی بساز"

    Column(
        Modifier.fillMaxSize().background(ColBg).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Mascot(LocalMascot.current, 1, 60.dp)
        Spacer(Modifier.height(8.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text(msg, color = ColDn, fontSize = 12.sp, textAlign = TextAlign.Center,
            modifier = Modifier.heightIn(min = 22.dp).padding(vertical = 4.dp))

        when (ui) {
            1 -> PinPad(tick, submit)
            2 -> PatternPad(tick, { msg = "حداقل ۴ نقطه را وصل کن"; tick += 1 }, submit)
            else -> FingerprintPad(
                setup,
                onOk = { if (setup) { fpOk = true; msg = "" } else onDone("") },
                onErr = { msg = it }
            )
        }

        if (!setup && mode == 3) {
            TextButton(onClick = { usePin = !usePin; msg = "" }) {
                Text(if (usePin) "ورود با اثر انگشت" else "ورود با پین پشتیبان")
            }
        }
        if (onCancel != null) TextButton(onClick = onCancel) { Text("انصراف") }
    }
}

@Composable
private fun PinPad(reset: Int, onComplete: (String) -> Unit) {
    var pin by remember(reset) { mutableStateOf("") }
    val doneNow by rememberUpdatedState(onComplete)
    val on = ColSaffron
    val off = ColMuted.copy(alpha = 0.35f)

    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(vertical = 10.dp)) {
        for (i in 0..3) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(if (i < pin.length) on else off))
        }
    }
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (r in 0..3) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (c in 0..2) {
                        val k = keys[r * 3 + c]
                        Box(
                            Modifier.size(62.dp).clip(CircleShape)
                                .background(if (k == "") Color.Transparent else ColCard)
                                .clickable(enabled = k != "") {
                                    if (k == "⌫") pin = pin.dropLast(1)
                                    else if (pin.length < 4) {
                                        pin += k
                                        if (pin.length == 4) doneNow(pin)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) { Text(if (k == "⌫") k else faDigits(k), fontSize = 22.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatternPad(reset: Int, onShort: () -> Unit, onComplete: (String) -> Unit) {
    var seq by remember(reset) { mutableStateOf(listOf<Int>()) }
    var cur by remember(reset) { mutableStateOf<Offset?>(null) }
    val doneNow by rememberUpdatedState(onComplete)
    val shortNow by rememberUpdatedState(onShort)
    val accent = ColSaffron
    val idle = ColMuted
    val bg = ColCard

    Canvas(
        Modifier.size(250.dp).clip(RoundedCornerShape(24.dp)).background(bg)
            .pointerInput(reset) {
                val cell = size.width / 3f
                fun center(i: Int) = Offset((i % 3 + 0.5f) * cell, (i / 3 + 0.5f) * cell)
                fun hit(p: Offset): Int {
                    for (i in 0..8) if ((p - center(i)).getDistance() < cell * 0.3f) return i
                    return -1
                }
                detectDragGestures(
                    onDragStart = { p ->
                        val h = hit(p)
                        seq = if (h >= 0) listOf(h) else listOf()
                        cur = p
                    },
                    onDrag = { change, _ ->
                        cur = change.position
                        val h = hit(change.position)
                        if (h >= 0 && !seq.contains(h)) seq = seq + h
                    },
                    onDragEnd = {
                        cur = null
                        if (seq.size >= 4) doneNow(seq.joinToString("")) else shortNow()
                    },
                    onDragCancel = { cur = null }
                )
            }
    ) {
        val cell = size.width / 3f
        fun c(i: Int) = Offset((i % 3 + 0.5f) * cell, (i / 3 + 0.5f) * cell)
        for (k in 1 until seq.size) {
            drawLine(accent, c(seq[k - 1]), c(seq[k]), strokeWidth = 8f, cap = StrokeCap.Round)
        }
        val cp = cur
        if (cp != null && seq.isNotEmpty()) {
            drawLine(accent, c(seq.last()), cp, strokeWidth = 8f, cap = StrokeCap.Round)
        }
        for (i in 0..8) {
            val sel = seq.contains(i)
            drawCircle(color = if (sel) accent else idle, radius = if (sel) 16f else 12f, center = c(i))
        }
    }
}

@Composable
private fun FingerprintPad(setup: Boolean, onOk: () -> Unit, onErr: (String) -> Unit) {
    val ctx = LocalContext.current
    val okNow by rememberUpdatedState(onOk)
    val errNow by rememberUpdatedState(onErr)
    val go = { runBiometric(ctx, if (setup) "تایید اثر انگشت" else "باز کردن برنامه", { okNow() }, { errNow(it) }) }
    LaunchedEffect(Unit) { go() }
    Box(
        Modifier.size(120.dp).clip(CircleShape).background(ColCard).clickable { go() },
        contentAlignment = Alignment.Center
    ) { Text("☝️", fontSize = 48.sp) }
    Text("انگشتت را روی سنسور بگذار (یا روی دایره بزن)", color = ColMuted, fontSize = 12.sp,
        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
}
