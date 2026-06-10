package com.catat.app.di

import com.catat.app.data.repository.CaptureRepositoryImpl
import com.catat.app.data.repository.DeviceInfoRepositoryImpl
import com.catat.app.data.repository.ReportRepositoryImpl
import com.catat.app.data.repository.TemplateRepositoryImpl
import com.catat.app.data.storage.FileStorage
import com.catat.app.data.storage.FileStorageImpl
import com.catat.app.domain.repository.CaptureRepository
import com.catat.app.domain.repository.DeviceInfoRepository
import com.catat.app.domain.repository.ReportRepository
import com.catat.app.domain.repository.TemplateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    @Binds @Singleton
    abstract fun bindTemplateRepository(impl: TemplateRepositoryImpl): TemplateRepository

    @Binds @Singleton
    abstract fun bindCaptureRepository(impl: CaptureRepositoryImpl): CaptureRepository

    @Binds @Singleton
    abstract fun bindDeviceInfoRepository(impl: DeviceInfoRepositoryImpl): DeviceInfoRepository

    @Binds @Singleton
    abstract fun bindFileStorage(impl: FileStorageImpl): FileStorage
}
