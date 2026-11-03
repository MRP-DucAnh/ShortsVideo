package com.shorts.flowdrama

import android.app.Application
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.shorts.flowdrama.ads.AdPlacementEventType
import com.shorts.flowdrama.ads.AdsController
import com.shorts.flowdrama.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

/**
 * Composition root. Application dependencies are constructor-injected, never looked up by features.
 */
class FlowDramaApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, applicationScope)
    }
}

class AppContainer(application: Application, val scope: CoroutineScope) {
    val catalog = DefaultCatalogRepository(application, scope)
    val library = LocalLibraryRepository(application, scope)
    val preferences = DataStorePreferences(application, scope)
    val reminders = SharedPreferencesReminders(application, scope)
    val ads = AdsController(application, BuildConfig.VERSION_NAME, BuildConfig.DEBUG, scope, R.xml.remote_config_defaults)

    init {
        val analytics = FirebaseAnalytics.getInstance(application)
        scope.launch {
            ads.events.collect { event ->
                analytics.logEvent(
                    if (event.type == AdPlacementEventType.Click) "ad_placement_click" else "ad_placement_impression",
                    Bundle().apply {
                        putString("ad_placement", event.placement)
                        putString("ad_format", event.format.key)
                    },
                )
            }
        }
    }
}
