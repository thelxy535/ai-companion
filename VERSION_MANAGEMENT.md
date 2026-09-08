# 版本管理

## 当前版本

- `versionCode`: 21
- `versionName`: `8.0.0-v85`
- application id：`com.companion.cc`
- Debug application id：`com.companion.cc.debug`

## 版本规则

`versionCode` 每次可安装构建递增；`versionName` 使用 `X.Y.Z`，开发验证版本可追加 `-vN` 或 `-beta.N`。

- Major：架构或产品方向变化
- Minor：新增完整功能
- Patch：兼容性、稳定性或错误修复
- 后缀：尚未作为正式版本发布的开发构建

## 发布前检查

1. 更新 `app/build.gradle.kts` 中的版本号。
2. 检查 Room schema 和 migration 是否完整。
3. 运行 `testDebugUnitTest`、`lintDebug` 和 `assembleDebug`。
4. 在模拟器完成聊天、角色、记忆、导入导出和通知入口回归。
5. 真机只做系统行为最终确认。
6. 确认提交中没有 API Key、数据库、日志、截图和 APK。

具体变化记录在 [CHANGELOG.md](CHANGELOG.md)。
