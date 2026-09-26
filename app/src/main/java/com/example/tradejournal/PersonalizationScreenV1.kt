package com.example.tradejournal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** سربرگ مستقل «شخصی‌سازی» (فعلاً فقط نام). همون الگوی پیش‌نویس/ذخیره. */
@Composable
fun PersonalizationScreen(vm: TradeViewModel, onBack: () -> Unit) {
    var draftName by remember { mutableStateOf(vm.userName) }
    var confirmed by remember { mutableStateOf(false) }
    val dirty = draftName != vm.userName

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("بازگشت")) }
            Text(tr("شخصی‌سازی"), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }

        OutlinedTextField(
            draftName, { draftName = it }, label = { Text(tr("نام شما")) },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )

        if (dirty) Text(tr("تغییراتی داری که هنوز ذخیره نشده."), color = ColSaffron, fontSize = 12.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { vm.setName(draftName); confirmed = true },
                enabled = dirty,
                colors = ButtonDefaults.buttonColors(containerColor = ColSaffron, contentColor = ColOnSaffron),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) { Text(tr("ذخیره تغییرات"), fontWeight = FontWeight.ExtraBold) }

            if (dirty) TextButton(onClick = { draftName = vm.userName }, modifier = Modifier.height(50.dp)) {
                Text(tr("رها کردن تغییرات"))
            }
        }
    }

    if (confirmed) AlertDialog(
        onDismissRequest = { confirmed = false },
        confirmButton = { TextButton({ confirmed = false }) { Text(tr("باشه")) } },
        title = { Text(tr("ذخیره شد")) },
        text = { Text(tr("تغییرات با موفقیت اعمال شد.")) }
    )
}
