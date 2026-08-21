package com.companion.cc.di

import com.companion.cc.domain.vision.ConfigurableVisionProvider
import com.companion.cc.domain.vision.VisionProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 图片理解服务依赖注入；具体服务由用户设置在运行时选择。 */
@Module
@InstallIn(SingletonComponent::class)
abstract class VisionModule {
    @Binds
    @Singleton
    abstract fun bindVisionProvider(
        configurableVisionProvider: ConfigurableVisionProvider
    ): VisionProvider
}
