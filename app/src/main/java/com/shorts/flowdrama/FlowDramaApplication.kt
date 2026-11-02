package com.shorts.flowdrama

import android.app.Application
import com.shorts.flowdrama.ads.AdsController
import com.shorts.flowdrama.data.*
import kotlinx.coroutines.*

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
    val ads = AdsController(application, BuildConfig.VERSION_NAME, BuildConfig.DEBUG, scope)
}
