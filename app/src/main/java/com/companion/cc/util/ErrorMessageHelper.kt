package com.companion.cc.util

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * 错误消息转换工具
 * 将技术错误转换为用户友好的提示
 */
object ErrorMessageHelper {

    /**
     * 将异常转换为用户友好的错误消息
     */
    fun getFriendlyMessage(throwable: Throwable): String {
        return when {
            // 网络连接问题
            throwable is UnknownHostException -> {
                "网络连接失败，请检查网络设置"
            }
            throwable is ConnectException -> {
                "无法连接到服务器，请检查Base URL配置"
            }
            throwable is SocketTimeoutException -> {
                "请求超时，请稍后重试"
            }
            throwable is SSLException -> {
                "安全连接失败，请检查网络环境"
            }

            // API错误
            throwable.message?.contains("401") == true -> {
                "API Key无效或已过期，请在设置中更新"
            }
            throwable.message?.contains("403") == true -> {
                "没有权限访问该服务，请检查API Key"
            }
            throwable.message?.contains("400") == true -> {
                "请求格式错误，请检查模型配置"
            }
            throwable.message?.contains("429") == true -> {
                "请求过于频繁，请稍后再试"
            }
            throwable.message?.contains("500") == true -> {
                "服务器错误，请稍后重试"
            }
            throwable.message?.contains("502") == true ||
            throwable.message?.contains("503") == true -> {
                "服务暂时不可用，请稍后重试"
            }

            // 其他已知错误
            throwable.message?.contains("empty response", ignoreCase = true) == true -> {
                "服务器返回空响应，请检查API配置"
            }
            throwable.message?.contains("json", ignoreCase = true) == true -> {
                "数据解析失败，可能是API格式变更"
            }

            // 默认消息
            else -> {
                val message = throwable.message
                if (!message.isNullOrBlank() && message.length < 100) {
                    "出错了：$message"
                } else {
                    "AI回复失败，请稍后重试"
                }
            }
        }
    }

    /**
     * 判断错误是否可以重试
     */
    fun isRetryable(throwable: Throwable): Boolean {
        return when {
            // 网络暂时性问题 - 可重试
            throwable is SocketTimeoutException -> true
            throwable is ConnectException -> true
            throwable.message?.contains("500") == true -> true
            throwable.message?.contains("502") == true -> true
            throwable.message?.contains("503") == true -> true

            // 配置错误 - 不可重试
            throwable is UnknownHostException -> false
            throwable.message?.contains("401") == true -> false
            throwable.message?.contains("403") == true -> false
            throwable.message?.contains("400") == true -> false

            // 默认不重试
            else -> false
        }
    }

    /**
     * 获取错误的建议操作
     */
    fun getSuggestion(throwable: Throwable): String? {
        return when {
            throwable is UnknownHostException || throwable is ConnectException -> {
                "💡 请检查：1) 网络连接 2) Base URL是否正确"
            }
            throwable.message?.contains("401") == true ||
            throwable.message?.contains("403") == true -> {
                "💡 请在设置中更新API Key"
            }
            throwable.message?.contains("400") == true -> {
                "💡 请在设置中检查模型名称是否正确"
            }
            throwable.message?.contains("429") == true -> {
                "💡 请等待1-2分钟后再试"
            }
            else -> null
        }
    }
}
