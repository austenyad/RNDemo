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

/**
 * RN 模块管理器，封装所有 React Native 初始化和页面跳转逻辑。
 *
 * 宿主 App 使用方式：
 * 1. Application.onCreate() 中调用 RNManager.init(this, packages) { BuildConfig.DEBUG }
 * 2. 跳转 RN 页面：RNManager.startRNActivity(context, "组件名")
 */
object RNManager {

    private var application: Application? = null
    private var isInitialized = false

    @Suppress("DEPRECATION")
    private var _reactNativeHost: ReactNativeHost? = null

    /**
     * 初始化 RN 运行时，必须在 Application.onCreate() 中调用。
     *
     * @param app Application 实例
     * @param packages ReactPackage 列表（由宿主 App 通过 PackageList 提供）
     * @param devSupport 是否开启开发者菜单（默认 false）
     */
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

    /**
     * 打开一个 RN 页面。
     *
     * @param context 上下文
     * @param componentName RN 端 AppRegistry.registerComponent 注册的组件名
     * @param launchProps 传递给 RN 组件的初始参数（可选）
     */
    @JvmStatic
    @JvmOverloads
    fun startRNActivity(
        context: Context,
        componentName: String,
        launchProps: Map<String, String>? = null
    ) {
        check(isInitialized) { "RNManager.init() must be called before startRNActivity()" }
        // 提前设置组件名，避免 ReactActivity 在 intent 就绪前读取到默认值
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
