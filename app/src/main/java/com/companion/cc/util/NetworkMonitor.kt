package com.companion.cc.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 网络状态
 */
sealed class NetworkState {
    object Online : NetworkState()
    object Offline : NetworkState()
}

/**
 * 网络监控器
 *
 * 功能：
 * 1. 实时监控网络状态
 * 2. 提供 Flow 供 UI 订阅
 * 3. 自动处理生命周期
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * 网络状态 Flow
     *
     * 订阅此 Flow 可以实时监听网络变化
     */
    val networkState: Flow<NetworkState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Logger.d("NetworkMonitor", "【网络】网络可用")
                trySend(NetworkState.Online)
            }

            override fun onLost(network: Network) {
                Logger.d("NetworkMonitor", "【网络】网络断开")
                trySend(NetworkState.Offline)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val isConnected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                 networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                Logger.d("NetworkMonitor", "【网络】能力变化: isConnected=$isConnected")
                trySend(if (isConnected) NetworkState.Online else NetworkState.Offline)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // 发送当前网络状态
        trySend(getCurrentNetworkState())

        awaitClose {
            Logger.d("NetworkMonitor", "【网络】取消监听")
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()  // 只在状态变化时发送

    /**
     * 获取当前网络状态（同步）
     */
    fun getCurrentNetworkState(): NetworkState {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        return if (capabilities != null &&
                  capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                  capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            NetworkState.Online
        } else {
            NetworkState.Offline
        }
    }

    /**
     * 检查是否在线（同步）
     */
    fun isOnline(): Boolean = getCurrentNetworkState() is NetworkState.Online
}
