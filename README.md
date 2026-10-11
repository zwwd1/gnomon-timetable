# 晷

> 把日子排成课表，把空白留给生活。

晷（guǐ），取意日晷。它记下课程与作息，也把时间交还给你。

这是为四川职业技术学院重构的 Android 课表应用，基于 SleepDown-Schedule，保留原生 Miuix 界面、液态玻璃底栏和课表编辑体验。适合想打开就看课、课余少一点折腾的同学。

[下载 APK](https://github.com/zwwd1/scvtc-kejian/releases/latest) · [反馈问题](https://github.com/zwwd1/scvtc-kejian/issues) · [更新记录](release-notes/v1.2.6.md)

## 从一张课表开始

首次安装会邀请你连接川职教务。填写学号和学校密码后，身份核验通过便返回应用，课表和成绩随后同步；密码加密留在手机，登录过期时尝试自动恢复。等待中可以取消或打开官方页面，验证码和账号安全提示在学校页面完成。

课表保存在本机，没网也能查看。可以切换日视图和周视图、调整开学日期与节次时间、添加课程、设置提醒和桌面组件。同步学校变更时保留本地编辑。成绩先读缓存，首次取得成绩后立即展示，毕业学分要求随后更新。

液态玻璃可开关，关闭后统一使用高斯模糊。内置背景采用浅、深两张留白壁纸，也能换成自己的照片。启动入场动画和底栏入口可以自行调整。AI 助手需要配置自己的模型服务，课程授权默认关闭。

[使用方法](docs/SCVTC_USAGE.md)

## 代码里的来路

`cn/scvtc/campus` 连接学校 CAS 与真实教务接口；`com/xiaomanjun/sleepdownschedule` 承载课表界面、编辑和提醒；`school-core` 处理课程、学分与导入规则。认证页面只负责建立会话，学生身份接口确认账号后才保存连接。

课表写入 Room，成绩与学分单独缓存。密码、会话和成绩缓存通过 Android Keystore 与 AES-GCM 按账号加密，普通导出不含登录凭据。云服务与匿名统计暂停，数据默认留在本机。

[构建原理](docs/SCVTC_ARCHITECTURE.md) · [隐私说明](docs/SCVTC_PRIVACY.md)

## 自行构建

使用 JDK 21、Android SDK 37.0 与仓库中的 Gradle Wrapper：

```powershell
.\gradlew.bat :app:assembleGithubRelease
```

正式包沿用第一版包名和签名，可以保留数据覆盖安装。自行签名的 APK 无法覆盖正式包；签名配置需放在仓库外，具体方式见构建说明。

[GitHub Release](https://github.com/zwwd1/scvtc-kejian/releases) 提供安装包、源码标签与 SHA-256。[验证记录](VERIFICATION.md) 区分构建检查与实机结果。

## 致谢项目

[SleepDown-Schedule](https://github.com/xiaomanjun233/SleepDown-Schedule)、Miuix 与 AndroidLiquidGlass。开源让这些日常的小事有了继续生长的余地；项目与第三方组件的许可见 [LICENSE.md](LICENSE.md) 和 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
