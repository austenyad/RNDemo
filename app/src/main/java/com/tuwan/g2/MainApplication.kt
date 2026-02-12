package com.tuwan.g2

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeHost
import com.tuwan.rnlib.RNManager

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
