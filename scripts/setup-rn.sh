#!/bin/bash
# React Native Brownfield 快速接入脚本
# 用法: 在 Android 项目根目录执行
#   curl -sL <your-repo>/scripts/setup-rn.sh | bash
# 或者:
#   bash setup-rn.sh

set -e

RN_VERSION="0.84.0"
REACT_VERSION="19.2.3"
RN_CLI_VERSION="20.1.1"
METRO_CONFIG_VERSION="0.84.0"

echo "🚀 开始配置 React Native Brownfield 集成..."

# ============================================================
# 1. 创建 rn/ 目录并安装依赖
# ============================================================
if [ ! -d "rn" ]; then
  echo "📦 创建 rn/ 目录并安装依赖..."
  mkdir -p rn
  cat > rn/package.json << 'PACKAGE_EOF'
{
  "name": "rn-module",
  "version": "0.0.1",
  "private": true,
  "scripts": {
    "start": "react-native start",
    "bundle:android": "react-native bundle --platform android --dev false --entry-file index.js --bundle-output ../rnlib/src/main/assets/index.android.bundle --assets-dest ../rnlib/src/main/res/"
  },
  "dependencies": {
    "react": "REACT_VERSION_PLACEHOLDER",
    "react-native": "RN_VERSION_PLACEHOLDER"
  },
  "devDependencies": {
    "@react-native-community/cli": "CLI_VERSION_PLACEHOLDER",
    "@react-native-community/cli-platform-android": "CLI_VERSION_PLACEHOLDER",
    "@react-native/metro-config": "METRO_VERSION_PLACEHOLDER",
    "@types/react": "^19.1.1",
    "typescript": "~5.7.0"
  },
  "main": "index.js"
}
PACKAGE_EOF

  # 替换版本占位符
  sed -i.bak "s/REACT_VERSION_PLACEHOLDER/$REACT_VERSION/g" rn/package.json
  sed -i.bak "s/RN_VERSION_PLACEHOLDER/$RN_VERSION/g" rn/package.json
  sed -i.bak "s/CLI_VERSION_PLACEHOLDER/$RN_CLI_VERSION/g" rn/package.json
  sed -i.bak "s/METRO_VERSION_PLACEHOLDER/$METRO_CONFIG_VERSION/g" rn/package.json
  rm -f rn/package.json.bak

  cat > rn/index.js << 'INDEX_EOF'
import {AppRegistry} from 'react-native';
import App from './App';
AppRegistry.registerComponent('RNApp', () => App);
INDEX_EOF

  cat > rn/App.tsx << 'APP_EOF'
import React from 'react';
import {SafeAreaView, Text, StyleSheet, View} from 'react-native';

export default function App() {
  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.content}>
        <Text style={styles.title}>🚀 React Native</Text>
        <Text style={styles.subtitle}>Brownfield 集成成功</Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1, backgroundColor: '#f5f5f5'},
  content: {flex: 1, justifyContent: 'center', alignItems: 'center'},
  title: {fontSize: 28, fontWeight: '700', color: '#333'},
  subtitle: {fontSize: 16, color: '#666', marginTop: 8},
});
APP_EOF

  cat > rn/metro.config.js << 'METRO_EOF'
const {getDefaultConfig, mergeConfig} = require('@react-native/metro-config');
const path = require('path');

const projectRoot = __dirname;
const monorepoRoot = path.resolve(projectRoot, '..');

const config = {
  watchFolders: [monorepoRoot],
  resolver: {
    nodeModulesPaths: [path.resolve(projectRoot, 'node_modules')],
  },
};

module.exports = mergeConfig(getDefaultConfig(projectRoot), config);
METRO_EOF

  cat > rn/react-native.config.js << 'RNCONFIG_EOF'
module.exports = {
  project: {
    android: {
      sourceDir: '../app',
      appName: 'app',
    },
  },
};
RNCONFIG_EOF

  (cd rn && npm install)
  echo "✅ rn/ 目录创建完成"
else
  echo "⏭️  rn/ 目录已存在，跳过"
fi

# ============================================================
# 2. 创建 rnlib 模块
# ============================================================
if [ ! -d "rnlib/src" ]; then
  echo "📦 创建 rnlib 模块..."
  mkdir -p rnlib/src/main/java/com/tuwan/rnlib
  mkdir -p rnlib/src/main/assets

  cat > rnlib/build.gradle.kts << 'GRADLE_EOF'
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.tuwan.rnlib"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    api("com.facebook.react:react-android")
    api("com.facebook.react:hermes-android")
}
GRADLE_EOF

  cat > rnlib/src/main/AndroidManifest.xml << 'MANIFEST_EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application>
        <activity
            android:name=".RNContainerActivity"
            android:exported="false"
            android:theme="@style/Theme.AppCompat.Light.NoActionBar" />
    </application>
</manifest>
MANIFEST_EOF

  touch rnlib/proguard-rules.pro
  touch rnlib/consumer-rules.pro

  # RNManager.kt
  cat > rnlib/src/main/java/com/tuwan/rnlib/RNManager.kt << 'RNMGR_EOF'
package com.tuwan.rnlib

import android.app.Application
import android.content.Context
import android.content.Intent
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint
import com.facebook.react.defaults.DefaultReactHost
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.react.soloader.OpenSourceMergedSoMapping
import com.facebook.soloader.SoLoader

object RNManager {

    private var application: Application? = null
    private var isInitialized = false

    @Suppress("DEPRECATION")
    private var _reactNativeHost: ReactNativeHost? = null

    @JvmStatic
    @JvmOverloads
    fun init(
        app: Application,
        packages: List<ReactPackage>,
        devSupport: Boolean = false
    ) {
        if (isInitialized) return
        application = app

        SoLoader.init(app, OpenSourceMergedSoMapping)

        @Suppress("DEPRECATION")
        _reactNativeHost = object : DefaultReactNativeHost(app) {
            override fun getPackages(): List<ReactPackage> = packages
            override fun getJSMainModuleName(): String = "index"
            override fun getUseDeveloperSupport(): Boolean = devSupport
            override val isNewArchEnabled: Boolean = true
            override val isHermesEnabled: Boolean = true
        }

        DefaultNewArchitectureEntryPoint.load()
        isInitialized = true
    }

    @JvmStatic
    @JvmOverloads
    fun startRNActivity(
        context: Context,
        componentName: String,
        launchProps: Map<String, String>? = null
    ) {
        check(isInitialized) { "RNManager.init() must be called before startRNActivity()" }
        RNContainerActivity.pendingComponentName = componentName
        val intent = Intent(context, RNContainerActivity::class.java).apply {
            putExtra(RNContainerActivity.EXTRA_COMPONENT_NAME, componentName)
            launchProps?.forEach { (k, v) -> putExtra(k, v) }
        }
        context.startActivity(intent)
    }

    @Suppress("DEPRECATION")
    fun getReactNativeHost(): ReactNativeHost {
        return _reactNativeHost
            ?: throw IllegalStateException("RNManager.init() has not been called")
    }

    fun getReactHost(): ReactHost {
        val app = application
            ?: throw IllegalStateException("RNManager.init() has not been called")
        @Suppress("DEPRECATION")
        return DefaultReactHost.getDefaultReactHost(app.applicationContext, getReactNativeHost())
    }
}
RNMGR_EOF

  # RNContainerActivity.kt
  cat > rnlib/src/main/java/com/tuwan/rnlib/RNContainerActivity.kt << 'CONTAINER_EOF'
package com.tuwan.rnlib

import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

class RNContainerActivity : ReactActivity() {

    companion object {
        const val EXTRA_COMPONENT_NAME = "rn_component_name"

        @Volatile
        internal var pendingComponentName: String? = null
    }

    override fun getMainComponentName(): String {
        return pendingComponentName
            ?: intent?.getStringExtra(EXTRA_COMPONENT_NAME)
            ?: "App"
    }

    override fun createReactActivityDelegate(): ReactActivityDelegate {
        return DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)
    }

    override fun onDestroy() {
        super.onDestroy()
        pendingComponentName = null
    }
}
CONTAINER_EOF

  echo "✅ rnlib 模块创建完成"
else
  echo "⏭️  rnlib 模块已存在，跳过"
fi

# ============================================================
# 3. 打包初始 JS Bundle
# ============================================================
if [ ! -f "rnlib/src/main/assets/index.android.bundle" ]; then
  echo "📦 打包 JS Bundle..."
  (cd rn && npx @react-native-community/cli bundle \
    --platform android \
    --dev false \
    --entry-file index.js \
    --bundle-output ../rnlib/src/main/assets/index.android.bundle)
  echo "✅ JS Bundle 打包完成"
else
  echo "⏭️  JS Bundle 已存在，跳过"
fi

# ============================================================
# 4. 输出后续手动配置提示
# ============================================================
cat << 'TIPS'

============================================================
🎉 自动配置完成！还需要手动完成以下步骤：
============================================================

1️⃣  settings.gradle.kts 添加：

   pluginManagement {
       includeBuild("rn/node_modules/@react-native/gradle-plugin")
       // ... 保留原有 repositories
   }

   plugins {
       id("com.facebook.react.settings")
   }

   // 自动检测 npx 路径（兼容 nvm/fnm）
   val npxPath: String = providers.exec {
       commandLine("bash", "-lc", "which npx")
   }.standardOutput.asText.get().trim()

   extensions.configure<com.facebook.react.ReactSettingsExtension> {
       autolinkLibrariesFromCommand(
           command = listOf(npxPath, "@react-native-community/cli", "config"),
           workingDirectory = file("rn"),
           lockFiles = files("rn/package-lock.json")
       )
   }

   include(":rnlib")

2️⃣  app/build.gradle.kts 添加：

   plugins {
       id("com.facebook.react")  // 加在 plugins 块末尾
   }

   android {
       kotlinOptions { jvmTarget = "17" }
       buildFeatures { buildConfig = true }
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

3️⃣  gradle.properties 添加：

   reactNativeArchitectures=arm64-v8a
   newArchEnabled=true
   hermesEnabled=true

4️⃣  Application 类实现 ReactApplication：

   class MainApplication : Application(), ReactApplication {
       override fun onCreate() {
           super.onCreate()
           RNManager.init(this, PackageList(this).packages, BuildConfig.DEBUG)
       }
       override val reactNativeHost get() = RNManager.getReactNativeHost()
       override val reactHost get() = RNManager.getReactHost()
   }

5️⃣  跳转 RN 页面：

   RNManager.startRNActivity(context, "RNApp")

============================================================
TIPS
