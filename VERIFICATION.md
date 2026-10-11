# 晷 1.2.5 验证记录

本次处理首次连接提示、学校 SSO 回调和失效密钥恢复；璟同时调整教务同步页布局、赞赏入口和安装包分发。

| 项目 | 本次结果 |
| --- | --- |
| APK | `scvtc-kejian-1.2.5.apk`，14,838,652 字节 |
| 包名 / 版本码 | `cn.scvtc.campus.preview` / 18 |
| APK SHA-256 | `8b73fa2bbc1d5bbb8355a3adcc96a456bd72bbec2151c3cdee63a32b3a125e67` |
| 原签名证书 | `5cab58b4d47d14e66143be594d3a4eebb5624be7e8f6a15354126bdab47343b3` |
| 构建源码 | [4b4d65518117981569566bb73b2e21b38f76a78a](https://github.com/zwwd1/scvtc-kejian/commit/4b4d65518117981569566bb73b2e21b38f76a78a) |
| Release 编译、R8、资源压缩、lintVital | 通过 |
| APK CRC、签名、ZIP / ELF 对齐、源资产 | 通过 |
| 源码 ZIP | 1928 个文件与构建提交逐个 Git blob 校验一致 |
| 用户账号和密码明文 | 源码及解压 APK 条目扫描未检出 |

未连接手机，未运行应用、自动化功能测试或帧率测量。真实学校登录、验证码、会话到期恢复、清除数据后的首次提示与交互仍需使用者验收；编译通过不代表这些操作已经在设备上验证。未运行全量 Lint，正式打包要求的 lintVital 已完成。

原包名、原签名与本机数据格式沿用。失效密钥对应的旧密文保留，不能宣称重新登录会解密已失效的记录。已保存的普通课表继续保留，登录材料不进入导出、公开源码或 AI 请求。

[本次更新](release-notes/v1.2.5.md) · [使用方法](docs/SCVTC_USAGE.md) · [隐私与加密](docs/SCVTC_PRIVACY.md)

正式发布收据：[GitHub Release](https://github.com/zwwd1/scvtc-kejian/releases/tag/v1.2.5)。服务器返回的每个 APK 大小与 SHA-256 均与本机原签名产物一致，源码标签指向构建提交，发布任务通过。应用内更新清单只使用这些已公开文件。
