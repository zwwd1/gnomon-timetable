# Gnomon 课程表 1.2.9 发布验证 · 2026-10-13

1.2.9 已发布到 [GitHub Release](https://github.com/zwwd1/gnomon-timetable/releases/tag/v1.2.9)。每个项目仅提供一份新版日常 APK，可沿用原包名和原签名覆盖安装。手机仍为 1.2.8；新包实机复测待连接，不把先前结果当作新包验收。

| 项目 | 本轮结果 |
| --- | --- |
| APK | Gnomon-Timetable-1.2.9.apk，14,889,416 字节，通用包 |
| 包名 / code | cn.scvtc.campus.preview / 22 |
| SHA-256 | 2a407a578a3ec707cd6300b02499a6398329ea28b9be33d3678e6e82c70c2eaa |
| 对应源码 | 9f09afbbff5c4fb3cf41cfadf1d705c7c9ace264，v1.2.9 标签已核对 |
| 编译输入指纹 | 99183e53acb9af705bb5b8da03a6cf9c0964a4679f68c8c01baf09fe18147ba7，Windows 与 Git 的纯行尾差别已另行核对，内容差异为零 |
| 构建与安装包检查 | Release、R8、资源压缩、lintVital、原证书、CRC、ZIP / ELF 对齐、资产、mapping、凭据明文检查通过 |
| 原生库 | 四个 ABI 的四个库保留 |
| 定向回归 | 后台核心 5 项通过 |
| 公开文件 | GitHub 返回的 APK 大小和摘要与最终本机文件一致 |
| 发布门槛 | 原签名发布流程通过 |
| 新包实机 | 首次指南、流畅度、自然会话到期、真正后台提交与覆盖安装尚待复测 |

此前 1.2.8 在 Android 17 的受控慢 CAS 完成回调条件下，原生身份核验 6,890 ms。这不是 1.2.9 实机结果，不能据此承诺学校认证永久有效或系统后台始终准时运行。

本机交付目录 D:/APK/1.2.9-20261013 只放两个 APK。私人账号、签名材料、设备诊断与临时测试组件不进入仓库或发布文件。

# 历史本地交付：晷 1.2.7 · 2026-10-12

修正移动认证路由与可选提醒处理，最终 APK 已在 Android 13 真机覆盖安装。追加修复按用户要求留在本地，没有再次提交、推送或发布；公开提交仅作为构建基线，准确工作树由输入指纹与本地补丁记录。

| 项目 | 最终结果 |
| --- | --- |
| 交付 APK | `晷-课表作息-1.2.7.apk`，14,889,404 字节，约 14.2 MiB |
| 包名 / 版本码 | `cn.scvtc.campus.preview` / 20 |
| APK SHA-256 | `517791d7b0b4ffdcb431a0caf0aa0dcc154077489c83d5aa2d521d6fcd07e356` |
| 原签名证书 | `5cab58b4d47d14e66143be594d3a4eebb5624be7e8f6a15354126bdab47343b3` |
| 本地源码基线 | `3b4c28bdb2c2ce7fa59f347c3972dab6aded6d7e`，包含其后的本地修复 |
| 实际编译输入指纹 | `ec07877ef05bddcc14a9e575d14fb2c5a89eead5c443a4110be3b0712b8a7c9b`，1670 个文件 |
| Release 编译 / R8 / 资源压缩 / lintVital | 通过 |
| 原签名 / CRC / ZIP、ELF 对齐 / 资产与 mapping | 通过 |
| 原生库与 1.2.6 比较 | 四个 ABI 的四个库逐字节相同 |
| 账号、密码明文扫描 | 未检出；原赞赏码 PNG 保持一致 |
| 定向 JVM 测试 | `RetainedSessionPolicyTest` 7 项、`SwuAuthRoutesTest` 5 项，失败与错误均为 0 |
| 认证脚本与官网 Vue 回归 | 会话失效、可选提醒关闭和连续提醒通过，强制验证仍被阻止自动操作 |
| Android 13 真机 | 原生自动登录、关闭进程后免输入重开、认证菜单与成绩页通过；本轮应用 UID 的 crash 日志为空 |
| Android 17 / API 37 真机 | 1.2.6 学校网页菜单复现 WebView/Vulkan 原生崩溃；1.2.7 同一网页菜单三次开关、原生自动登录、课表与成绩、进程重开和密码保留通过，检查区间无新增崩溃 |

没有清除用户手机数据；首次无数据邀请、自然会话到期、后台长时间冻结、全套菜单与帧率尚未完整实测。没有运行全量 Lint。包名、签名和已有数据格式沿用，认证失败保留缓存；学校强制认证仍需本人完成。

本地交付目录：`D:\APK\1.2.7-20261012`，只有晷、璟两个 APK。[定位和实机结果](docs/LOGIN_NAVIGATION_20261012.md) · [本轮更新](release-notes/v1.2.7.md)。详细指纹、构建日志和私有真机证据在工作区 `output/verification`，不随安装包上传。

# 历史记录：晷 1.2.6

本次处理自动认证长时间等待和旧版官方认证入口。身份核验不再等待学期接口，登录页支持取消和官方认证交接，排队的首次同步也纳入取消管理。

| 项目 | 本次结果 |
| --- | --- |
| APK | `scvtc-kejian-1.2.6.apk`，14,855,380 字节 |
| 包名 / 版本码 | `cn.scvtc.campus.preview` / 19 |
| APK SHA-256 | `5c133f8afdd9950b4e9a32a3a6dcc47098a57c2ee2d036cdef7c753e63c3bec6` |
| 原签名证书 | `5cab58b4d47d14e66143be594d3a4eebb5624be7e8f6a15354126bdab47343b3` |
| 构建源码 | [54911cf02bdd338be801ea03caff78ff7f835d22](https://github.com/zwwd1/scvtc-kejian/commit/54911cf02bdd338be801ea03caff78ff7f835d22) |
| Release 编译、R8、资源压缩、lintVital | 通过 |
| APK CRC、签名、ZIP / ELF 对齐、源资产 | 通过 |
| 源码 ZIP | 1931 个文件与构建提交逐个 Git blob 校验一致 |
| 用户账号和密码明文 | 源码及解压 APK 条目扫描未检出 |

未连接手机，未运行应用、自动化功能测试或帧率测量。真实学校登录、验证码、会话到期恢复、清除数据后的首次提示与交互仍需使用者验收；编译通过不代表这些操作已经在设备上验证。未运行全量 Lint，正式打包要求的 lintVital 已完成。

原包名、原签名与本机数据格式沿用。失效密钥对应的旧密文保留，不能宣称重新登录会解密已失效的记录。已保存的普通课表继续保留，登录材料不进入导出、公开源码或 AI 请求。

[问题定位与修正](docs/LOGIN_REPAIR_20261011.md) · [本次更新](release-notes/v1.2.6.md) · [使用方法](docs/SCVTC_USAGE.md) · [隐私与加密](docs/SCVTC_PRIVACY.md)

正式发布收据：[GitHub Release](https://github.com/zwwd1/scvtc-kejian/releases/tag/v1.2.6)。服务器返回的每个 APK 大小与 SHA-256 均与本机原签名产物一致，源码标签指向构建提交，发布任务通过。应用内更新清单只使用这些已公开文件。
