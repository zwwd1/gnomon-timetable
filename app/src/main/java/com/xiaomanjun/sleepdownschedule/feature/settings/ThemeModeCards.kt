package com.xiaomanjun.sleepdownschedule.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.shapes.RoundedRectangle
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity

@Composable
internal fun ThemeModeCards(config: ScheduleConfigEntity, change: (ScheduleConfigEntity) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(false, true).forEach { dark ->
            val selected = !config.followSystemDarkMode && config.darkMode == dark
            val base = if (dark) Color(0xFF202631) else Color(0xFFF2F6FC)
            Column(Modifier.weight(1f).border(if (selected) 2.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedRectangle(18.dp)).background(base, RoundedRectangle(18.dp))
                .clickable(onClickLabel = if (dark) "切换深色模式" else "切换浅色模式") {
                    change(config.copy(followSystemDarkMode = false, darkMode = dark))
                }.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Column(Modifier.fillMaxWidth().background(if (dark) Color(0xFF343F51) else Color.White, RoundedRectangle(10.dp)).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.fillMaxWidth(.85f).height(4.dp).background(Color(0xFF79ACEC), RoundedRectangle(2.dp)))
                        Box(Modifier.fillMaxWidth(.55f).height(4.dp).background(Color(0xFF79ACEC).copy(alpha = .5f), RoundedRectangle(2.dp)))
                    }
                }
                Text((if (dark) "深色模式" else "浅色模式") + if (selected) " · 已选" else "",
                    color = if (dark) Color.White else Color(0xFF25354A), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
