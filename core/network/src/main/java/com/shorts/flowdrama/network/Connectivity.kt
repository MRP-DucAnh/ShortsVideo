package com.shorts.flowdrama.network

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/** Observe the default route: Wi-Fi without validated internet is offline too. */
class ConnectivityMonitor(context: Context) {
    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    @Suppress("DEPRECATION")
    fun isOnline(): Boolean {
        // Some devices briefly retain validated capabilities after all radios are disabled.
        if (manager.activeNetworkInfo?.isConnected != true) return false
        return manager.getNetworkCapabilities(manager.activeNetwork)?.let {
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } == true
    }

    val online = callbackFlow {
        fun publish() { trySend(isOnline()) }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = publish()
            override fun onLost(network: Network) = publish()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = publish()
        }
        manager.registerDefaultNetworkCallback(callback)
        publish()
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.conflate().stateIn(CoroutineScope(SupervisorJob() + Dispatchers.Default),
        SharingStarted.WhileSubscribed(5000), isOnline())
}

@Volatile private var sharedMonitor: ConnectivityMonitor? = null
private fun monitor(context: Context): ConnectivityMonitor = sharedMonitor ?: synchronized(ConnectivityMonitor::class.java) {
    sharedMonitor ?: ConnectivityMonitor(context.applicationContext).also { sharedMonitor = it }
}

@Composable
fun rememberOnline(): State<Boolean> {
    val context = LocalContext.current.applicationContext
    val monitor = remember(context) { monitor(context) }
    return monitor.online.collectAsState()
}

/** Read the current system state rather than a previously emitted Flow value. */
fun hasInternetConnection(context: Context): Boolean = monitor(context).isOnline()

fun openInternetSettings(context: Context) {
    val actions = listOf(
        if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_WIRELESS_SETTINGS,
        Settings.ACTION_WIRELESS_SETTINGS,
        Settings.ACTION_SETTINGS,
    )
    for (action in actions) {
        if (runCatching { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
    }
}
