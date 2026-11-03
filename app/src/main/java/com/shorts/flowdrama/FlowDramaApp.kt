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
import com.shorts.flowdrama.ads.compose.PreloadAdEffect
import com.shorts.flowdrama.ads.config.AdPlacements
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FlowDramaApp(container: AppContainer) {
    PreloadAdEffect(container.ads, AdPlacements.OnboardingComplete)
    val vm: AppViewModel = viewModel { AppViewModel(container) }
    val onboardingVm: OnboardingViewModel = viewModel {
        OnboardingViewModel(container.preferences, container.reminders)
    }
    val reminderWindows by onboardingVm.selectedReminderWindows.collectAsStateWithLifecycle()
    val onboardingSaveError by onboardingVm.saveError.collectAsStateWithLifecycle()

    val state by vm.state.collectAsStateWithLifecycle()
    val preferences by vm.preferences.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val episodeProgress by vm.episodeProgress.collectAsStateWithLifecycle()
    val likedEpisodes by vm.likedEpisodes.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(Route(Destination.Splash))
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val unlockState by vm.episodeUnlock.state.collectAsStateWithLifecycle()
    val fullscreenShowing by container.ads.isFullscreenShowing.collectAsStateWithLifecycle()
    var enteredPlayerAt by remember { mutableLongStateOf(0L) }
    var pendingPlayerExitAd by remember { mutableStateOf(false) }
    LaunchedEffect(backStack.lastOrNull()) {
        if ((backStack.lastOrNull() as? Route)?.screen == Destination.Player)
            enteredPlayerAt = android.os.SystemClock.elapsedRealtime()
    }
    LaunchedEffect(unlockState.target, fullscreenShowing) {
        if (unlockState.target == null && !fullscreenShowing &&
            (backStack.lastOrNull() as? Route)?.screen == Destination.Player)
            enteredPlayerAt = android.os.SystemClock.elapsedRealtime()
    }
    var notice by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    LaunchedEffect(state.catalog) {
        state.catalog?.let { preloadHomeHero(context.applicationContext, it) }
    }
    val activity = context as Activity
    LaunchedEffect(pendingPlayerExitAd) {
        if (pendingPlayerExitAd) {
            pendingPlayerExitAd = false
            // Navigate first; lifecycle and blockedByAd pause any Player retained during transition.
            container.ads.showInterstitial(activity, AdPlacements.PlayerExit)
        }
    }

    fun push(route: Route) {
        backStack.add(route)
    }
    fun back() {
        val leavingPlayer = (backStack.lastOrNull() as? Route)?.screen == Destination.Player
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
            if (leavingPlayer && playerExitAdEligible(enteredPlayerAt, android.os.SystemClock.elapsedRealtime()))
                pendingPlayerExitAd = true
        }
    }
    LaunchedEffect(unlockState.completed) {
        unlockState.completed?.let { target ->
            if ((backStack.lastOrNull() as? Route)?.screen == Destination.Player) backStack.removeLastOrNull()
            push(Route(Destination.Player, target.showId, target.number))
            vm.episodeUnlock.consumeCompletion()
        }
    }
    fun finish() {
        vm.update { it.copy(onboardingDone = true) }
        container.ads.showFullscreenAd(activity, AdPlacements.OnboardingComplete) {
            backStack.clear()
            backStack.add(Route(Destination.Main))
        }
    }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            onboardingVm.setRemindersEnabled(granted)
            finish()
        }
    var splashMinimumElapsed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(3_000L)
        splashMinimumElapsed = true
    }
    LaunchedEffect(state.ready, state.error, splashMinimumElapsed) {
        if (splashMinimumElapsed && state.ready && backStack.lastOrNull() == Route(Destination.Splash) && !state.error) {
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
                                    Destination.LanguageOnboard ->
                                        OnboardingScreen1Language(
                                            language = preferences.language,
                                            followDevice = preferences.followDevice,
                                            onSelect = { lang, device ->
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
                                            onContinue = {
                                                push(Route(Destination.Intro, number = 0))
                                            },
                                        )
                                    Destination.Language ->
                                        SettingsLanguageScreen(
                                            language = preferences.language,
                                            followDevice = preferences.followDevice,
                                            onSelect = { lang, device ->
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
                                            onBack = { back() },
                                        )
                                    Destination.Intro ->
                                        OnboardingFlow(
                                            step = route.number,
                                            selectedMoodIds = preferences.moodIds,
                                            selectedTagIds = preferences.tasteIds,
                                            categories = catalog.categories,
                                            selectedReminderWindows = reminderWindows,
                                            saveError = onboardingSaveError,
                                            onToggleMood = {
                                                onboardingVm.toggleMood(it)
                                            },
                                            onToggleGenre = onboardingVm::toggleGenre,
                                            onToggleReminder = onboardingVm::toggleReminder,
                                            onNext = {
                                                push(
                                                    Route(
                                                        Destination.Intro,
                                                        number = route.number + 1,
                                                    )
                                                )
                                            },
                                            onSkip = {
                                                if (route.number < 2)
                                                    push(
                                                        Route(
                                                            Destination.Intro,
                                                            number = route.number + 1,
                                                        )
                                                    )
                                                else {
                                                    onboardingVm.setRemindersEnabled(false)
                                                    finish()
                                                }
                                            },
                                            onNotification = {
                                                if (Build.VERSION.SDK_INT >= 33)
                                                    permission.launch(
                                                        Manifest.permission.POST_NOTIFICATIONS
                                                    )
                                                else {
                                                    val manager =
                                                        activity.getSystemService(
                                                            android.content.Context
                                                                .NOTIFICATION_SERVICE
                                                        ) as android.app.NotificationManager
                                                    onboardingVm.setRemindersEnabled(
                                                        manager.areNotificationsEnabled()
                                                    )
                                                    finish()
                                                }
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
                                                        container.preferences,
                                                    )
                                                }
                                            val playback by
                                                playbackVm.state.collectAsStateWithLifecycle()
                                            LaunchedEffect(route) {
                                                playbackVm.load(show.id, route.number)
                                            }
                                            PreloadAdEffect(container.ads,
                                                if (route.screen == Destination.Detail) AdPlacements.DetailEpisodeUnlockRewarded
                                                else AdPlacements.PlayerEpisodeUnlockRewarded)
                                            if (route.screen == Destination.Player)
                                                PreloadAdEffect(container.ads, AdPlacements.PlayerExit)
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
                                                        { episode -> vm.episodeUnlock.request(show.id, episode.number) },
                                                        watched = episodeProgress.orEmpty().filter { it.showId == show.id && it.watched }.map { it.episodeNumber }.toSet(),
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
                                                    { episode -> vm.episodeUnlock.request(show.id, episode.number, AdPlacements.PlayerEpisodeUnlockRewarded) },
                                                    onCheckpoint = playbackVm::checkpoint,
                                                    onEpisodeComplete = playbackVm::completeEpisode,
                                                    blockedByAd = unlockState.target != null || fullscreenShowing,
                                                    saved = show.id in saved,
                                                    onSave = { vm.save(show.id) },
                                                    episodeProgress = episodeProgress.orEmpty().filter { it.showId == show.id },
                                                    progressReady = episodeProgress != null,
                                                    likedEpisodes = likedEpisodes,
                                                    onLikeEpisode = vm::toggleEpisodeLike,
                                                )
                                        }
                                    Destination.History ->
                                        Box(Modifier.safeDrawingPadding()) {
                                            HistoryScreen(
                                                catalog,
                                                episodeProgress,
                                                { back() },
                                                { showId, episodeNumber -> push(Route(Destination.Player, showId, episodeNumber)) },
                                                vm::removeEpisodeHistory,
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
        unlockState.target?.let { target ->
            val targetShow = state.catalog?.shows?.find { it.id == target.showId }
            EpisodeUnlockDialog(container.ads, vm.episodeUnlock, unlockState, activity,
                targetShow?.let { showTitle(it) }.orEmpty())
        }
        if (notice.isNotEmpty())
            AlertDialog(
                onDismissRequest = { notice = "" },
                title = { Text(tr("Notifications")) },
                text = { Text(notice) },
                confirmButton = { TextButton(onClick = { notice = "" }) { Text(tr("OK")) } },
            )
    }
}
