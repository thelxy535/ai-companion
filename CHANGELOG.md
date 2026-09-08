# 版本更新日志

## 当前开发版本：8.0.0-v85

### ✦ 近期完成

- 弦曜 SYLORA 品牌名称、蓝色星环图标和启动视觉统一
- 自定义角色卡支持 JSON、Markdown、YAML 和纯文本导入导出
- 角色独立人格、语气、主动性、生活线和关系边界
- 场景连续性与动作描写分离，避免短时间内无理由换衣、换房间或转场
- 长期记忆、关系叙事、反思审核、记忆图谱和记忆胶囊
- 聊天上下文与界面展示窗口解耦，历史消息不会因显示窗口滚动而直接丢失
- API 配置支持 OpenAI 兼容 Base URL，并补充本地测试和错误处理

### ✅ 验证

- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleDebug`
- 模拟器启动回归

## v2.1.0-beta.2 (versionCode: 3) - 2026-08-14

### ✨ 新功能

**头像系统**
- ✅ 添加用户头像自定义功能
- ✅ 添加 AI 伴侣头像自定义功能
- ✅ 支持从相册选择图片作为头像
- ✅ 支持清除自定义头像，恢复默认
- ✅ 头像数据持久化保存

**名称更新**
- ✅ 统一"小灿"改名为"小璨"（19处）
- ✅ 所有界面、注释、文档已更新

### 🔧 技术实现

**数据层**:
- `SettingsManager.kt` 添加头像管理方法
  - `userAvatarFlow`: 用户头像 Flow
  - `saveUserAvatar()`: 保存用户头像
  - `getCompanionAvatarFlow()`: 获取 AI 头像
  - `saveCompanionAvatar()`: 保存 AI 头像

**UI 组件**:
- `AvatarSettings.kt` 新建
  - `AvatarSettingsDialog`: 头像设置对话框
  - `AvatarSettingItem`: 设置页面头像项
  - 集成图片选择器（PickVisualMedia）

**数据模型**:
- `Companion.kt` 添加 `avatarUrl` 字段
  - 支持自定义头像 URL
  - 默认使用 emoji 作为头像

### 📋 文件清单

**新建文件**:
1. `AvatarSettings.kt` - 头像设置 UI 组件

**修改文件**:
1. `SettingsManager.kt` - 添加头像管理方法
2. `Companion.kt` - 添加 avatarUrl 字段
3. 所有包含"小灿"的文件（批量替换为"小璨"）

### 🎯 使用方式

**用户头像**:
```kotlin
// 读取
val userAvatar by settingsManager.userAvatarFlow.collectAsState(initial = null)

// 保存
settingsManager.saveUserAvatar("file:///path/to/avatar.jpg")

// 清除
settingsManager.saveUserAvatar(null)
```

**AI 伴侣头像**:
```kotlin
// 读取
val companionAvatar by settingsManager.getCompanionAvatarFlow("xiaocan").collectAsState(initial = null)

// 保存
settingsManager.saveCompanionAvatar("xiaocan", "file:///path/to/avatar.jpg")

// 清除
settingsManager.saveCompanionAvatar("xiaocan", null)
```

**UI 组件使用**:
```kotlin
var showAvatarDialog by remember { mutableStateOf(false) }

AvatarSettingItem(
    title = "用户头像",
    avatarUrl = userAvatar,
    onClick = { showAvatarDialog = true }
)

if (showAvatarDialog) {
    AvatarSettingsDialog(
        currentAvatarUrl = userAvatar,
        title = "设置用户头像",
        onDismiss = { showAvatarDialog = false },
        onAvatarSelected = { uri ->
            // 保存头像 URI
            settingsManager.saveUserAvatar(uri.toString())
        },
        onClearAvatar = {
            settingsManager.saveUserAvatar(null)
        }
    )
}
```

### 🔄 覆盖安装验证

✅ 从 versionCode 2 升级到 3 成功
✅ 用户数据保留（收藏、标签等）
✅ 头像设置独立存储，不影响其他数据

---

## v2.1.0-beta.1 (versionCode: 2) - 2026-08-14

### ✨ 新功能
- ✅ 完整数据库迁移策略（版本 1→7）
- ✅ 标签系统（创建、管理、为消息添加标签）
- ✅ 收藏夹独立页面
- ✅ 全局动态字体大小支持

### 🔧 技术改进
- 移除 `fallbackToDestructiveMigration()`，保护用户数据
- 完整的标签 UI 集成到聊天界面
- 收藏夹加入导航系统
- 字体大小全局响应

---

## 待实现功能

### 头像系统完善（后续版本）
- [ ] 在设置页面添加头像设置入口
- [ ] 在聊天界面显示自定义头像
- [ ] 头像缓存优化
- [ ] 支持头像裁剪功能

### 其他增强功能
- [ ] 通知系统（WorkManager）
- [ ] 高级搜索后端（日期范围查询）

---

## 安装说明

**覆盖安装**（推荐）:
```bash
cd D:/CC-Switch/cc-native-android
./gradlew installDebug
```

**手动安装**:
APK 位置: `app/build/outputs/apk/debug/app-debug.apk`

**卸载重装**:
```bash
adb uninstall com.companion.cc
./gradlew installDebug
```

---

## 版本对比

| 版本 | versionCode | 主要更新 |
|-----|-------------|---------|
| 2.1.0-beta.2 | 3 | 头像系统 + 小璨改名 |
| 2.1.0-beta.1 | 2 | 标签、收藏夹、字体、迁移 |
| 2.0.0 | 1 | 基础功能 |

---

**当前已安装版本**: v2.1.0-beta.2 (versionCode: 3)

**测试要点**:
1. ✅ "小灿"已全部改为"小璨"
2. ✅ 头像 API 已就绪（SettingsManager）
3. ✅ 头像 UI 组件已创建（AvatarSettings.kt）
4. ⏳ 需要在设置页面集成头像设置入口
5. ⏳ 需要在聊天界面使用自定义头像
