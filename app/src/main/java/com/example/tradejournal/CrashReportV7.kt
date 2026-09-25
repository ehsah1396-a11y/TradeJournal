package com.example.tradejournal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/** متن آخرین خطای اپ را ذخیره می‌کند تا دفعه‌ی بعد نشان داده شود */
object CrashReporter {
    private fun file(c: Context) = File(c.filesDir, "crash.txt")

    fun install(c: Context) {
        val app = c.applicationContext
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try { file(app).writeText(Log.getStackTraceString(e)) } catch (x: Exception) { }
            if (prev != null) prev.uncaughtException(t, e)
        }
    }

    fun read(c: Context): String? = try {
        val f = file(c)
        if (f.exists()) f.readText() else null
    } catch (e: Exception) {
        null
    }

    fun clear(c: Context) {
        try { file(c).delete() } catch (e: Exception) { }
    }
}

@Composable
fun CrashScreen(trace: String, onClear: () -> Unit) {
    val ctx = LocalContext.current
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(16.dp).padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("دفعه‌ی قبل اپ خطا داد", fontSize = 18.sp)
                Text("دکمه‌ی «کپی متن خطا» را بزن و متن را برای من بفرست (یا اسکرین‌شات بگیر).", fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("crash", trace))
                    }) { Text("کپی متن خطا") }
                    Button(onClick = onClear) { Text("پاک کردن و ادامه") }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    SelectionContainer { Text(trace.take(6000), fontSize = 10.sp) }
                }
            }
        }
    }
}
