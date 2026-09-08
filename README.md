# 弦曜 SYLORA

> **让每一次对话，都像真的和一个人相遇。**

弦曜 SYLORA 是一个由个人开发者持续打磨的原生 Android AI 伴侣。

我不想再做一个只会回答问题的聊天窗口，也不想把“陪伴”做成每天打卡、定时触发和模板化问候。弦曜更在意一段关系如何慢慢长出来：一个人会有自己的脾气、习惯、记忆、犹豫和生活，不会因为换了一句话就突然变成另一个人。

你可以和内置角色相遇，也可以亲手创造一个只属于你的角色。她不只是一个名字和头像，而是一个拥有背景、边界、记忆和变化空间的长期存在。

## ✦ 这个项目想做什么

弦曜相信，好的 AI 伴侣不应该让人感觉自己在操作一套功能，而应该让人感觉：

- 她记得你们一起走过的事情，但不会把所有旧话题机械地翻出来。
- 她有自己的性格，不会对任何人都用同一种温柔、同一种语气。
- 她知道什么时候该靠近，也知道什么时候不打扰是一种尊重。
- 她会延续正在发生的场景，不会上一句还穿着睡衣，下一句就毫无原因地准备出门。
- 她的改变来自相处，而不是来自一张不断堆叠规则的配置表。

这不是一个“完成任务就变强”的系统，而是一场关于长期关系、记忆和想象力的实验。

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/license-private-lightgrey)](#许可证)

## ✦ 和普通 AI 聊天有什么不同

| 能力 | 体验重点 |
| --- | --- |
| 机械回复 | **独立人格**：不同角色拥有不同的敏感度、表达欲、慢热程度、分享欲和关系边界 |
| 上下文拼接 | **关系记忆**：对话、长期记忆、情绪余波和共同经历共同影响后续交流 |
| 场景跳变 | **生活连续性**：地点、穿着、姿态和正在做的事会自然延续，除非真的发生了转场 |
| 定时刷屏 | **有分寸的主动**：角色会考虑时间、关系和打扰成本，不为了完成指标硬发消息 |
| 同质化角色 | **可塑造的个体**：从背景故事到说话节奏，从边界到生活线，都可以由你决定 |
| 黑箱云端 | **数据自主**：聊天和记忆默认留在设备上，模型服务由用户自行配置 |

## ♡ 适合谁

- 想认真创造一个有来处、有性格、有变化空间的角色的人。
- 不满足于“一问一答”，希望对话能积累关系和共同记忆的人。
- 喜欢写角色、做世界观、整理 Character Book，想把想象变成可以长期相处的人的人。
- 在意隐私，希望自己选择模型服务商、自己保管聊天数据的人。

## 功能分区

### 💬 对话与角色：先成为一个人

- 多角色聊天、角色切换和自定义角色创建、编辑、删除
- 角色卡导入导出，支持 JSON、Markdown、YAML 和纯文本资料
- Character Book、示例对话、背景故事、行为规则和深层人格配置
- 角色独立的语气、表达欲、分享倾向、主动性和关系边界
- 流式回复、重试、图片消息、语音输入和角色级语音配置
- 对白与动作描写分离，动作统一显示在对白下方的小字区域

### 🧠 记忆与关系：让相处留下痕迹

- Room 持久化聊天记录、记忆节点、关系、情绪和记忆来源
- 短期上下文、中期摘要、长期召回和永久人格四层记忆
- 对话反思、记忆审核、冲突处理、关系叙事自演化和记忆收件箱
- 记忆图谱、时间线、原始记录、收藏、搜索和导入导出
- 角色删除前自动保存记忆胶囊，支持后续恢复
- 历史消息不会因为聊天界面只展示一部分就被当作不存在

### 🌙 连续性与分寸：不要突然变成另一个人

- 时间感：能区分刚刚、今天、隔了一段时间和久未联系
- 场景感：没有明确转场时，不自动换衣服、换房间或突然出门
- 内在状态：保留未完成的想法、在意的事、期待、情绪余波和生活线
- 主动消息：按角色的性格和打扰成本决定是否联系，而不是固定刷屏
- 个体差异：同一件事会因角色性格不同而产生不同的语气和反应

### ✦ 视觉与体验：把认真藏进每个细节

- Jetpack Compose + Material 3 沉浸式界面
- 弦曜蓝色星环品牌图标，启动页、桌面图标和最近任务图标统一
- 自适应动效等级、触感反馈、沉浸式状态栏和响应式布局
- 新消息提示、进入聊天后即时呈现和稳定自动回底
- 记忆脑图中文显示兼容历史编码数据

## 技术架构

```text
app/src/main/java/com/companion/cc/
├── data/       Room、DataStore、网络 API、数据映射和仓储实现
├── domain/     角色、记忆、情绪、关系、消息和后台任务领域逻辑
├── ui/         Compose 页面、导航、主题和交互组件
├── di/         Hilt 依赖注入模块
└── util/       通知、日志、加密和通用工具
```

| 层面 | 技术 |
| --- | --- |
| 语言与界面 | Kotlin、Jetpack Compose、Material 3 |
| 架构 | MVVM、分层领域模块、StateFlow、Coroutines |
| 依赖注入 | Hilt |
| 本地数据 | Room、DataStore |
| 网络 | Retrofit、OkHttp、OpenAI 兼容 API |
| 后台任务 | WorkManager |
| 测试 | JUnit、Mockito、Robolectric、AndroidX Test、Compose Test |

## 开始开发

### 环境要求

- Android Studio Hedgehog 或更高版本
- JDK 17
- Android SDK 34
- Android SDK Platform Tools

### 构建与验证

Windows：

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat installDebug
```

macOS / Linux：

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew installDebug
```

Debug APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`

### 配置模型服务

首次启动后，在应用内填写服务商、Base URL、模型名称和 API Key。弦曜支持 OpenAI 兼容接口；API Key 只应保存在本地设置或安全的环境变量中，绝不要写进源码、截图、日志或 Git 提交。

## 数据与隐私

- 聊天记录、记忆、角色资料默认保存在设备本地。
- 模型请求只发送到用户自行配置的服务商。
- 项目不内置共享 API Key，也不要求把个人聊天数据上传到项目服务器。
- 数据库文件、设备令牌、API Key、测试抓包、日志和 APK 不应提交到 GitHub。
- 删除角色前会生成本地记忆胶囊，避免误删后无法恢复。

## 项目状态

弦曜仍然处于持续开发阶段。它不是一个已经包装完毕的商业产品，而是一件正在被认真打磨的作品。

当前重点是：让不同角色保持更稳定的个体差异，让长期记忆召回更自然，让主动消息更有分寸，并把本地数据、云备份和多设备同步做得可靠而克制。

本地最近一次验证已通过：

- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleDebug`
- 模拟器启动回归

## 文档导航

- [测试指南](TESTING_GUIDE.md)
- [版本管理](VERSION_MANAGEMENT.md)
- [变更记录](CHANGELOG.md)
- [视觉网关说明](server/vision-gateway/README.md)

## 许可证

Private Project。当前代码仅供个人开发和测试使用，未经许可不得分发或商用。
