package cn.scvtc.campus

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

val DefaultDockDestinations = listOf("grades", "courses", "settings")

internal fun restoredDockDestinations(saved: String?, schema: Int): List<String> {
    val ids = saved?.split(',')?.filter { it in AppExperiencePreferences.destinations }?.distinct()
        ?.takeIf { "settings" in it } ?: return DefaultDockDestinations
    // Migrate the two previously shipped defaults once, while retaining custom choices.
    return if (schema < 2 && (ids == listOf("courses", "settings") ||
        ids == listOf("courses", "grades", "sync", "settings"))) DefaultDockDestinations else ids
}

data class AppExperience(val entranceAnimation:Boolean=true,val dock:List<String> = DefaultDockDestinations,
    val showPeriodTimes:Boolean=true, val cardTint:Float=.55f, val detailOpacity:Float=.88f,
    val haptics:Boolean=true, val predictiveBack:Boolean=true, val materialQuality:Float=1f,
    val showBreakDividers:Boolean=true, val showTeacher:Boolean=true, val showLocation:Boolean=true,
    val glass: com.xiaomanjun.sleepdownschedule.glass.GlassAppearance = com.xiaomanjun.sleepdownschedule.glass.GlassAppearance(),
    val adaptiveStartupGlass:Boolean=true)

val LocalAppExperience = androidx.compose.runtime.compositionLocalOf { AppExperience() }

/** Device-local presentation choices, separate from accounts and academic records. */
class AppExperiencePreferences private constructor(context:Context) {
    private val prefs=context.applicationContext.getSharedPreferences("scvtc-app-experience",Context.MODE_PRIVATE)
    private val mutable=MutableStateFlow(read())
    val state=mutable.asStateFlow()
    private val mutablePresentationPreview=MutableStateFlow<AppExperience?>(null)
    val presentationPreview=mutablePresentationPreview.asStateFlow()
    fun previewPresentation(value:AppExperience?) { mutablePresentationPreview.value=value }
    private val mutablePreview=MutableStateFlow<com.xiaomanjun.sleepdownschedule.glass.GlassAppearance?>(null)
    val glassPreview=mutablePreview.asStateFlow()
    fun previewGlass(value:com.xiaomanjun.sleepdownschedule.glass.GlassAppearance?) { mutablePreview.value=value }
    fun saveGlass(value:com.xiaomanjun.sleepdownschedule.glass.GlassAppearance) {
        check(prefs.edit().putString("glass-v1",value.encode()).commit())
        mutable.value=read()
        mutablePreview.value=null
    }
    private fun read()=AppExperience(prefs.getBoolean("entrance",true),
        restoredDockDestinations(prefs.getString("dock",null), prefs.getInt("dock-schema", 0)),
        showPeriodTimes=prefs.getBoolean("period-times",true),
        cardTint=prefs.getFloat("card-tint",.55f).coerceIn(0f,1f),
        detailOpacity=prefs.getFloat("detail-opacity",.88f).coerceIn(.45f,1f),
        haptics=prefs.getBoolean("haptics",true), predictiveBack=prefs.getBoolean("predictive-back",true),
        materialQuality=prefs.getFloat("material-quality",1f).coerceIn(.5f,1f),
        showBreakDividers=prefs.getBoolean("break-dividers",true), showTeacher=prefs.getBoolean("show-teacher",true),
        showLocation=prefs.getBoolean("show-location",true),
        glass=com.xiaomanjun.sleepdownschedule.glass.GlassAppearance.decode(prefs.getString("glass-v1",null)),
        adaptiveStartupGlass=prefs.getBoolean("adaptive-startup-glass",true))
    fun presentation(value:AppExperience) {
        check(prefs.edit().putBoolean("period-times",value.showPeriodTimes)
            .putFloat("card-tint",value.cardTint.coerceIn(0f,1f))
            .putFloat("detail-opacity",value.detailOpacity.coerceIn(.45f,1f))
            .putBoolean("haptics",value.haptics).putBoolean("predictive-back",value.predictiveBack)
            .putFloat("material-quality",value.materialQuality.coerceIn(.5f,1f))
            .putBoolean("break-dividers",value.showBreakDividers).putBoolean("show-teacher",value.showTeacher)
            .putBoolean("show-location",value.showLocation)
            .putBoolean("adaptive-startup-glass",value.adaptiveStartupGlass).commit())
        mutable.value=read()
    }
    fun entrance(enabled:Boolean) {
        check(prefs.edit().putBoolean("entrance",enabled).commit())
        mutable.value=read()
    }
    fun dock(ids:List<String>) {
        require(ids.size in 1..4 && ids.distinct().size==ids.size && "settings" in ids && ids.all { it in destinations })
        check(prefs.edit().putString("dock",ids.joinToString(",")).putInt("dock-schema",2).commit())
        mutable.value=read()
    }
    companion object {
        val destinations=linkedMapOf("grades" to "成绩查询","courses" to "课表","settings" to "个人中心","sync" to "教务同步")
        @Volatile private var instance:AppExperiencePreferences?=null
        fun get(context:Context):AppExperiencePreferences=instance?:synchronized(this) {
            instance?:AppExperiencePreferences(context).also { instance=it }
        }
    }
}
