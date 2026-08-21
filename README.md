# CC - Companion Chat (Native Android)

原生Android版本，使用Kotlin和Jetpack Compose构建。

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **架构**: MVVM + Clean Architecture
- **DI**: Hilt
- **数据库**: Room
- **网络**: Retrofit
- **异步**: Coroutines + Flow

## 项目结构

```
app/
├── data/           # 数据层
│   ├── local/      # 本地数据 (Room)
│   └── remote/     # 远程数据 (Retrofit)
├── domain/         # 领域层
├── ui/             # UI层
│   ├── theme/      # 主题系统
│   ├── navigation/ # 导航
│   ├── splash/     # 启动页
│   ├── home/       # 首页
│   └── chat/       # 聊天页
└── di/             # 依赖注入
```

## 构建

```bash
./gradlew assembleDebug
```

## 安装

```bash
./gradlew installDebug
```

## 开发进度

- [x] 项目初始化
- [x] 基础架构搭建
- [x] 主题系统
- [x] 导航系统
- [x] 数据库层
- [x] 依赖注入
- [ ] UI实现
- [ ] 功能开发
- [ ] 测试
- [ ] 发布

## 依赖版本

- Kotlin: 1.9.20
- Compose BOM: 2023.10.01
- Material 3: 1.1.2
- Hilt: 2.48
- Room: 2.6.1
- Retrofit: 2.9.0
- Navigation: 2.7.5

## 要求

- Android Studio Hedgehog (2023.1.1) 或更高
- JDK 17
- Android SDK 34
- Gradle 8.2

## 许可证

Private Project
