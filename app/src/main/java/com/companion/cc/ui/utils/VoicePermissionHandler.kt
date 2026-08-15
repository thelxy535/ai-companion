package com.companion.cc.ui.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * 语音权限处理器
 */
object VoicePermissionHandler {

    /**
     * 检查录音权限
     */
    fun hasRecordAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 获取需要请求的权限
     */
    fun getRequiredPermissions(): Array<String> {
        return arrayOf(Manifest.permission.RECORD_AUDIO)
    }
}
