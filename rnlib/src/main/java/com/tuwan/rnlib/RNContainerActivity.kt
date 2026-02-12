package com.tuwan.rnlib

import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

/**
 * 通用 RN 容器 Activity。
 * 通过 Intent extra 传入组件名，可以承载任意 RN 页面。
 *
 * 注意：宿主 Application 必须实现 ReactApplication，
 * ReactActivity 会自动从 Application 获取 ReactHost。
 */
class RNContainerActivity : ReactActivity() {

    companion object {
        const val EXTRA_COMPONENT_NAME = "rn_component_name"

        /**
         * 临时存储组件名，解决 ReactActivity 在 intent 就绪前
         * 就调用 getMainComponentName() 的时序问题。
         */
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
