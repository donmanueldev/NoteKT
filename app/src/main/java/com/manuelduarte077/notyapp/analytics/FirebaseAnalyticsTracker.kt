package com.manuelduarte077.notyapp.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAnalyticsTracker @Inject constructor(
    @ApplicationContext context: Context,
) : AnalyticsTracker {
    private val analytics = FirebaseAnalytics.getInstance(context)

    override fun logEvent(name: String, parameters: Map<String, String>) {
        val bundle = Bundle().apply {
            parameters.forEach { (key, value) -> putString(key, value) }
        }
        analytics.logEvent(name, bundle)
    }

    override fun logScreenView(screenName: String) {
        val parameters = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, parameters)
    }
}
