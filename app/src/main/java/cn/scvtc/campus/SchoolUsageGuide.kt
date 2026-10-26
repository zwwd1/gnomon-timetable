package cn.scvtc.campus

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cn.scvtc.campus.guide.SchoolGuideDeck
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LiquidContentDialog
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity

@Composable
fun SchoolUsageGuide(config: ScheduleConfigEntity, backdrop: Backdrop?, onDismiss: () -> Unit, onConnect: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { context.applicationContext.getSharedPreferences("school-illustrated-guide-v2", Context.MODE_PRIVATE) }
    val initialPage = remember(prefs) { prefs.getInt("page", 0).coerceIn(0, 5) }
    var visible by rememberSaveable { mutableStateOf(true) }
    var connectAfterClose by rememberSaveable { mutableStateOf(false) }
    LiquidContentDialog(visible, backdrop, config,
        onDismissRequest = { visible = false },
        onDismissFinished = { if (connectAfterClose) onConnect() else onDismiss() }) {
        SchoolGuideDeck("Gnomon 课程表", "个人中心", Modifier.fillMaxSize(), initialPage,
            reduceMotion = !android.animation.ValueAnimator.areAnimatorsEnabled(),
            onPageChanged = { prefs.edit().putInt("page", it).apply() },
            onClose = { visible = false },
            onFinish = { prefs.edit().putInt("page", 0).putBoolean("completed", true).apply(); visible = false },
            onConnect = { connectAfterClose = true; visible = false })
    }
}
