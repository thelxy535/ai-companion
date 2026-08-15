package com.companion.cc

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CCApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化逻辑
    }
}
