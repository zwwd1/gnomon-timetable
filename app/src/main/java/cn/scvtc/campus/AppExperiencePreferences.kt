package cn.scvtc.campus

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppExperience(val entranceAnimation:Boolean=true,val dock:List<String> = listOf("courses","settings"),
    val glass: com.xiaomanjun.sleepdownschedule.glass.GlassAppearance = com.xiaomanjun.sleepdownschedule.glass.GlassAppearance())

/** Device-local presentation choices, separate from accounts and academic records. */
class AppExperiencePreferences private constructor(context:Context) {
    private val prefs=context.applicationContext.getSharedPreferences("scvtc-app-experience",Context.MODE_PRIVATE)
    private val mutable=MutableStateFlow(read())
    val state=mutable.asStateFlow()
    private val mutablePreview=MutableStateFlow<com.xiaomanjun.sleepdownschedule.glass.GlassAppearance?>(null)
    val glassPreview=mutablePreview.asStateFlow()
    fun previewGlass(value:com.xiaomanjun.sleepdownschedule.glass.GlassAppearance?) { mutablePreview.value=value }
    fun saveGlass(value:com.xiaomanjun.sleepdownschedule.glass.GlassAppearance) {
        check(prefs.edit().putString("glass-v1",value.encode()).commit())
        mutable.value=read()
        mutablePreview.value=null
    }
    private fun read()=AppExperience(prefs.getBoolean("entrance",true),
        prefs.getString("dock",null)?.split(',')?.filter { it in destinations }?.distinct()
            ?.takeIf { "settings" in it } ?: listOf("courses","settings"),
        com.xiaomanjun.sleepdownschedule.glass.GlassAppearance.decode(prefs.getString("glass-v1",null)))
    fun entrance(enabled:Boolean) {
        check(prefs.edit().putBoolean("entrance",enabled).commit())
        mutable.value=read()
    }
    fun dock(ids:List<String>) {
        require(ids.size in 1..4 && ids.distinct().size==ids.size && "settings" in ids && ids.all { it in destinations })
        check(prefs.edit().putString("dock",ids.joinToString(",")).commit())
        mutable.value=read()
    }
    companion object {
        val destinations=linkedMapOf("courses" to "课程","grades" to "成绩","sync" to "教务","settings" to "设置")
        @Volatile private var instance:AppExperiencePreferences?=null
        fun get(context:Context):AppExperiencePreferences=instance?:synchronized(this) {
            instance?:AppExperiencePreferences(context).also { instance=it }
        }
    }
}
