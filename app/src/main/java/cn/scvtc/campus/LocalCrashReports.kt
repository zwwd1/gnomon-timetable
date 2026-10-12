package cn.scvtc.campus

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import com.xiaomanjun.sleepdownschedule.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicBoolean

/** Local crash evidence only. Messages, URLs, web content and credentials are excluded. */
object LocalCrashReports {
    private val installed=AtomicBoolean()
    private val crashing=AtomicBoolean()
    private const val LIMIT=32*1024
    private var stage="启动"
    private var pending:String?=null
    private var showing=false
    private var foreground=java.lang.ref.WeakReference<Activity>(null)
    private fun file(app:Application,name:String)=AtomicFile(File(app.filesDir,"scvtc-errors/$name"))
    private fun read(app:Application,name:String):String?=runCatching{
        file(app,name).openRead().bufferedReader().use { reader ->
            val block=CharArray(LIMIT);var total=0
            while(total<LIMIT){val count=reader.read(block,total,LIMIT-total);if(count<=0)break;total+=count}
            if(total==0)null else String(block,0,total)
        }
    }.getOrNull()
    private fun write(app:Application,name:String,text:String) {
        val target=file(app,name);target.baseFile.parentFile?.mkdirs()
        val stream=target.startWrite()
        try{stream.write(text.take(LIMIT).toByteArray(Charsets.UTF_8));target.finishWrite(stream)}
        catch(e:Exception){target.failWrite(stream);throw e}
    }
    fun mark(app:Application,label:String) {
        stage=label.take(80)
        runCatching{write(app,"stage.txt",stage)}
    }
    fun install(app:Application) {
        if(Application.getProcessName()!=app.packageName||!installed.compareAndSet(false,true))return
        val previousStart=read(app,"started.txt")?.toLongOrNull()
        val previousStage=read(app,"stage.txt").orEmpty()
        pending=read(app,"pending.txt")
        val previousEnvironment=read(app,"environment.txt").orEmpty().ifBlank{"版本不可用"}
        runCatching {
            write(app,"started.txt",System.currentTimeMillis().toString())
            write(app,"environment.txt","App ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        }
        val original=Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler{failedThread,failure->
            try {
                if(crashing.compareAndSet(false,true)) {
                    val report=buildString {
                        appendLine("应用错误报告")
                        appendLine("时间："+SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.ROOT).apply{
                            timeZone=TimeZone.getTimeZone("Asia/Shanghai")
                        }.format(Date()))
                        appendLine("App ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        appendLine("Android API ${android.os.Build.VERSION.SDK_INT}")
                        appendLine("阶段：$stage")
                        appendLine("线程：${if(failedThread===Looper.getMainLooper().thread)"主线程" else "后台线程"}")
                        var cause:Throwable?=failure;var depth=0
                        while(cause!=null&&depth++<6) {
                            appendLine(cause.javaClass.name)
                            cause.stackTrace.take(64).forEach{appendLine("    at $it")}
                            cause=cause.cause
                        }
                        appendLine("仅保存在本机；不记录异常消息、账号、密码、Cookie 或网页内容。")
                    }.take(LIMIT)
                    write(app,"latest.txt",report);write(app,"pending.txt",report)
                }
            } catch(_:Throwable) {
                // Preserve the original crash; reporting cannot keep a broken process alive.
            } finally {
                try{original?.uncaughtException(failedThread,failure)}
                finally{android.os.Process.killProcess(android.os.Process.myPid())}
            }
        }
        app.registerActivityLifecycleCallbacks(object:Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity:Activity) {
                foreground=java.lang.ref.WeakReference(activity)
                mark(app,activity.javaClass.simpleName)
                showPending(app,activity)
            }
            override fun onActivityCreated(activity:Activity,state:Bundle?){}
            override fun onActivityStarted(activity:Activity){}
            override fun onActivityPaused(activity:Activity){if(foreground.get()===activity)foreground.clear()}
            override fun onActivityStopped(activity:Activity){}
            override fun onActivitySaveInstanceState(activity:Activity,state:Bundle){}
            override fun onActivityDestroyed(activity:Activity){}
        })
        Thread({
            if(pending!=null||previousStart==null)return@Thread
            val exit=runCatching{app.getSystemService(ActivityManager::class.java)
                ?.getHistoricalProcessExitReasons(app.packageName,0,8)?.firstOrNull {
                    it.processName==app.packageName&&it.timestamp>=previousStart&&
                        it.reason in setOf(ApplicationExitInfo.REASON_CRASH_NATIVE,ApplicationExitInfo.REASON_ANR)
                }}.getOrNull()?:return@Thread
            val time=SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.ROOT).apply{
                timeZone=TimeZone.getTimeZone("Asia/Shanghai")
            }.format(Date(exit.timestamp))
            val report="应用异常退出报告\n时间：$time\n$previousEnvironment\n"+
                "阶段：$previousStage\n系统原因：${if(exit.reason==ApplicationExitInfo.REASON_CRASH_NATIVE)"原生崩溃" else "应用无响应"}\n"+
                "系统未提供 Java 调用栈。未读取系统跟踪文件、账号、密码或网页内容。"
            runCatching{write(app,"latest.txt",report);write(app,"pending.txt",report)}
            Handler(Looper.getMainLooper()).post{pending=report;foreground.get()?.let{showPending(app,it)}}
        },"scvtc-error-recovery").apply{isDaemon=true;start()}
    }
    private fun showPending(app:Application,activity:Activity) {
        val report=pending?:return
        if(showing||activity.isFinishing||activity.isDestroyed)return
        showing=true
        android.app.AlertDialog.Builder(activity).setTitle("上次应用异常退出")
            .setMessage(report)
            .setPositiveButton("复制报告"){_,_->
                activity.getSystemService(ClipboardManager::class.java)
                    ?.setPrimaryClip(ClipData.newPlainText("应用错误报告",report))
            }.setNegativeButton("继续使用",null)
            .setOnDismissListener{
                pending=null;showing=false
                runCatching{file(app,"pending.txt").delete()}
            }.show()
    }
}
