# 晷 1.2.6 验证记录

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
