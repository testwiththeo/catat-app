package com.catat.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Use cases are @Inject constructor classes — no manual @Provides needed.
 * This module is a placeholder for any future factory bindings.
 */
@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule
