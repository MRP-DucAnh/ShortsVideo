package com.shorts.flowdrama

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.*
import androidx.navigation3.ui.NavDisplay
import com.shorts.flowdrama.browse.*
import com.shorts.flowdrama.data.DataStorePreferences
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.detail.*
import com.shorts.flowdrama.domain.*
import com.shorts.flowdrama.home.*
import com.shorts.flowdrama.library.*
import com.shorts.flowdrama.onboarding.*
import com.shorts.flowdrama.playback.*
import com.shorts.flowdrama.reels.*
import com.shorts.flowdrama.search.*
import com.shorts.flowdrama.settings.*
import kotlinx.coroutines.launch

@Composable
fun FlowDramaApp(container: AppContainer) {
    val vm: AppViewModel = viewModel { AppViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val preferences by vm.preferences.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(Route(Destination.Splash))
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var locked by rememberSaveable { mutableStateOf(false) }
    var notice by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val activity = context as Activity
    fun push(route: Route) {
        backStack.add(route)
    }
    fun back() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }
    fun finish() {
        vm.update { it.copy(onboardingDone = true) }
        container.ads.finishOnboarding(activity) {
            backStack.clear()
            backStack.add(Route(Destination.Main))
        }
    }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            finish()
            notice =
                if (granted)
                    "Notifications are enabled. Scheduled reminders are not configured yet."
                else "You can enable notifications in Android settings."
        }
    LaunchedEffect(state.ready) {
        if (state.ready && backStack.lastOrNull() == Route(Destination.Splash) && !state.error) {
            backStack.clear()
            backStack.add(
                Route(
                    if (preferences.onboardingDone) Destination.Main
                    else Destination.LanguageOnboard
                )
            )
        }
    }
    FlowTheme(preferences.language, preferences.lightTheme) {
        val translatorLanguage = LocalLanguage.current
        LaunchedEffect(vm, translatorLanguage) {
            vm.messages.collect { snackbar.showSnackbar(it, duration = SnackbarDuration.Short) }
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { back() },
                    modifier = Modifier.fillMaxSize(),
                    entryProvider = { key ->
                        val route = key as Route
                        NavEntry(route) {
                            val catalog = state.catalog
                            if (route.screen == Destination.Splash) {
                                if (state.error)
                                    BlankState(
                                        "You're offline",
                                        "Check Wi-Fi or mobile data - the river will still be here.",
                                        "Try again",
                                        Icons.Outlined.WifiOff,
                                        vm::load,
                                    )
                                else SplashScreen()
                            } else if (catalog == null) LoadingState()
                            else {
                                val show = catalog.shows.find { it.id == route.id }
                                when (route.screen) {
                                    Destination.Splash -> SplashScreen()
                                    Destination.LanguageOnboard,
                                    Destination.Language ->
                                        Box(Modifier.safeDrawingPadding()) {
                                            LanguageScreen(
                                                preferences.language,
                                                preferences.followDevice,
                                                route.screen == Destination.LanguageOnboard,
                                                { lang, device ->
                                                    vm.update {
                                                        it.copy(
                                                            language =
                                                                if (device)
                                                                    DataStorePreferences
                                                                        .deviceLanguage()
                                                                else lang,
                                                            followDevice = device,
                                                        )
                                                    }
                                                },
                                                { back() },
                                                { push(Route(Destination.Intro, number = 0)) },
                                                poster = catalog.shows.firstOrNull()?.posterUrl,
                                            )
                                        }
                                    Destination.Intro ->
                                        CinemaOnboarding(
                                            route.number,
                                            catalog.shows.firstOrNull()?.posterUrl,
                                            preferences.tasteIds,
                                            { tags ->
                                                vm.update {
                                                    it.copy(
                                                        tasteIds =
                                                            tags.intersect(
                                                                catalog.categories
                                                                    .map { it.id }
                                                                    .toSet()
                                                            )
                                                    )
                                                }
                                            },
                                            {
                                                if (route.number < 2)
                                                    push(
                                                        Route(
                                                            Destination.Intro,
                                                            number = route.number + 1,
                                                        )
                                                    )
                                                else finish()
                                            },
                                            {
                                                if (route.number < 2)
                                                    push(
                                                        Route(
                                                            Destination.Intro,
                                                            number = route.number + 1,
                                                        )
                                                    )
                                                else finish()
                                            },
                                            {
                                                if (Build.VERSION.SDK_INT >= 33)
                                                    permission.launch(
                                                        Manifest.permission.POST_NOTIFICATIONS
                                                    )
                                                else finish()
                                            },
                                        )
                                    Destination.Main ->
                                        MainTabs(
                                            route.number.coerceIn(0, 4),
                                            catalog,
                                            preferences,
                                            saved,
                                            history,
                                            container,
                                            vm,
                                            { push(it) },
                                            {
                                                notice =
                                                    "New episode reminders are managed in Android notification settings."
                                            },
                                        )
                                    Destination.Search ->
                                        Box(Modifier.safeDrawingPadding()) {
                                            SearchScreen(
                                                catalog,
                                                { back() },
                                                { push(Route(Destination.Detail, it)) },
                                            )
                                        }
                                    Destination.Detail,
                                    Destination.Player ->
                                        if (show != null) {
                                            val playbackVm: PlaybackViewModel =
                                                viewModel(key = "${route.screen}:${show.id}") {
                                                    PlaybackViewModel(
                                                        container.catalog,
                                                        container.library,
                                                        container.scope,
                                                    )
                                                }
                                            val playback by
                                                playbackVm.state.collectAsStateWithLifecycle()
                                            LaunchedEffect(route) {
                                                playbackVm.load(show.id, route.number)
                                            }
                                            val progress = history.find { it.showId == show.id }
                                            if (route.screen == Destination.Detail)
                                                Box(Modifier.safeDrawingPadding()) {
                                                    DetailScreen(
                                                        show,
                                                        playback.episodes.takeIf {
                                                            !playback.loading
                                                        },
                                                        show.id in saved,
                                                        progress,
                                                        playback.error,
                                                        { playbackVm.load(show.id, route.number) },
                                                        { back() },
                                                        { vm.save(show.id) },
                                                        { number ->
                                                            push(
                                                                Route(
                                                                    Destination.Player,
                                                                    show.id,
                                                                    number,
                                                                )
                                                            )
                                                        },
                                                        { locked = true },
                                                    )
                                                }
                                            else
                                                PlaybackScreen(
                                                    show,
                                                    playback,
                                                    preferences,
                                                    catalog.defaultLanguage,
                                                    progress,
                                                    { back() },
                                                    playbackVm::select,
                                                    vm::update,
                                                    playbackVm::persist,
                                                    { locked = true },
                                                    onCheckpoint = playbackVm::checkpoint,
                                                )
                                        }
                                    Destination.History ->
                                        Box(Modifier.safeDrawingPadding()) {
                                            HistoryScreen(
                                                catalog,
                                                history,
                                                { back() },
                                                { push(Route(Destination.Detail, it)) },
                                                vm::removeHistory,
                                                vm::clearHistory,
                                                {
                                                    backStack.clear()
                                                    backStack.add(
                                                        Route(Destination.Main, number = 2)
                                                    )
                                                },
                                            )
                                        }
                                    Destination.Saved ->
                                        Box(Modifier.safeDrawingPadding()) {
                                            SavedScreen(
                                                catalog,
                                                saved,
                                                { back() },
                                                { push(Route(Destination.Detail, it)) },
                                                vm::save,
                                                {
                                                    backStack.clear()
                                                    backStack.add(
                                                        Route(Destination.Main, number = 2)
                                                    )
                                                },
                                            )
                                        }
                                    Destination.Policy ->
                                        Box(Modifier.safeDrawingPadding()) {
                                            Column {
                                                Heading(route.id, { back() })
                                                Text(
                                                    if (route.id == "Open-source licenses")
                                                        "Kotlin, AndroidX / Jetpack Compose, Media3 — Apache 2.0\nOkHttp — Apache 2.0\nCoil — Apache 2.0\nFirebase & Google Mobile Ads — Google SDK terms\n\nSee dependency license details in docs/licenses.md."
                                                    else
                                                        "This take-home build has no published legal policy URL yet. It uses anonymous local viewing history and preferences, Firebase Remote Config, and Google test ads. No sign-in or payment is implemented.",
                                                    Modifier.padding(24.dp),
                                                )
                                            }
                                        }
                                }
                            }
                        }
                    },
                )
                SnackbarHost(
                    snackbar,
                    Modifier.align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 76.dp),
                    snackbar = { data ->
                        Snackbar(
                            data,
                            containerColor = Sunset.Surface3,
                            contentColor = Sunset.Success,
                        )
                    },
                )
            }
        }
        if (locked)
            AlertDialog(
                onDismissRequest = { locked = false },
                title = { Text(tr("Episode locked")) },
                text = {
                    Text(
                        tr(
                            "This episode is currently locked. Unlocking is not available in this version."
                        )
                    )
                },
                confirmButton = { TextButton(onClick = { locked = false }) { Text(tr("OK")) } },
            )
        if (notice.isNotEmpty())
            AlertDialog(
                onDismissRequest = { notice = "" },
                title = { Text(tr("Notifications")) },
                text = { Text(notice) },
                confirmButton = { TextButton(onClick = { notice = "" }) { Text(tr("OK")) } },
            )
    }
}
