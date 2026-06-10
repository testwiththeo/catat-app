package com.catat.app.di

import android.app.ActivityManager
import android.content.ClipboardManager
import android.content.Context
import android.media.projection.MediaProjectionManager
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.view.WindowManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideWindowManager(@ApplicationContext ctx: Context): WindowManager =
        ctx.getSystemService(WindowManager::class.java)

    @Provides @Singleton
    fun provideMediaProjectionManager(@ApplicationContext ctx: Context): MediaProjectionManager =
        ctx.getSystemService(MediaProjectionManager::class.java)

    @Provides @Singleton
    fun provideConnectivityManager(@ApplicationContext ctx: Context): ConnectivityManager =
        ctx.getSystemService(ConnectivityManager::class.java)

    @Provides @Singleton
    fun provideBatteryManager(@ApplicationContext ctx: Context): BatteryManager =
        ctx.getSystemService(BatteryManager::class.java)

    @Provides @Singleton
    fun provideActivityManager(@ApplicationContext ctx: Context): ActivityManager =
        ctx.getSystemService(ActivityManager::class.java)

    @Provides @Singleton
    fun provideClipboardManager(@ApplicationContext ctx: Context): ClipboardManager =
        ctx.getSystemService(ClipboardManager::class.java)
}
