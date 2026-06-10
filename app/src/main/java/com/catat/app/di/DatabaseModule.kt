package com.catat.app.di

import android.content.Context
import androidx.room.Room
import com.catat.app.data.db.CatatDatabase
import com.catat.app.data.db.dao.BugReportDao
import com.catat.app.data.db.dao.TemplateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideCatatDatabase(@ApplicationContext ctx: Context): CatatDatabase =
        Room.databaseBuilder(ctx, CatatDatabase::class.java, "catat.db")
            .addMigrations(CatatDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideBugReportDao(db: CatatDatabase): BugReportDao = db.bugReportDao()

    @Provides
    fun provideTemplateDao(db: CatatDatabase): TemplateDao = db.templateDao()
}
