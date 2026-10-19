<div align="center">
<img src="app/src/main/res/drawable-nodpi/campus_icon_portrait.webp" width="88" alt="Gnomon 图标">
<h1>Gnomon 课程表</h1>
<p>今天去哪间教室，一眼就能找到。</p>

[![下载](https://img.shields.io/github/v/release/zwwd1/gnomon-timetable?label=下载最新版&color=2788ed)](https://github.com/zwwd1/gnomon-timetable/releases/latest)
![Android](https://img.shields.io/badge/Android-13%2B-34a853?logo=android&logoColor=white)
[![许可](https://img.shields.io/badge/许可-署名非商业-8b78c4)](LICENSE.md)

**[下载 APK](#download) · [界面预览](#screenshots) · [功能总览](#features) · [快速上手](#start) · [常见问题](#faq) · [文档索引](#docs)**
</div>

只是想查一节课，智慧川职却常让我来回找入口，用久了挺烦的。于是按自己的习惯做了 Gnomon：课表、教室、老师、作息和提醒，放在顺手的位置。这是自己随心而动的作品，希望能帮到川职院的同辈。

> 本项目基于 [SleepDown课程表](https://github.com/xiaomanjun233/SleepDown-Schedule) 修改；原作者：**xiaomanjun233**。这是 zwwd1 维护的非官方修改版，加入川职认证与同步、成绩查询、数据保护及本机文件管理，与学校和原作者均无官方关系。

<details>
<summary><strong>目录</strong></summary>

- [下载与安装](#download)
- [界面预览](#screenshots)
- [功能总览](#features)
- [快速上手](#start)
- [外观与日常设置](#appearance)
- [Gnomon 与 Lumen 怎么选](#compare)
- [数据与隐私](#privacy)
- [实现与构建](#build)
- [维护与验证](#maintenance)
- [常见问题](#faq)
- [文档索引](#docs) · [来源与许可](#credits)

</details>

---

<a id="download"></a>

## 下载与安装

| 当前版本 | 安装包 | 适用设备 |
| :--- | :--- | :--- |
| **[1.2.17](https://github.com/zwwd1/gnomon-timetable/releases/tag/v1.2.17)** | [下载通用 APK](https://github.com/zwwd1/gnomon-timetable/releases/download/v1.2.17/Gnomon-Timetable-1.2.17.apk) · 14.39 MiB | Android 13 及以上，通用包 |

[SHA-256 校验](https://github.com/zwwd1/gnomon-timetable/releases/download/v1.2.17/Gnomon-Timetable-1.2.17.apk.sha256) · [构建来源](https://github.com/zwwd1/gnomon-timetable/releases/download/v1.2.17/BUILD_PROVENANCE.json) · [对应源码](https://github.com/zwwd1/gnomon-timetable/tree/828f4a988931f08b54438957eab969062fe347b6) · [历史版本](https://github.com/zwwd1/gnomon-timetable/releases)

正式包沿用原包名与签名，可保留数据覆盖安装。自行构建并换用其他签名的包不能直接覆盖正式包；请先保留自己的课程和备份。

---

<a id="screenshots"></a>

## 界面预览

<table><tr>
<td align="center"><img src="docs/images/20261017/timetable.png" width="240" alt="Gnomon 实机周课表"><br><b>一周安排</b><br>点课程放大，长按再编辑。</td>
<td align="center"><img src="docs/images/20261017/profile.png" width="240" alt="Gnomon 实机个人中心"><br><b>常用入口</b><br>偏好、更新、关于、导入和导出。</td>
<td align="center"><img src="docs/images/20261017/motion.png" width="240" alt="Gnomon 实机材质与动效设置"><br><b>效果自己选</b><br>完整材质、低负载和入场动画。</td>
</tr></table>

配图拍摄于 **2026-10-17，Redmi K40 / Android 13，Gnomon 1.2.14**。画面来自实际应用，操作说明见[图文上手](docs/SCVTC_USAGE.md)。

---

<a id="features"></a>

## 功能总览

| 课程安排 | 教务与离线数据 | 外观与工具 |
| :--- | :--- | :--- |
| 日 / 周课表、教师与教室 | 川职认证与课表同步 | 液态 / 高斯 / 柔光材质 |
| 点按查看、长按编辑 | 成绩、学期与学分查询 | 完整 / 均衡 / 低负载质量 |
| 多课表、作息与课程提醒 | 已保存课表离线查看 | 入场动画与系统动画状态 |
| 桌面组件与今日课程 | 按账号、学期保存记录 | JSON / ICS、本机备份及手动 WebDAV |

课程详情、编辑、作息和提醒围绕原生课表展开；同步失败时保留上次成功保存的课程。具体入口见[图文上手](docs/SCVTC_USAGE.md)和[功能范围](docs/GNOMON_1.2.14.md)。

---

<a id="start"></a>

## 快速上手

| 步骤 | 在哪里点 | 完成后能看到什么 |
| --- | --- | --- |
| ① 连接学校 | 首次邀请 → 连接川职教务 → 输入学号、学校密码 | 核验后保存课表；密码按账号加密留在本机 |
| ② 看课程 | 底栏 **课表** → 日 / 周 → 点课程 | 放大查看课名、教师和地点；长按才编辑 |
| ③ 校对时间 | 个人中心 → 课表详细设置 | 核对开学日期和节次时间，再启用提醒 |
| ④ 查成绩 | 底栏 **成绩查询** | 先显示本机记录，可换学期、搜索和筛选 |
| ⑤ 留备份 | 个人中心 → 课表导出 / 课表备份与恢复 | 导出 JSON / ICS，或完整备份；导入先预览 |

学校要求验证码或安全确认时，进入应用里的**学校认证页**完成。普通会话到期会尝试恢复，学校强制的人工验证仍需本人操作。不要反复提交密码，也不用卸载清数据。[连接与故障处理](docs/SCVTC_USAGE.md)

---

<a id="appearance"></a>

## 外观与日常设置

- **默认三栏**：成绩查询 / 课表 / 个人中心。个人中心 → 应用偏好设置可增减入口。
- **三种材质**：液态、高斯、柔光分别保存参数；预览后点应用，取消不会写入。恢复默认液态会回到原组件配方。
- **完整效果由你选择**：材质质量可选完整、均衡、低负载；启动期间是否暂降材质质量另有开关。入场动画也能单独关闭。
- **看不到动画时**：系统“动画时长缩放”设为 0 时，应用会遵循。偏好里提供系统状态与设置入口。
- **文件留在手上**：JSON / ICS 导入导出、本机备份历史、手动 WebDAV；学校密码不进入普通导出。

---

<a id="compare"></a>

## Gnomon 与 Lumen 怎么选

| 你更常做什么 | 本仓库：Gnomon | [Lumen](https://github.com/zwwd1/lumen-timetable) |
| :--- | :--- | :--- |
| 安排上课日常 | 课程编辑、作息、提醒和桌面组件 | 也保留编辑、提醒和组件 |
| 集中查看教务 | 课表与常用成绩查询 | 课表、成绩、考试、学分和校园服务 |
| 使用已有记录 | 课表离线可看 | 查询结果本机缓存 |

选一款顺手的即可，也可以同时安装。两款使用各自的数据空间，不会自动共享账号和课表。

---

<a id="privacy"></a>

## 数据与隐私

学校认证与查询直接访问学校服务。密码和可恢复会话使用 Android Keystore 管理的密钥加密保存在本机，普通课程导出不携带学校凭据。

课程与作息保存在应用私有 Room 数据库，由应用沙箱和 Android 设备存储加密保护。上游云同步与匿名统计暂停；AI、手动 WebDAV 需自行配置，主动调用的服务仍可能联网。完整保护范围见[隐私说明](docs/SCVTC_PRIVACY.md)。

---

<a id="build"></a>

## 实现与构建

Kotlin / Compose 承载界面、编辑与提醒，Room 保存课程和配置。川职认证和查询位于 `cn/scvtc/campus`，课程校验规则在 `school-core`。会话优先复用，需要时尝试重新认证；读取失败保留上次课表，写入前分别校验账号与学期。

![从学校认证、校验到本机保存与页面展示的数据流程](docs/images/project-data-flow.svg)

### 自行构建

准备 **JDK 21、Android SDK 37**，配置 `JAVA_HOME` 与 `ANDROID_HOME`，使用仓库的 Gradle Wrapper：

```powershell
git clone https://github.com/zwwd1/gnomon-timetable.git
cd gnomon-timetable
.\gradlew.bat :app:assembleGithubRelease
```

构建沿用仓库的 Gradle Wrapper，以及项目附带的 Miuix / Backdrop 补丁来源。发布签名配置放在仓库外；完整工具链和签名变量见[构建与实现](docs/SCVTC_ARCHITECTURE.md)。

复现当前正式包时使用版本标签 `v1.2.17`，也可直接查看[发行对应源码](https://github.com/zwwd1/gnomon-timetable/tree/828f4a988931f08b54438957eab969062fe347b6)。自行签名不等于原签名覆盖升级。

---

<a id="maintenance"></a>

## 维护与验证

不会实时维护，更新要看自己的时间和精力。学校哪天换接口或认证方式，它可能就失效了，也不保证马上有人修。源码和构建方法都留着；需要的话 fork 一份，**自行 vibe coding**，按自己的习惯继续改。

### 已记录的验证范围

Gnomon 1.2.17 的原签名构建、发布来源和 **Redmi K40 / Android 13** 覆盖升级已有记录，保留既有数据完成了部分核心页面检查。新装、所有 ROM、Doze / 重启后的长时后台、通知与组件持续更新等仍有未覆盖项，具体以 [VERIFICATION.md](VERIFICATION.md) 和 [BUILD_PROVENANCE.json](BUILD_PROVENANCE.json) 为准。

---

<a id="faq"></a>

## 常见问题

<details>
<summary><strong>已有账号和课表，升级需要卸载吗？</strong></summary>

无需先卸载。使用本项目原签名、满足版本要求的正式包覆盖安装；签名不一致时先核对来源。卸载或清数据会丢失本机密钥，不能靠复制加密文件恢复登录。

</details>

<details>
<summary><strong>为什么还会要求学校验证码或重新认证？</strong></summary>

应用会尝试恢复普通过期会话；学校要求验证码、安全确认或其他本人操作时，仍需在官方认证页完成。失败时保留离线记录，不要反复提交密码。

</details>

<details>
<summary><strong>为什么后台同步不是准点发生？</strong></summary>

Android、电量、网络和厂商后台限制会影响调度。已有课程可从本机读取，需要最新教务信息时手动刷新。设置与故障处理见[图文上手](docs/SCVTC_USAGE.md)。

</details>

---

<a id="docs"></a>

## 文档索引

| 想了解什么 | 阅读入口 |
| :--- | :--- |
| 登录、课程操作和故障处理 | [图文上手](docs/SCVTC_USAGE.md) |
| 作息、材质、文件和入口范围 | [1.2.14 功能说明](docs/GNOMON_1.2.14.md) · [作息设置](docs/PERIOD_SCHEMES_GUIDE.md) |
| 本版改动与历史版本 | [1.2.17 更新说明](release-notes/v1.2.17.md) · [全部发行记录](https://github.com/zwwd1/gnomon-timetable/releases) |
| 认证、数据流程与构建 | [构建原理](docs/SCVTC_ARCHITECTURE.md) · [开发文档目录](docs/README.md) |
| 数据保存与实际验证 | [隐私说明](docs/SCVTC_PRIVACY.md) · [验证记录](VERIFICATION.md) |
| 项目来源与许可 | [原项目介绍](docs/UPSTREAM_README.md) · [许可证](LICENSE.md) · [第三方说明](THIRD_PARTY_NOTICES.md) |

---

<a id="credits"></a>

## 来源与许可

本项目基于 [SleepDown课程表](https://github.com/xiaomanjun233/SleepDown-Schedule) 修改，原作者 **xiaomanjun233**；川职适配由 **zwwd1** 维护。原许可、版权和第三方说明完整保留：[LICENSE.md](LICENSE.md) · [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。界面组件感谢 Miuix 与 AndroidLiquidGlass。
