package com.companion.cc.util

import android.util.Log
import com.companion.cc.BuildConfig

/**
 * 统一的日志管理工具
 *
 * 功能：
 * 1. Debug/Release 分离
 * 2. 自动脱敏敏感信息
 * 3. 统一日志格式
 * 4. 可扩展崩溃上报
 */
object Logger {
    private const val TAG_PREFIX = "CC-Switch"

    /**
     * Debug 日志（仅在 Debug 模式下输出）
     */
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d("$TAG_PREFIX-$tag", message)
        }
    }

    /**
     * Info 日志
     */
    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i("$TAG_PREFIX-$tag", message)
        }
    }

    /**
     * Warning 日志
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w("$TAG_PREFIX-$tag", message, throwable)
    }

    /**
     * Error 日志（总是输出，生产环境可上报）
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$TAG_PREFIX-$tag", message, throwable)

        // 生产环境上报到崩溃分析平台
        if (!BuildConfig.DEBUG) {
            // TODO: 集成 Firebase Crashlytics 或其他崩溃上报服务
            // FirebaseCrashlytics.getInstance().log("$tag: $message")
            // throwable?.let { FirebaseCrashlytics.getInstance().recordException(it) }
        }
    }

    /**
     * 敏感信息日志（自动脱敏）
     *
     * 会自动替换以下敏感信息：
     * - API Key / Token
     * - Password
     * - 用户 ID（部分脱敏）
     */
    fun sensitive(tag: String, message: String) {
        if (!BuildConfig.DEBUG) return

        val sanitized = message
            // API Key / Token 脱敏
            .replace(Regex("(key|token|apikey|api_key)\\s*[=:]\\s*['\"]?([\\w-]+)['\"]?", RegexOption.IGNORE_CASE)) {
                "${it.groupValues[1]}=***${it.groupValues[2].takeLast(4)}"
            }
            // Password 脱敏
            .replace(Regex("(password|pwd|pass)\\s*[=:]\\s*['\"]?[^'\"\\s]+['\"]?", RegexOption.IGNORE_CASE)) {
                "${it.groupValues[1]}=***"
            }
            // 用户 ID 部分脱敏（保留前后各4位）
            .replace(Regex("user_([\\w-]{8,})")) {
                val id = it.groupValues[1]
                if (id.length > 12) {
                    "user_${id.take(4)}***${id.takeLast(4)}"
                } else {
                    it.value
                }
            }

        Log.d("$TAG_PREFIX-$tag", "[SENSITIVE] $sanitized")
    }

    /**
     * 性能日志（记录耗时操作）
     */
    fun perf(tag: String, operation: String, durationMs: Long) {
        if (BuildConfig.DEBUG) {
            val level = when {
                durationMs > 1000 -> "⚠️ SLOW"
                durationMs > 500 -> "⏱️ MEDIUM"
                else -> "✅ FAST"
            }
            Log.d("$TAG_PREFIX-$tag", "[$level] $operation took ${durationMs}ms")
        }
    }

    /**
     * 网络请求日志
     */
    fun network(tag: String, method: String, url: String, statusCode: Int? = null, durationMs: Long? = null) {
        if (!BuildConfig.DEBUG) return

        val statusStr = statusCode?.let { " - Status: $it" } ?: ""
        val durationStr = durationMs?.let { " - ${it}ms" } ?: ""
        Log.d("$TAG_PREFIX-Network", "[$method] $url$statusStr$durationStr")
    }

    /**
     * 状态日志（用于追踪状态变化）
     */
    fun state(tag: String, stateName: String, oldValue: Any?, newValue: Any?) {
        if (BuildConfig.DEBUG) {
            Log.d("$TAG_PREFIX-$tag", "[STATE] $stateName: $oldValue → $newValue")
        }
    }

    /**
     * 生命周期日志
     */
    fun lifecycle(tag: String, event: String) {
        if (BuildConfig.DEBUG) {
            Log.d("$TAG_PREFIX-Lifecycle", "[$tag] $event")
        }
    }
}
