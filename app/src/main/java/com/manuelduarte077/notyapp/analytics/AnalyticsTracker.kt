package com.manuelduarte077.notyapp.analytics

interface AnalyticsTracker {
    fun logEvent(name: String, parameters: Map<String, String> = emptyMap())

    fun logScreenView(screenName: String)
}
