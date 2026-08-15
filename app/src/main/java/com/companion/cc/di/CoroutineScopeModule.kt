package com.companion.cc.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * 应用级别的 CoroutineScope
 *
 * 替代 GlobalScope，提供受控的生命周期
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * 协程模块
 *
 * 提供应用级别的 CoroutineScope
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {

    /**
     * 应用级别的 Scope
     *
     * 生命周期：应用启动到应用销毁
     * 用途：后台任务、长时间运行的任务
     *
     * 相比 GlobalScope 的优势：
     * 1. 生命周期受控（可以取消）
     * 2. 可以在测试中替换
     * 3. 符合依赖注入最佳实践
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
