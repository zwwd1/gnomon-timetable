package cn.scvtc.campus

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LiquidAlertAction
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LiquidAlertActionStyle
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LiquidAlertDialog
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.sleepDownPanelForegroundColor
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity

private val schoolUsageSteps = listOf(
    "先连接学校，也能稍后再用" to "填写川职学号和学校统一认证密码，点击“登录并自动同步”。密码加密保存在这台手机，身份核验后才读取课表。\n\n遇到验证码或安全确认，打开学校认证页完成，再回应用继续。登录成功不等于课表已经保存，请等到同步结果出现。\n\n暂不连接也能手动添加或导入课程。以后在个人中心 → 自动刷新课表 → 重新登录连接。",
    "找到课程和教室" to "底栏“课表”查看课程，切换日 / 周视图和教学周。点按课程放大查看老师、地点与时间；长按课程进入编辑。\n\n本周空白时，先核对当前课表、学期和周次，再检查同步结果。真实空周也可能没有课，不必删除原表或清除数据。\n\n成绩与学分可从底栏成绩入口或个人中心查看；未出分时可切往期或总体。",
    "校准日期，再开启提醒" to "个人中心 → 当前课表详细设置，核对第一周周一、总周数、节次和作息时间。按学校实际作息填写，不把未知时间当成真实上课时间。\n\n随后到个人中心 → 通知设置开启课程提醒，并按系统提示允许通知。未设置正确时间前，提醒和日历导出的时间可能不完整。\n\n桌面小组件由桌面添加；它与应用里的课表共用已保存数据。",
    "看懂同步结果" to "个人中心 → 自动刷新课表，可立即刷新或设置自动周期。查看最近成功时间和错误提示，等待当前任务完成，避免连续点刷新。\n\n断网、会话到期或学校服务异常时，已有课表会保留；恢复网络后再重试。学校要求本人验证或修改了密码时，重新连接教务。\n\n后台任务可能被系统延后。需要最新信息时手动刷新，再按需检查自启动和电量限制。",
    "导入、备份和换手机" to "个人中心里选择课表导入、课表导出或课表备份与恢复。导入先核对预览，再确认保存；完整迁移请使用备份功能，并检查作息和手动课程。\n\n导出时在系统文件选择器里选保存位置。普通导出不包含学校密码；WebDAV 需要自行配置并手动操作。\n\n更新应用请覆盖安装，保留原数据。换手机后需要重新登录，两款课程表不会自动共享账号和数据。",
    "调整流畅度，随时重看" to "个人中心 → 应用偏好设置，可调整材质质量、玻璃效果与入场动画。滑动不顺时可先试“均衡”或“低负载”，再按喜好调整，不必清空数据。\n\n系统关闭动画时，应用会遵循系统设置。个人中心 → 使用指南，可随时重新打开这些说明。\n\n反馈问题请附版本、机型、操作步骤和错误提示，并隐藏学号、密码、Cookie 和个人成绩。"
)

/** Shares the first-use explanation with the permanent settings entry. */
@Composable
fun SchoolUsageGuide(
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    onDismiss: () -> Unit,
    onConnect: () -> Unit
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val step = schoolUsageSteps[page]
    LiquidAlertDialog(
        title = "使用指南 · ${page + 1}/${schoolUsageSteps.size}",
        message = step.second,
        messageContent = {
            key(page) {
                Text("${step.first}\n\n${step.second}", modifier = Modifier.verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = sleepDownPanelForegroundColor(config).copy(alpha = 0.82f))
            }
        },
        actions = buildList {
            if (page > 0) add(LiquidAlertAction("上一步", LiquidAlertActionStyle.Secondary,
                dismissOnClick = false) { page = (page - 1).coerceAtLeast(0) })
            add(LiquidAlertAction(if (page == schoolUsageSteps.lastIndex) "完成" else "稍后再看",
                LiquidAlertActionStyle.Secondary, onClick = onDismiss))
            if (page < schoolUsageSteps.lastIndex) {
                add(LiquidAlertAction("下一步", LiquidAlertActionStyle.Primary,
                    dismissOnClick = false) { page = (page + 1).coerceAtMost(schoolUsageSteps.lastIndex) })
            } else {
                add(LiquidAlertAction("连接教务", LiquidAlertActionStyle.Primary, onClick = onConnect))
            }
        },
        config = config,
        backdrop = backdrop,
        onDismissRequest = onDismiss
    )
}
