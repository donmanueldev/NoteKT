package com.manuelduarte077.notyapp.analytics

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {
    @Binds
    @Singleton
    abstract fun bindAnalyticsTracker(
        tracker: FirebaseAnalyticsTracker,
    ): AnalyticsTracker
}
