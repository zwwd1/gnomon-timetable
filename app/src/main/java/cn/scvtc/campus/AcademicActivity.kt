package cn.scvtc.campus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.catalog.components.LiquidBottomTabs
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.xiaomanjun.sleepdownschedule.core.ui.settings.GlassMiuixSettingsTheme
import com.xiaomanjun.sleepdownschedule.core.ui.settings.GlassMiuixRootSettingsScaffold
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.DialogLiquidButton
import com.xiaomanjun.sleepdownschedule.app.ui.DockScrollPadding
import com.xiaomanjun.sleepdownschedule.MainActivity
import com.xiaomanjun.sleepdownschedule.feature.home.RootDestinationRequests
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.scvtc.campus.core.CreditSummary
import cn.scvtc.campus.core.NativeRecord
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import com.xiaomanjun.sleepdownschedule.CourseScheduleTheme
import com.xiaomanjun.sleepdownschedule.app.ui.DetailActivityScaffold
import com.xiaomanjun.sleepdownschedule.app.ui.detailContentTopPadding
import com.xiaomanjun.sleepdownschedule.feature.schedule.autorefresh.AutoRefreshScheduleStore
import com.xiaomanjun.sleepdownschedule.feature.settings.SettingsGroup
import com.xiaomanjun.sleepdownschedule.feature.settings.SettingsActionButton
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.utils.overScrollVertical
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SleepDownLiquidDropdownPreference
import java.math.BigDecimal

/** Real academic records, using SleepDown's existing theme and detail scaffold. */
class AcademicActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RootDestinationRequests.open("grades")
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }
}

@Composable
fun AcademicContent(config: ScheduleConfigEntity, backdrop: Backdrop?, autoRefresh: Boolean = true) {
    GlassMiuixRootSettingsScaffold(title = "成绩与学分", config = config) { innerPadding ->
        AcademicBody(config, backdrop, autoRefresh, innerPadding)
    }
}

@Composable
private fun AcademicBody(config: ScheduleConfigEntity, backdrop: Backdrop?, autoRefresh: Boolean,
                         innerPadding: PaddingValues) {
            val context = LocalContext.current
            val app = context.applicationContext as CourseScheduleApp
            val profile by AutoRefreshScheduleStore.observe(app).collectAsState()
            val account = profile?.takeIf { it.schoolId == ScvtcNativeBridge.SCHOOL }?.username.orEmpty()
            var snapshot by remember(account) { mutableStateOf<AcademicSnapshot?>(null) }
            var tab by rememberSaveable(account) { mutableIntStateOf(0) }
            var selectedTerm by rememberSaveable(account) { mutableStateOf("all") }
            var query by rememberSaveable(account) { mutableStateOf("") }
            var sort by rememberSaveable(account) { mutableIntStateOf(0) }
            var scoreFilter by rememberSaveable(account) { mutableIntStateOf(0) }
            val state by AcademicRepository.state.collectAsState()
            LaunchedEffect(account) {
                if (account.isNotBlank()) {
                    snapshot = withContext(Dispatchers.IO) { AcademicRepository.cached(app, account) }
                    if (snapshot == null && autoRefresh) AcademicRepository.requestRefresh(app, account)
                }
            }
            LaunchedEffect(account, state.account, state.busy, state.revision) {
                if (account.isNotBlank() && state.account == account && (!state.busy || state.revision > 0))
                    snapshot = withContext(Dispatchers.IO) { AcademicRepository.cached(app, account) }
            }
            val terms = remember(snapshot, selectedTerm) {
                listOf("all") + (snapshot?.terms.orEmpty() + listOf(selectedTerm).filter { it != "all" }).distinct().sortedDescending()
            }
            val termRecords = remember(snapshot, selectedTerm) {
                snapshot?.grades.orEmpty().filter { selectedTerm == "all" || it.fields["学期"] == selectedTerm }
            }
            val visibleRecords = remember(termRecords, query, sort, scoreFilter) {
                val search = query.trim()
                val matching = termRecords.withIndex().filter {
                    (it.value.title.contains(search, true) || it.value.fields["课程代码"].orEmpty().contains(search, true)) &&
                        (scoreFilter==0 || academicFailed(it.value))
                }
                when (sort) {
                    1 -> matching.sortedWith(compareByDescending<IndexedValue<NativeRecord>> { it.value.fields["成绩"]?.toBigDecimalOrNull() }.thenBy { it.value.title })
                    2 -> matching.sortedWith(compareByDescending<IndexedValue<NativeRecord>> { it.value.fields["学分"]?.toBigDecimalOrNull() }.thenBy { it.value.title })
                    else -> matching
                }
            }
                    LazyColumn(Modifier.fillMaxSize().overScrollVertical(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = innerPadding.calculateTopPadding() + 8.dp,
                            bottom = DockScrollPadding + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item(key = "heading") {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(if (snapshot == null) "学校记录 · 本机离线保存" else "${snapshot!!.grades.size} 条成绩 · 本机已保存",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (account.isNotBlank() && autoRefresh) DialogLiquidButton(backdrop, if (state.busy) "同步中" else "刷新", {
                                    if (!state.busy) AcademicRepository.requestRefresh(app, account)
                                }, monochromeNeutral = true)
                            }
                        }
                        if (account.isBlank()) item {
                            SettingsGroup(backdrop, config) {
                                Text("连接一次学校账号后，可查看真实成绩与学分，并离线保存记录。", Modifier.padding(16.dp))
                                if (autoRefresh) SettingsActionButton("连接学校教务", backdrop, onClick = {
                                    context.startActivity(Intent(context, ScvtcLoginActivity::class.java))
                                }, modifier = Modifier.padding(16.dp).fillMaxWidth())
                            }
                        } else {
                            item {
                                SettingsGroup(backdrop, config) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(if (state.account == account && state.message.isNotBlank()) state.message
                                            else if (snapshot == null) "尚未取得学校记录" else "本机成绩已保存，可离线查看")
                                        snapshot?.let { Text("最近成功同步：" + java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it.fetchedAt)),
                                            style = MaterialTheme.typography.bodySmall) }
                                        if (autoRefresh && state.account == account && state.authRequired) SettingsActionButton("补充学校认证", backdrop, onClick = {
                                            context.startActivity(Intent(context, ScvtcLoginActivity::class.java))
                                        }, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                            item { AcademicSegments(listOf("成绩", "学分"), tab, backdrop) { tab = it } }
                            if (tab == 0) {
                                item {
                                    SettingsGroup(backdrop, config) {
                                        SleepDownLiquidDropdownPreference(items = terms.map { if (it == "all") "全部学期" else it }, backdrop = backdrop, config = config,
                                            selectedIndex = terms.indexOf(selectedTerm).coerceAtLeast(0), title = "学期",
                                            onSelectedIndexChange = { selectedTerm = terms[it] })
                                        SleepDownLiquidDropdownPreference(items = listOf("学校顺序", "成绩从高到低", "学分从高到低"), backdrop = backdrop, config = config,
                                            selectedIndex = sort, title = "排序", onSelectedIndexChange = { sort = it })
                                    }
                                }
                                item {
                                    SettingsGroup(backdrop, config) {
                                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            AcademicStat("加权绩点", academicGpa(termRecords), Modifier.weight(1f))
                                            AcademicStat("确认学分", if (snapshot == null) "--" else CreditSummary.from(termRecords).display, Modifier.weight(1f))
                                            AcademicStat("成绩记录", if (snapshot == null) "--" else termRecords.size.toString(), Modifier.weight(1f))
                                        }
                                    }
                                }
                                item {
                                    TextField(query, { query = it }, label = "搜索课程名称或代码", modifier = Modifier.fillMaxWidth())
                                    AcademicSegments(listOf("全部成绩","需要留意"),scoreFilter,backdrop,Modifier.padding(top=12.dp)) { scoreFilter=it }
                                    if(scoreFilter==1) Text("显示未通过的数值成绩与学校标注的不及格记录。",
                                        Modifier.padding(top=8.dp),style=MaterialTheme.typography.bodySmall)
                                    Text("显示 ${visibleRecords.size} / ${termRecords.size} 条 · 点卡片查看详情", Modifier.padding(top = 8.dp),
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                if (visibleRecords.isEmpty()) item {
                                    SettingsGroup(backdrop, config) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(if (snapshot == null && state.busy && state.account==account) "正在读取学校成绩…" else if (snapshot == null) "尚无已验证记录" else if(scoreFilter==1) "当前筛选没有需要留意的成绩" else if (query.isNotBlank()) "没有匹配的课程" else "该学期暂无成绩")
                                            Text(if (query.isNotBlank()) "试试课程名称或代码，或清空搜索条件。" else
                                                "刷新未成功时仍保留原缓存；不会把失败当成学校没有成绩。", style = MaterialTheme.typography.bodySmall)
                                            if (query.isNotBlank()||scoreFilter!=0) SettingsActionButton("显示全部", backdrop, onClick = { query = "";scoreFilter=0 })
                                        }
                                    }
                                }
                                items(visibleRecords, key = { scvtcKey(it.value.title + it.value.fields.toString()) + ":" + it.index }) { entry ->
                                    val record = entry.value
                                    var expanded by rememberSaveable(record) { mutableStateOf(false) }
                                    SettingsGroup(backdrop, config) {
                                        Column(Modifier.fillMaxWidth().clickable(role = Role.Button,
                                            onClickLabel = if (expanded) "收起成绩详情" else "展开成绩详情") { expanded = !expanded }
                                            .heightIn(min = 48.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(record.title, fontWeight = FontWeight.Medium)
                                                    Text(listOfNotNull(record.fields["课程性质"], record.fields["学期"]).filter(String::isNotBlank).joinToString(" · "),
                                                        style = MaterialTheme.typography.bodySmall)
                                                }
                                                Text(record.fields["成绩"].orEmpty().ifBlank { "--" }, style = MaterialTheme.typography.headlineSmall,
                                                    color = if(academicFailed(record)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                            }
                                            Text("学分 ${record.fields["学分"].orEmpty().ifBlank { "--" }} · 绩点 ${record.fields["绩点"].orEmpty().ifBlank { "--" }} · ${if (expanded) "收起详情 ▴" else "查看详情 ▾"}",
                                                style = MaterialTheme.typography.bodyMedium)
                                            AnimatedVisibility(expanded) {
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    record.fields.filterKeys { it != "课程名称" }.forEach { (label, value) ->
                                                        Text("$label：$value", style = MaterialTheme.typography.bodyMedium)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                item {
                                    val summary = CreditSummary.from(snapshot?.grades.orEmpty())
                                    SettingsGroup(backdrop, config) {
                                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text("成绩中已确认获得", style = MaterialTheme.typography.titleMedium)
                                            Text(if (snapshot == null) "--" else "${summary.display} 学分", style = MaterialTheme.typography.headlineLarge,
                                                color = MaterialTheme.colorScheme.primary)
                                            Text("学校毕业学分要求：${snapshot?.requiredCredits?.ifBlank { "尚未读取" } ?: "尚未读取"}")
                                            snapshot?.takeIf { it.requiredCreditsFetchedAt > 0 }?.let {
                                                Text("毕业要求最近成功同步：" + java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it.requiredCreditsFetchedAt)),
                                                    style = MaterialTheme.typography.bodySmall)
                                            }
                                            val requirement = snapshot?.requiredCredits?.toBigDecimalOrNull()
                                            if (snapshot != null && requirement != null && requirement.signum() > 0) {
                                                val ratio=summary.confirmed.divide(requirement,4,java.math.RoundingMode.HALF_UP).toFloat().coerceIn(0f,1f)
                                                AcademicCreditMeter(ratio)
                                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                                                    Text("已确认 ${summary.display}",style=MaterialTheme.typography.bodySmall)
                                                    Text("要求 "+requirement.stripTrailingZeros().toPlainString(),style=MaterialTheme.typography.bodySmall)
                                                }
                                                Text("距离学分要求还差 ${(requirement - summary.confirmed).max(BigDecimal.ZERO).stripTrailingZeros().toPlainString()} 学分")
                                            }
                                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                                                AcademicStat("确认课程",if(snapshot==null)"--"else summary.courses.toString(),Modifier.weight(1f))
                                                AcademicStat("待核对记录",if(snapshot==null)"--"else summary.uncounted.toString(),Modifier.weight(1f))
                                            }
                                            Text("重修按课程代码合并，取学校返回的获得学分最大值。缺少必要字段的记录保留在成绩页，暂不计入所得。",
                                                style=MaterialTheme.typography.bodySmall)
                                            Text("仅比较学校学分要求和已确认所得，不代表毕业资格审核通过。", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                item {
                                    SettingsGroup(backdrop, config) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("各学期成绩所得", fontWeight = FontWeight.Medium)
                                            Text("点击学期查看对应课程",style=MaterialTheme.typography.bodySmall)
                                            snapshot?.terms.orEmpty().filter { term -> snapshot!!.grades.any { it.fields["学期"] == term } }.forEach { term ->
                                                val records = snapshot!!.grades.filter { it.fields["学期"] == term }
                                                Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { selectedTerm = term; tab = 0 }
                                                    .heightIn(min = 64.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                                        Text(term)
                                                        Text("${records.size} 条成绩",style=MaterialTheme.typography.bodySmall)
                                                    }
                                                    Text("${CreditSummary.from(records).display} 学分  ›")
                                                }
                                            }
                                            Text("学期数据独立汇总；跨学期重修不能把各学期所得直接相加。", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
}

@Composable
private fun AcademicSegments(titles: List<String>, selected: Int, backdrop: Backdrop?, modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit) {
    if (backdrop == null) { TabRow(titles, selected, onSelected, modifier); return }
    LiquidBottomTabs({ selected }, onSelected, backdrop, titles.size, modifier.fillMaxWidth(),
        containerHeight = 48.dp, indicatorHeight = 40.dp, blurRadius = 8.dp,
        lensHeight = 8.dp, lensAmount = 12.dp, pressedContentScale = 1.04f) {
        titles.forEachIndexed { index, title -> LiquidBottomTab(onClick = { onSelected(index) }) {
            Text(title, style = MaterialTheme.typography.labelLarge,
                color = if (selected == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        } }
    }
}

@Composable
private fun AcademicStat(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

private fun academicFailed(record:NativeRecord):Boolean {
    val score=record.fields["成绩"].orEmpty().trim()
    return score.toBigDecimalOrNull()?.let{it<BigDecimal("60")} ?: (score in setOf("不及格","不合格","未通过"))
}

@Composable
private fun AcademicCreditMeter(ratio:Float) {
    Box(Modifier.fillMaxWidth().height(12.dp)
        .semantics { progressBarRangeInfo=ProgressBarRangeInfo(ratio,0f..1f) }
        .background(MaterialTheme.colorScheme.onSurface.copy(alpha=0.12f),RoundedCornerShape(6.dp))) {
        Box(Modifier.fillMaxWidth(ratio).fillMaxHeight()
            .background(MaterialTheme.colorScheme.primary,RoundedCornerShape(6.dp)))
    }
}

private fun academicGpa(records: List<NativeRecord>): String {
    val pairs = records.mapNotNull {
        val credit = it.fields["学分"]?.toBigDecimalOrNull()?.takeIf { value -> value.signum() > 0 } ?: return@mapNotNull null
        val point = it.fields["绩点"]?.toBigDecimalOrNull()?.takeIf { value -> value.signum() >= 0 } ?: return@mapNotNull null
        credit to point
    }
    val weight = pairs.fold(BigDecimal.ZERO) { sum, pair -> sum + pair.first }
    return if (weight.signum() == 0) "--" else pairs.fold(BigDecimal.ZERO) { sum, pair -> sum + pair.first * pair.second }
        .divide(weight, 2, java.math.RoundingMode.HALF_UP).toPlainString()
}
