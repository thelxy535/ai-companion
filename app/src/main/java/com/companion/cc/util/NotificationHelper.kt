package com.companion.cc.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.annotation.SuppressLint
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.companion.cc.MainActivity
import com.companion.cc.R
import com.companion.cc.domain.memory.MemoryInboxNotificationPolicy

/**
 * 通知管理器
 * 负责发送各种类型的通知
 */
object NotificationHelper {

    private const val CHANNEL_ID = "cc_switch_chat_v2"
    private const val CHANNEL_NAME = "弦曜消息"
    private const val CHANNEL_DESCRIPTION = "角色消息、主动联系和记忆提醒"

    /**
     * 创建通知渠道（Android 8.0+）
     */
    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 180, 100, 180)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * 发送聊天提醒通知
     */
    @SuppressLint("NotificationPermission")
    fun sendChatReminderNotification(context: Context) {
        // V7 设置页通知开关门控
        if (!com.companion.cc.data.local.SettingsManager.isNotificationPrefEnabled(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("今天还没聊天哦 💬")
            .setContentText("和小璨聊聊天，分享你的心情吧~")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (!canPostNotifications(context)) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1001, notification)
    }

    /**
     * V9PM 主动消息通知（文案=消息内容本身，像微信）
     */
    @SuppressLint("NotificationPermission")
    fun sendProactiveMessageNotification(
        context: Context,
        companionName: String,
        content: String,
        companionId: String? = null
    ) {
        if (!com.companion.cc.data.local.SettingsManager.isNotificationPrefEnabled(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            companionId?.takeIf { it.isNotBlank() }?.let { putExtra("open_companion_id", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1003,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(companionName)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        if (!canPostNotifications(context)) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1003, notification)
    }

    /** Shows a completed reply only when the user has left the app. */
    @SuppressLint("NotificationPermission")
    fun sendIncomingReplyNotification(
        context: Context,
        companionName: String,
        content: String,
        companionId: String
    ) {
        if (!com.companion.cc.data.local.SettingsManager.isNotificationPrefEnabled(context)) return
        if (isAppInForeground(context) || !canPostNotifications(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_companion_id", companionId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1005,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(companionName)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
            ?.notify(1005, notification)
    }

    /**
     * 发送记忆回顾通知
     */
    @SuppressLint("NotificationPermission")
    fun sendMemoryReviewNotification(context: Context, memoryCount: Int, daysAgo: Int) {
        // V7 设置页通知开关门控
        if (!com.companion.cc.data.local.SettingsManager.isNotificationPrefEnabled(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("记忆回顾 📖")
            .setContentText("${daysAgo}天前你和小璨聊了 $memoryCount 条消息")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (!canPostNotifications(context)) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1002, notification)
    }

    /** Summarizes the inbox without exposing candidate text or evidence in a notification. */
    @SuppressLint("NotificationPermission")
    fun sendMemoryInboxNotification(
        context: Context,
        scopeKey: String,
        pendingCount: Int,
        threshold: Int = MemoryInboxNotificationPolicy.DEFAULT_THRESHOLD,
        cooldownMs: Long = MemoryInboxNotificationPolicy.DEFAULT_COOLDOWN_MS,
        now: Long = System.currentTimeMillis()
    ) {
        if (!com.companion.cc.data.local.SettingsManager.isNotificationPrefEnabled(context)) return
        val preferences = context.getSharedPreferences("memory_inbox_notifications", Context.MODE_PRIVATE)
        val key = "last:$scopeKey"
        val lastNotifiedAt = preferences.getLong(key, 0L)
        if (!MemoryInboxNotificationPolicy.shouldNotify(pendingCount, now, lastNotifiedAt, threshold, cooldownMs)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_memory_inbox", true)
            putExtra("memory_inbox_companion_id", scopeKey.substringAfter("companion:", ""))
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1004,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("记忆收件箱")
            .setContentText("整理出 $pendingCount 组可能值得留下的片段")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (!canPostNotifications(context)) return
        context.getSystemService(Context.NOTIFICATION_SERVICE)
            ?.let { (it as NotificationManager).notify(1004, notification) }
        preferences.edit().putLong(key, now).apply()
    }

    internal fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun isAppInForeground(context: Context): Boolean =
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager)
            ?.runningAppProcesses
            ?.any { process ->
                process.processName == context.packageName &&
                    process.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            } == true
}
