package com.example.tradejournal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File

// فایل‌های عکس و ویس هر معامله با زمان ثبت آن معامله نام‌گذاری می‌شوند
fun mediaDir(c: Context): File = File(c.filesDir, "media").apply { mkdirs() }
fun photoFile(c: Context, key: String): File = File(mediaDir(c), "$key.jpg")
fun voiceFile(c: Context, key: String): File = File(mediaDir(c), "$key.m4a")

fun deleteMedia(c: Context, t: Trade) {
    photoFile(c, t.createdAt.toString()).delete()
    voiceFile(c, t.createdAt.toString()).delete()
}

/** عکس را کوچک می‌کند (حداکثر ۱۶۰۰ پیکسل) و به‌صورت JPEG ذخیره می‌کند */
fun saveImage(c: Context, uri: Uri, dest: File): Boolean {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        val big = maxOf(bounds.outWidth, bounds.outHeight)
        while (big / sample > 1600) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        if (bmp == null) {
            false
        } else {
            dest.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            bmp.recycle()
            true
        }
    } catch (e: Exception) {
        false
    }
}

fun loadBitmap(path: String, max: Int): ImageBitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var s = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / s > max) s *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = s })?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

@Suppress("DEPRECATION")
private fun newRecorder(c: Context): MediaRecorder =
    if (Build.VERSION.SDK_INT >= 31) MediaRecorder(c) else MediaRecorder()

class VoiceRecorder(private val ctx: Context) {
    private var rec: MediaRecorder? = null
    var recording by mutableStateOf(false)

    fun start(dest: File): Boolean {
        var r: MediaRecorder? = null
        return try {
            r = newRecorder(ctx)
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(dest.absolutePath)
            r.prepare()
            r.start()
            rec = r
            recording = true
            true
        } catch (e: Exception) {
            try { r?.release() } catch (e2: Exception) { }
            rec = null
            recording = false
            false
        }
    }

    fun stop() {
        try { rec?.stop() } catch (e: Exception) { }
        try { rec?.release() } catch (e: Exception) { }
        rec = null
        recording = false
    }
}

class VoicePlayer {
    private var mp: MediaPlayer? = null
    var playing by mutableStateOf(false)

    fun toggle(path: String) {
        if (playing) { stop(); return }
        try {
            val p = MediaPlayer()
            p.setDataSource(path)
            p.prepare()
            p.setOnCompletionListener { stop() }
            p.start()
            mp = p
            playing = true
        } catch (e: Exception) {
            stop()
        }
    }

    fun stop() {
        try { mp?.stop() } catch (e: Exception) { }
        try { mp?.release() } catch (e: Exception) { }
        mp = null
        playing = false
    }
}

@Composable
fun PlayButton(path: String, modifier: Modifier = Modifier) {
    val player = remember { VoicePlayer() }
    DisposableEffect(Unit) { onDispose { player.stop() } }
    Box(
        modifier.size(38.dp).clip(CircleShape).background(ColSaffron).clickable { player.toggle(path) },
        contentAlignment = Alignment.Center
    ) { Text(if (player.playing) "■" else "▶", color = ColOnSaffron, fontSize = 14.sp) }
}

@Composable
fun PhotoThumb(path: String, size: Dp, stamp: Long = 0L) {
    val bmp = remember(path, stamp) { loadBitmap(path, 240) }
    var big by remember { mutableStateOf(false) }
    if (bmp != null) {
        Image(
            bitmap = bmp, contentDescription = "عکس معامله",
            modifier = Modifier.size(size).clip(RoundedCornerShape(10.dp)).clickable { big = true },
            contentScale = ContentScale.Crop
        )
    }
    if (big) {
        val full = remember(path, stamp) { loadBitmap(path, 2000) }
        Dialog(onDismissRequest = { big = false }) {
            if (full != null) {
                Image(
                    bitmap = full, contentDescription = "عکس معامله",
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
