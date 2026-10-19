# Gnomon 文档

[项目首页](../README.md) · [下载安装](../README.md#download) · [反馈问题](https://github.com/zwwd1/gnomon-timetable/issues)

## 使用应用

| 内容 | 文档 |
| --- | --- |
| 登录、课表、成绩、外观和常见问题 | [使用说明](SCVTC_USAGE.md) |
| 密码、课程和缓存保存在哪里 | [隐私说明](SCVTC_PRIVACY.md) |
| 当前版本改了什么 | [1.2.17 更新说明](../release-notes/v1.2.17.md) · [历史发行](https://github.com/zwwd1/gnomon-timetable/releases) |
| 已测试的设备和场景 | [验证记录](../VERIFICATION.md) · [构建来源](../BUILD_PROVENANCE.json) |

## 开发与构建

[构建与数据流程](SCVTC_ARCHITECTURE.md)介绍认证、同步、缓存和构建要求。开始修改前阅读 [AGENTS.md](../AGENTS.md)。

| 任务 | 参考文档 |
| --- | --- |
| 目录、依赖与结构调整 | [PROJECT_STRUCTURE.md](architecture/PROJECT_STRUCTURE.md) |
| 页面、弹窗、选择器与公共控件 | [SLEEPDOWN_DESIGN_SYSTEM.md](architecture/SLEEPDOWN_DESIGN_SYSTEM.md) |
| 玻璃采样、渲染和生命周期 | [LIQUID_GLASS_FRAMEWORK.md](performance/LIQUID_GLASS_FRAMEWORK.md) |
| 跨 Activity 转场 | [TRANSITION_FRAMEWORK.md](TRANSITION_FRAMEWORK.md) |
| AI 文件导入 | [AI_FILE_IMPORT.md](ai/AI_FILE_IMPORT.md) |
| AI助理 | [DAY_AGENT_RUNTIME.md](architecture/DAY_AGENT_RUNTIME.md) |
| 备份与升级 | [BACKUP_FORMAT_V1.md](migration/BACKUP_FORMAT_V1.md)、[1_2_0_PACKAGE_MIGRATION.md](migration/1_2_0_PACKAGE_MIGRATION.md) |
| 节次设置 | [PERIOD_SCHEMES_GUIDE.md](PERIOD_SCHEMES_GUIDE.md) |
| 普通版中的实验功能、版本和发布 | [EXP_BRANCH_AND_RELEASES.md](EXP_BRANCH_AND_RELEASES.md) |

## 项目来源

[原项目介绍](UPSTREAM_README.md) · [许可证](../LICENSE.md) · [第三方说明](../THIRD_PARTY_NOTICES.md)

## 历史记录

带日期的排查、性能报告和版本说明记录当时的实现与测试结果。了解某次改动可以查看这些文件；判断当前功能时，请结合当前代码和最新版本说明。文档截图来自较早版本，拍摄时间、机型和版本在使用说明中标注。
