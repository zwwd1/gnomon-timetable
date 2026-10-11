package cn.scvtc.campus

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.scvtc.campus.core.School
import com.xiaomanjun.sleepdownschedule.R
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import com.xiaomanjun.sleepdownschedule.CourseScheduleTheme
import com.xiaomanjun.sleepdownschedule.app.ui.DetailActivityScaffold
import com.xiaomanjun.sleepdownschedule.app.ui.HomeIconButton
import com.xiaomanjun.sleepdownschedule.app.ui.detailContentTopPadding
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SleepDownLiquidCascadingPopup
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SleepDownLiquidMenuItem
import com.xiaomanjun.sleepdownschedule.feature.settings.SettingsActionButton
import com.xiaomanjun.sleepdownschedule.feature.settings.SettingsGroup
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.feature.schedule.autorefresh.AutoRefreshScheduleStore
import kotlinx.coroutines.*
import top.yukonga.miuix.kmp.basic.TextField

class SchoolLoginForm : ViewModel() {
    var account by mutableStateOf("")
    var password by mutableStateOf("")
    override fun onCleared() { password="" }
}

class ScvtcLoginActivity : ComponentActivity() {
    private var officialWeb by mutableStateOf<WebView?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScvtcNativeBridge.interactiveLogin=true
        enableEdgeToEdge()
        val form=ViewModelProvider(this)[SchoolLoginForm::class.java]
        val app=application as CourseScheduleApp
        val memory=OfficialLoginMemory(app)
        if(form.account.isBlank())form.account=AutoRefreshScheduleStore.load(app)?.username.orEmpty().ifBlank{memory.lastInputAccount()}
        if(form.password.isBlank())form.password=memory.inputPassword(form.account)
        setContent {
            var config by remember{mutableStateOf(ScheduleConfigEntity(totalWeeks=20,currentWeek=1,notificationLeadMinutes=10))}
            var awaiting by rememberSaveable{mutableStateOf(false)}
            var verification by rememberSaveable{mutableStateOf(false)}
            var full by rememberSaveable{mutableStateOf(false)}
            var menu by remember{mutableStateOf(false)}
            var handingOff by remember{mutableStateOf(false)}
            var slowConnection by remember{mutableStateOf(false)}
            var showPassword by rememberSaveable{mutableStateOf(false)}
            var localError by remember{mutableStateOf("")}
            val state by ScvtcNativeBridge.loginState.collectAsState()
            val scope=rememberCoroutineScope()
            val focus=LocalFocusManager.current
            val busy=state.busy||handingOff
            val openOfficial:()->Unit={
                if(!handingOff){
                    focus.clearFocus();awaiting=false;handingOff=true;localError=""
                    scope.launch {
                        try {
                            ScvtcNativeBridge.cancelSignIn()
                            if(form.account.matches(Regex("[0-9]{6,20}"))&&form.password.length in 1..512)
                                memory.rememberInput(form.account,form.password)
                            verification=true
                        } catch(e:CancellationException){throw e}
                        catch(_:Exception){localError="未能准备学校页面，请重试"}
                        finally{handingOff=false}
                    }
                }
            }
            val submit:()->Unit={
                if(!busy){
                    when{
                        !form.account.matches(Regex("[0-9]{6,20}"))->localError="请填写完整学号"
                        form.password.length !in 1..512->localError="请填写学校密码"
                        else->{
                            focus.clearFocus();localError="";awaiting=true
                            val task=ScvtcNativeBridge.signIn(app,form.account,form.password)
                            scope.launch{try{task.await()}catch(e:CancellationException){throw e}catch(_:Exception){}}
                        }
                    }
                }
            }
            fun finishVerified(){form.password="";setResult(RESULT_OK);finish()}
            fun back(){
                if(full)full=false
                else if(verification&&officialWeb?.canGoBack()==true)officialWeb?.goBack()
                else if(verification)verification=false
                else finish()
            }
            LaunchedEffect(Unit){config=app.repository.activeSnapshot().config}
            LaunchedEffect(busy){slowConnection=false;if(busy){delay(8000);slowConnection=true}}
            LaunchedEffect(state,awaiting){
                if(awaiting&&state.account==form.account&&state.verified)finishVerified()
                else if(awaiting&&!state.busy&&state.account.isBlank()){awaiting=false;localError="上次学校认证被中断，请重试"}
            }
            BackHandler(verification||full){back()}
            BackHandler(menu){menu=false}
            CourseScheduleTheme(config=config){
                DetailActivityScaffold(if(verification)"学校统一身份认证"else"连接川职教务",config,
                    onBack={back()},compactTopBar=true,topBarVisible=!full,
                    topBarActions={backdrop->
                        Box {
                            HomeIconButton(backdrop,config,R.drawable.ic_more_horizontal,"学校认证操作",selected=false,onClick={menu=true})
                            val items=if(verification)listOf(
                                SleepDownLiquidMenuItem("home","返回教务首页",onClick={officialWeb?.loadUrl(School.HOME)}),
                                SleepDownLiquidMenuItem("portal","一网通办",onClick={officialWeb?.loadUrl(School.PORTAL)}),
                                SleepDownLiquidMenuItem("reload","刷新当前页面",onClick={officialWeb?.reload()}),
                                SleepDownLiquidMenuItem("auth","重新认证",onClick={officialWeb?.loadUrl(CasAuthManager.JWXT_SSO_ENTRY)}),
                                SleepDownLiquidMenuItem("form","返回账号密码登录",onClick={verification=false}),
                                SleepDownLiquidMenuItem("full","全屏浏览",onClick={full=true}),
                                SleepDownLiquidMenuItem("browser","在系统浏览器打开",onClick={
                                    runCatching{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(officialWeb?.url?:CasAuthManager.JWXT_SSO_ENTRY)))}
                                        .onFailure{localError="系统没有可用的浏览器"}
                                })
                            )else listOf(SleepDownLiquidMenuItem("official","打开学校认证页",onClick=openOfficial))
                            SleepDownLiquidCascadingPopup(menu,Rect.Zero,items,{menu=false},backdrop,config,
                                panelMinWidth=220.dp,horizontalSafeInset=12.dp)
                        }
                    }
                ){backdrop->
                    if(verification){
                        Box(Modifier.fillMaxSize()){
                            SchoolVerification(form.account,form.password,
                                Modifier.fillMaxSize().padding(top=if(full)0.dp else detailContentTopPadding())
                                    .imePadding().navigationBarsPadding()){finishVerified()}
                            if(full)SettingsActionButton("退出全屏",backdrop,onClick={full=false},
                                modifier=Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp),monochrome=true)
                        }
                    }else Box(Modifier.fillMaxSize().padding(top=detailContentTopPadding()).imePadding()
                        .navigationBarsPadding(),contentAlignment=Alignment.TopCenter){
                        Column(Modifier.widthIn(max=560.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                            verticalArrangement=Arrangement.spacedBy(16.dp)){
                            SettingsGroup(backdrop,config){
                                Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
                                    Text("连接一次，课表随行",style=MaterialTheme.typography.titleMedium)
                                    Text("使用学校统一认证的学号和密码。身份通过后先进入应用，课表和成绩在后台同步。",
                                        style=MaterialTheme.typography.bodyMedium)
                                    TextField(form.account,{value->
                                        val previouslyRemembered=memory.inputPassword(form.account)
                                        val replacePassword=form.password.isBlank()||previouslyRemembered.isNotEmpty()&&form.password==previouslyRemembered
                                        form.account=value.trim();localError="";awaiting=false
                                        if(replacePassword)form.password=memory.inputPassword(form.account)
                                    },label="学号",enabled=!busy,singleLine=true,
                                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number,imeAction=ImeAction.Next),
                                        modifier=Modifier.fillMaxWidth())
                                    TextField(form.password,{form.password=it;localError="";awaiting=false},label="学校密码",
                                        enabled=!busy,singleLine=true,
                                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,imeAction=ImeAction.Done),
                                        keyboardActions=KeyboardActions(onDone={submit()}),
                                        visualTransformation=if(showPassword)VisualTransformation.None else PasswordVisualTransformation(),
                                        modifier=Modifier.fillMaxWidth())
                                    TextButton(onClick={showPassword=!showPassword},enabled=!busy,
                                        modifier=Modifier.align(Alignment.End)){Text(if(showPassword)"隐藏密码"else"显示密码")}
                                    val error=localError.ifBlank{
                                        if(awaiting&&state.account==form.account&&!state.busy&&!state.verified)state.message else ""
                                    }
                                    if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error,
                                        modifier=Modifier.semantics{liveRegion=LiveRegionMode.Polite})
                                    if(busy){
                                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                                            horizontalArrangement=Arrangement.spacedBy(12.dp)){
                                            CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp)
                                            Text(if(handingOff)"正在打开官方页面…"else state.message,
                                                modifier=Modifier.weight(1f).semantics{liveRegion=LiveRegionMode.Polite},
                                                style=MaterialTheme.typography.bodyMedium)
                                        }
                                        if(slowConnection)Text("连接较慢，可以打开下方的官方认证页继续",
                                            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                        SettingsActionButton("取消本次登录",backdrop,onClick={
                                            if(!handingOff)scope.launch{ScvtcNativeBridge.cancelSignIn()}
                                        },modifier=Modifier.fillMaxWidth(),monochrome=true)
                                    }else SettingsActionButton("登录并自动同步",backdrop,onClick=submit,
                                        modifier=Modifier.fillMaxWidth())
                                }
                            }
                            Text("密码按账号加密保存在本机，用于下次登录和会话恢复；不会写入普通导出或云端。",
                                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            SettingsActionButton("打开学校认证页",backdrop,onClick=openOfficial,
                                modifier=Modifier.fillMaxWidth(),monochrome=true)
                            Text("遇到验证码或账号安全提示，可随时在官方页面继续。学校暂时无法读取数据时，已有课表会保留。",
                                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
    /** Probe only an actual JWXT page; typing in CAS never triggers API polling. */
    @Composable
    private fun SchoolVerification(account:String,password:String,modifier:Modifier,onVerified:()->Unit){
        var message by remember{mutableStateOf("请在学校官方页面完成认证。成功后，应用会自动继续同步。")}
        var documentReady by remember{mutableStateOf(false)}
        var detectedAccount by remember{mutableStateOf("")}
        var probeEpoch by remember{mutableIntStateOf(0)}
        var browserEpoch by remember{mutableIntStateOf(0)}
        var rendererGone by remember(browserEpoch){mutableStateOf(false)}
        var checking by remember{mutableStateOf(false)}
        val app=application as CourseScheduleApp
        val memory=remember(browserEpoch){OfficialLoginMemory(app)}
        val verifiedCallback by rememberUpdatedState(onVerified)
        val web=remember(account,browserEpoch){
            WebView(this).apply{
                settings.apply{
                    javaScriptEnabled=true;domStorageEnabled=true;useWideViewPort=true;loadWithOverviewMode=true
                    allowFileAccess=false;allowContentAccess=false;mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
                }
                webChromeClient=WebChromeClient()
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this,true)
                memory.install(this,onIdentity={value->detectedAccount=value;probeEpoch++}){account}
                var homeRetried=false
                webViewClient=object:WebViewClient(){
                    override fun onPageStarted(view:WebView,url:String?,icon:android.graphics.Bitmap?){
                        documentReady=false;detectedAccount="";message="正在打开学校页面…";probeEpoch++
                    }
                    override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean{
                        if(!request.isForMainFrame)return false
                        val u=request.url
                        if(memory.recoveryNavigation(view,account,u.toString()))return true
                        val trusted=u.scheme=="https"&&u.userInfo==null&&
                            (u.host in School.allowedHosts+"cas.scvtc.edu.cn"&&u.port in setOf(-1,443)||
                                u.host=="www.shulin-soft.com"&&u.port==8267&&u.path=="/casLogin.html")
                        if(!trusted)message="该链接不在学校认证范围内，可从菜单用系统浏览器打开"
                        return !trusted
                    }
                    override fun onPageFinished(view:WebView,url:String){
                        if(rendererGone||view.url!=url)return
                        memory.prepare(view);CookieManager.getInstance().flush()
                        if(Uri.parse(url).host=="www.shulin-soft.com"){view.loadUrl(CasAuthManager.JWXT_SSO_ENTRY);return}
                        if(memory.recoveryNavigation(view,account,url))return
                        documentReady=Uri.parse(url).host=="jwxt.scvtc.edu.cn"
                        message=if(documentReady)"正在核验学校身份…"else"请完成学校要求的认证或账号安全提示"
                        if(account.isNotBlank()&&Uri.parse(url).host=="cas.scvtc.edu.cn"){
                            if(password.isNotEmpty())memory.configure(view,account,password){}
                            else memory.recover(view,account){}
                        }
                        probeEpoch++
                    }
                    override fun onReceivedError(view:WebView,request:WebResourceRequest,error:WebResourceError){
                        if(request.isForMainFrame){documentReady=false;memory.failedNetwork(account);message="学校页面连接失败，可从菜单刷新或重新认证"}
                    }
                    override fun onReceivedHttpError(view:WebView,request:WebResourceRequest,response:WebResourceResponse){
                        if(!request.isForMainFrame)return
                        documentReady=false
                        if(request.url.host=="jwxt.scvtc.edu.cn"&&request.url.path=="/api/cas/login"&&!homeRetried){
                            homeRetried=true;view.loadUrl(School.HOME)
                        }else message="学校页面暂不可用（HTTP "+response.statusCode+"），可刷新重试"
                    }
                    override fun onRenderProcessGone(view:WebView,detail:RenderProcessGoneDetail):Boolean{
                        documentReady=false;rendererGone=true;officialWeb=null
                        (view.parent as? android.view.ViewGroup)?.removeView(view);view.destroy()
                        message="学校页面已关闭，点击重新打开；已填内容保留";return true
                    }
                }
            }
        }
        DisposableEffect(web){
            officialWeb=web;web.onResume()
            onDispose{if(officialWeb===web)officialWeb=null;if(!rendererGone){web.stopLoading();web.destroy()}}
        }
        LaunchedEffect(web){web.loadUrl(CasAuthManager.JWXT_SSO_ENTRY)}
        LaunchedEffect(web,probeEpoch){
            if(!documentReady||rendererGone)return@LaunchedEffect
            val expected=account.ifBlank{detectedAccount}
            if(expected.isBlank()){message="请完成学校登录，身份确认后会自动继续";return@LaunchedEffect}
            checking=true
            try{
                repeat(3){attempt->
                    delay(if(attempt==0)600 else 1200)
                    if(!documentReady||Uri.parse(web.url.orEmpty()).host!="jwxt.scvtc.edu.cn")return@LaunchedEffect
                    val student=try{JwxtApi(headers={memory.apiHeaders(expected,it)}).student(expected)}
                        catch(e:JwxtAuthenticationRequired){null}
                    if(student!=null){
                        memory.confirmed(student.account,promoteEnrollment=true)
                        ScvtcNativeBridge.completeOfficialSignIn(app,student)
                        verifiedCallback();return@LaunchedEffect
                    }
                }
                message="尚未取得教务身份，请完成学校页面认证后点击确认"
            }catch(e:CancellationException){throw e}
            catch(_:Exception){message="学校身份读取暂未完成，当前认证保留，可重试"}
            finally{checking=false}
        }
        Column(modifier){
            Text(message,Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp).semantics{liveRegion=LiveRegionMode.Polite},
                style=MaterialTheme.typography.bodySmall)
            if(rendererGone)TextButton(onClick={browserEpoch++},modifier=Modifier.fillMaxWidth()){Text("重新打开学校页面")}
            else{
                if(documentReady&&!checking)TextButton(onClick={probeEpoch++},modifier=Modifier.fillMaxWidth()){Text("我已完成认证，确认身份")}
                AndroidView(factory={web},modifier=Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
    override fun onDestroy(){ScvtcNativeBridge.interactiveLogin=false;super.onDestroy()}
}
