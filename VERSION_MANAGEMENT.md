# CC-Switch Android 版本管理

## 版本号规则

### 格式
```
versionCode: 整数，每次发布递增
versionName: X.Y.Z[-beta.N]
```

### 说明
- **X (Major)**: 主要版本，重大功能更新
- **Y (Minor)**: 次要版本，新功能添加
- **Z (Patch)**: 补丁版本，Bug 修复
- **beta.N**: 测试版本号（可选）

---

## 版本历史

### v2.1.0-beta.1 (versionCode: 2) - 2026-08-14

**新功能**:
- ✅ 完整数据库迁移策略（版本 1→7）
- ✅ 标签系统（创建、管理、为消息添加标签）
- ✅ 收藏夹独立页面
- ✅ 全局动态字体大小支持

**技术改进**:
- 移除 `fallbackToDestructiveMigration()`，保护用户数据
- 完整的标签 UI 集成到聊天界面
- 收藏夹加入导航系统
- 字体大小全局响应

**已知问题**:
- 通知系统未实现
- 高级搜索后端未完成

---

### v2.0.0 (versionCode: 1) - 之前版本

**基础功能**:
- 聊天界面
- 记忆树
- 数据统计
- 设置页面
- 收藏功能（数据库层）
- 图片支持

---

## 发布流程

### 测试版本发布

1. **更新版本号**
   ```kotlin
   // app/build.gradle.kts
   versionCode = X  // 递增
   versionName = "X.Y.Z-beta.N"
   ```

2. **编译安装**
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

3. **测试验证**
   - 检查新功能
   - 验证数据库迁移
   - 测试覆盖安装

4. **记录版本**
   - 更新本文档
   - 记录新功能和已知问题

### 正式版本发布

1. **更新版本号**（去掉 beta 标记）
   ```kotlin
   versionCode = X
   versionName = "X.Y.Z"
   ```

2. **编译 Release 版本**
   ```bash
   ./gradlew assembleRelease
   ```

3. **签名和发布**
   - 使用发布密钥签名
   - 上传到应用商店

---

## 覆盖安装说明

### Android 覆盖安装规则

只有满足以下条件才能覆盖安装：
1. ✅ **applicationId 相同**（已设置：`com.companion.cc`）
2. ✅ **versionCode 递增**（从 1 → 2 → 3...）
3. ✅ **相同的签名密钥**（Debug 版本自动使用 debug.keystore）

### 如何覆盖安装

**方式 1: Gradle 命令**
```bash
cd D:/CC-Switch/cc-native-android
./gradlew installDebug
```
会自动覆盖安装（如果版本号更高）

**方式 2: ADB 命令**
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
`-r` 参数表示覆盖安装

**方式 3: 模拟器直接安装**
- 拖拽 APK 文件到模拟器
- 会提示"更新应用"

---

## 下次更新步骤

### 准备发布 v2.1.0-beta.2

1. **完成新功能**（例如通知系统）

2. **更新版本号**
   ```kotlin
   versionCode = 3
   versionName = "2.1.0-beta.2"
   ```

3. **编译安装**
   ```bash
   ./gradlew installDebug
   ```

4. **验证更新**
   - 检查版本号显示
   - 验证数据保留（收藏、标签不丢失）
   - 测试新功能

5. **更新文档**
   - 记录本文档的版本历史
   - 更新 CHANGELOG.md

---

## 版本号快速参考

| 更新类型 | versionCode | versionName 示例 |
|---------|-------------|-----------------|
| 主要更新 | +1 | 2.0.0 → 3.0.0 |
| 新功能 | +1 | 2.0.0 → 2.1.0 |
| Bug 修复 | +1 | 2.0.0 → 2.0.1 |
| 测试版 | +1 | 2.1.0-beta.1 |
| 测试更新 | +1 | beta.1 → beta.2 |

**重要**: versionCode 必须始终递增，不能回退！

---

## 当前版本

```
versionCode: 2
versionName: 2.1.0-beta.1
applicationId: com.companion.cc
```

**构建命令**:
```bash
# 编译 Debug 版本
./gradlew assembleDebug

# 安装到设备（覆盖安装）
./gradlew installDebug

# 卸载应用
adb uninstall com.companion.cc

# 清理后重新安装
./gradlew clean installDebug
```
