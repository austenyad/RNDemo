# React Native 模块化集成指南（rnlib）

## 概述

本项目将 React Native 以 **Brownfield（棕地）** 方式集成到现有 Android 原生项目中，并将 RN 运行时封装为独立的 `rnlib` Android Library 模块。宿主 App 只需两行代码即可初始化 RN 并跳转到任意 RN 页面。

## 技术栈

| 组件 | 版本 |
|------|------|
| React Native | 0.84.0 |
| React | 19.2.3 |
| Hermes | 随 RN 0.84.0 |
| AGP | 8.7.3 |
| Gradle | 8.13 |
| Kotlin | 2.0.21 |
| Java | 17 |
| New Architecture | 启用 |
| Fabric | 启用 |

## 项目结构

```
G2/
├── app/                          # 宿主 App（Compose + RN）
│   ├── build.gradle.kts          # 包含 com.facebook.react 插件
│   └── src/main/java/.../
│       ├── MainApplication.kt    # 实现 ReactApplication，调用 RNManager.init()
│       └── MainActivity.kt       # 原生 Compose 页面，按钮跳转 RN
├── rnlib/                        # RN 封装库模块
│   ├── build.gradle.kts          # Android Library，依赖 react-android/hermes-android
│   └── src/main/
│       ├── assets/
│       │   └── index.android.bundle  # 预打包的 JS Bundle
│       ├── java/.../
│       │   ├── RNManager.kt         # 核心管理器（初始化 + 页面跳转）
│       │   └── RNContainerActivity.kt # 通用 RN 容器 Activity
│       └── AndroidManifest.xml
├── rn/                           # React Native 源码目录
│   ├── package.json
│   ├── index.js                  # RN 入口，注册组件 "G2RN"
│   ├── App.tsx                   # RN 页面
│   └── node_modules/
├── settings.gradle.kts           # 包含 RNGP settings 插件配置
├── gradle.properties             # RN 相关属性（newArchEnabled 等）
└── gradle/libs.versions.toml     # 版本目录
```

## 做了什么

### 1. 创建 RN 源码目录（`rn/`）

- 初始化了一个精简的 React Native 0.84.0 项目（非完整模板）
- `index.js` 注册组件名 `G2RN`
- `App.tsx` 包含一个简单的演示页面
- `metro.config.js` 配置 Metro bundler

### 2. 创建 rnlib 模块

封装了两个核心类：

**RNManager** — 单例管理器
- `init(app, packages, devSupport)` — 初始化 RN 运行时（SoLoader、ReactNativeHost、NewArch）
- `startRNActivity(context, componentName, launchProps?)` — 跳转到指定 RN 页面
- `getReactNativeHost()` / `getReactHost()` — 供 `ReactApplication` 代理调用

**RNContainerActivity** — 通用容器
- 继承 `ReactActivity`，通过 Intent extra 接收组件名
- 使用 `pendingComponentName` 静态变量解决 ReactActivity 构造时序问题
- 一个 Activity 承载所有 RN 页面

### 3. 配置 Gradle 构建

- `com.facebook.react` 插件放在 **app 模块**（RNGP 要求必须在 application 模块）
- `react {}` 块配置 `root`、`reactNativeDir`、`codegenDir` 指向 `rn/` 目录
- `settings.gradle.kts` 配置 RNGP settings 插件和 autolink
- rnlib 通过 `api()` 暴露 `react-android` 和 `hermes-android` 依赖
- app 和 rnlib 统一使用 **Java 17**（react-android 强制要求，不一致会导致 desugaring 静默丢弃类）

### 4. 解决的关键问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| ClassNotFoundException: RNContainerActivity | app 和 rnlib 的 JVM target 不一致（11 vs 17），desugaring 静默丢弃了 rnlib 的类 | 统一 JVM target 为 17 |
| ReactActivity 构造时 intent 为 null | ReactActivity 在 `onCreate` 之前就调用 `getMainComponentName()` | 使用 `pendingComponentName` 静态变量提前传递组件名 |
| React 版本不匹配 | `react: ^19.2.3` 解析到 19.2.4，与 RN 0.84.0 内置的 renderer 19.2.3 冲突 | 固定 `react: "19.2.3"`（去掉 `^`） |
| AGP 9.0 不兼容 RNGP | RN 0.84.0 的 Gradle 插件不支持 AGP 9.0 | 降级 AGP 到 8.7.3，Gradle 到 8.13 |

---

## 新项目接入指南

### 前置条件

- Node.js 和 npm 已安装
- Android Studio + JDK 17
- AGP 8.x（不支持 9.0）

### Step 1: 创建 RN 源码目录

在项目根目录创建 `rn/` 文件夹：

```bash
mkdir rn && cd rn
npm init -y
npm install react@19.2.3 react-native@0.84.0
npm install -D @react-native-community/cli @react-native-community/cli-platform-android @react-native/metro-config@0.84.0
```

创建 `rn/index.js`：
```javascript
import {AppRegistry} from 'react-native';
import App from './App';
AppRegistry.registerComponent('YourAppName', () => App);
```

创建 `rn/App.tsx`（你的 RN 页面）。

### Step 2: 复制 rnlib 模块

将 `rnlib/` 目录整体复制到新项目根目录，包含：
- `build.gradle.kts`
- `src/main/java/com/tuwan/rnlib/RNManager.kt`
- `src/main/java/com/tuwan/rnlib/RNContainerActivity.kt`
- `src/main/AndroidManifest.xml`
- `proguard-rules.pro` / `consumer-rules.pro`

### Step 3: 配置 settings.gradle.kts

```kotlin
pluginManagement {
    includeBuild("rn/node_modules/@react-native/gradle-plugin")
    // ... 其他 repositories
}

plugins {
    id("com.facebook.react.settings")
}

extensions.configure<com.facebook.react.ReactSettingsExtension> {
    autolinkLibrariesFromCommand(
        workingDirectory = file("rn"),
        lockFiles = files("rn/package-lock.json")
    )
}

// ...
include(":app")
include(":rnlib")
```

### Step 4: 配置 app/build.gradle.kts

```kotlin
plugins {
    // ... 其他插件
    id("com.facebook.react")
}

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

react {
    autolinkLibrariesWithApp()
    root = file("../rn")
    reactNativeDir = file("../rn/node_modules/react-native")
    codegenDir = file("../rn/node_modules/@react-native/codegen")
}

dependencies {
    implementation(project(":rnlib"))
}
```

### Step 5: 配置 gradle.properties

```properties
reactNativeArchitectures=arm64-v8a
newArchEnabled=true
hermesEnabled=true
```

### Step 6: 配置 Application

```kotlin
class MainApplication : Application(), ReactApplication {
    override fun onCreate() {
        super.onCreate()
        RNManager.init(this, PackageList(this).packages, BuildConfig.DEBUG)
    }

    @Suppress("DEPRECATION")
    override val reactNativeHost: ReactNativeHost
        get() = RNManager.getReactNativeHost()

    override val reactHost: ReactHost
        get() = RNManager.getReactHost()
}
```

### Step 7: 跳转 RN 页面

```kotlin
// 从任意 Activity/Fragment 跳转
RNManager.startRNActivity(context, "YourAppName")

// 带参数
RNManager.startRNActivity(context, "YourAppName", mapOf("userId" to "123"))
```

---

## 开发流程

### 日常开发（热重载模式）

1. 启动 Metro dev server：
   ```bash
   cd rn && npx react-native start
   ```

2. 确保 `RNManager.init()` 的 `devSupport` 参数为 `true`（`BuildConfig.DEBUG` 即可）

3. 在 Android Studio 运行 app，打开 RN 页面后修改 `rn/App.tsx`，保存即可热重载

> 注意：设备和电脑需在同一网络，或通过 USB 连接并执行 `adb reverse tcp:8081 tcp:8081`

### 打包发布

1. 打包 JS Bundle：
   ```bash
   cd rn
   npx react-native bundle \
     --platform android \
     --dev false \
     --entry-file index.js \
     --bundle-output ../rnlib/src/main/assets/index.android.bundle \
     --assets-dest ../rnlib/src/main/res/
   ```

2. 构建 APK：
   ```bash
   ./gradlew :app:assembleRelease
   ```

### 添加新的 RN 页面

1. 在 `rn/` 中创建新组件，如 `Settings.tsx`

2. 在 `rn/index.js` 中注册：
   ```javascript
   import Settings from './Settings';
   AppRegistry.registerComponent('Settings', () => Settings);
   ```

3. 原生端跳转：
   ```kotlin
   RNManager.startRNActivity(context, "Settings")
   ```

4. 重新打包 bundle 或使用 dev server 开发

### 添加 RN 第三方库

```bash
cd rn && npm install <package-name>
```

如果是带原生代码的库，需要重新构建：
```bash
./gradlew clean :app:assembleDebug
```

RNGP 的 autolink 会自动处理原生模块链接。

---

## 架构决策说明

### 为什么 `com.facebook.react` 插件在 app 而不是 rnlib？

RNGP（React Native Gradle Plugin）要求必须应用在 `com.android.application` 模块上，因为它需要：
- 执行 codegen（代码生成）
- 处理 autolink（自动链接原生模块）
- 管理 JS bundle 打包任务

rnlib 作为 library 模块无法承载这些功能。

### 为什么 PackageList 在 app 模块创建？

`PackageList` 是 RNGP autolink 在 app 模块自动生成的类，包含所有已链接的原生模块。rnlib 无法访问这个生成类，所以由 app 创建后传给 `RNManager.init()`。

### 为什么必须统一 Java 17？

`react-android` AAR 使用 Java 17 编译。如果 rnlib 使用 Java 11 而 app 使用 Java 17，Android 的 desugaring 过程会静默丢弃 rnlib 中引用了 Java 17 API 的类，导致运行时 `ClassNotFoundException`。

### pendingComponentName 的作用

`ReactActivity` 的父类在构造函数中就会调用 `getMainComponentName()`，此时 `intent` 还是 null。通过静态变量 `pendingComponentName` 在 `startActivity` 之前预设组件名，绕过这个时序问题。
