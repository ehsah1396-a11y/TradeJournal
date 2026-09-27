package com.example.tradejournal

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

/** اگه برای این نماد عکس اختصاصی داشته باشیم، شناسه‌ی drawable آن را برمی‌گرداند؛ وگرنه null */
private fun symbolDrawable(sym: String): Int? {
    val s = sym.uppercase()
    return when {
        s.contains("BTC") -> R.drawable.sym_btc
        s.contains("ETH") -> R.drawable.sym_eth
        s.contains("XAU") || s.contains("GOLD") -> R.drawable.sym_gold
        s.contains("XAG") || s.contains("SILVER") -> R.drawable.sym_silver
        s.contains("OIL") || s.contains("WTI") || s.contains("BRENT") -> R.drawable.sym_oil
        s.contains("DXY") -> R.drawable.sym_dxy
        s.contains("NAS100") || s.contains("NASDAQ") || s.contains("NDX") -> R.drawable.sym_nas100
        s.contains("SP500") || s.contains("US500") || s.contains("SPX") -> R.drawable.sym_sp500
        s.contains("EURUSD") -> R.drawable.sym_eurusd
        s.contains("GBPUSD") -> R.drawable.sym_gbpusd
        s.contains("USDJPY") -> R.drawable.sym_usdjpy
        s.contains("AAPL") || s.contains("APPLE") -> R.drawable.sym_apple
        else -> null
    }
}

/** عکس اختصاصی نماد را نشان می‌دهد؛ اگر عکسی نداشتیم، همان ایموجی قبلی جایگزینش می‌شود */
@Composable
fun SymbolBadge(sym: String, size: Dp) {
    val id = symbolDrawable(sym)
    if (id != null) {
        Image(
            painter = painterResource(id), contentDescription = sym,
            modifier = Modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop
        )
    } else {
        Text(symbolIcon(sym), fontSize = (size.value * 0.7f).sp)
    }
}
