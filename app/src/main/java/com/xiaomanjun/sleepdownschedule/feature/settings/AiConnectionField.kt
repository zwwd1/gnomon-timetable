package com.xiaomanjun.sleepdownschedule.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.TextField

@Composable
internal fun AiConnectionField(title: String, value: String, onChange: (String) -> Unit,
    enabled: Boolean = true, secret: Boolean = false, uri: Boolean = false) {
    var revealed by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            if (secret) androidx.compose.material3.TextButton(onClick = { revealed = !revealed },
                modifier = Modifier.heightIn(min = 48.dp)) { Text(if (revealed) "隐藏" else "显示") }
        }
        TextField(value, onChange, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = if (uri) KeyboardType.Uri else KeyboardType.Ascii, imeAction = ImeAction.Next),
            visualTransformation = if (secret && !revealed) PasswordVisualTransformation() else VisualTransformation.None)
    }
}
