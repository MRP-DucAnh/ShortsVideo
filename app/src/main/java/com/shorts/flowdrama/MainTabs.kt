package com.shorts.flowdrama

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorts.flowdrama.browse.*
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

private data class Tab(val name: String, val icon: ImageVector)

private val tabs =
    listOf(
        Tab("Home", FlowIcons.Home),
        Tab("Reels", FlowIcons.Reels),
        Tab("Browse", FlowIcons.Browse),
        Tab("Library", FlowIcons.Bookmark),
        Tab("Settings", FlowIcons.Settings),
    )

@Composable
internal fun MainTabs(
    initialTab: Int,
    catalog: Catalog,
    preferences: Preferences,
    saved: Set<String>,
    history: List<Progress>,
    container: AppContainer,
    vm: AppViewModel,
    push: (Route) -> Unit,
    onNotifications: () -> Unit,
) {
    val online by com.shorts.flowdrama.network.rememberOnline()
    val likedShows by vm.likedShows.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableIntStateOf(initialTab) }
    val holder = rememberSaveableStateHolder()
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp,
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(tab.icon, tr(tab.name), Modifier.size(23.dp)) },
                        label = { Text(tr(tab.name)) },
                        colors =
                            NavigationBarItemDefaults.colors(
                                selectedIconColor = Sunset.Accent,
                                selectedTextColor = Sunset.Accent,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = Sunset.Text3,
                                unselectedTextColor = Sunset.Text3,
                            ),
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            holder.SaveableStateProvider(selected) {
                when (selected) {
                    0 -> HomeScreen(
                            catalog,
                            history,
                            container.ads,
                            { push(Route(Destination.Detail, it)) },
                            { push(Route(Destination.Search)) },
                            { selected = 2 },
                            onNotifications,
                            online = online,
                            onRetry = vm::load,
                        )
                    1 ->
                        ReelsScreen(
                            catalog,
                            container.catalog,
                            preferences,
                            { push(Route(Destination.Detail, it)) },
                            {
                                vm.update {
                                    it.copy(
                                        speed =
                                            when (it.speed) {
                                                1f -> 1.5f
                                                1.5f -> 2f
                                                else -> 1f
                                            }
                                    )
                                }
                            },
                            saved = saved,
                            onSave = vm::save,
                            likedShows = likedShows,
                            onLike = vm::toggleShowLike,
                        )
                    2 ->
                        BrowseScreen(
                            catalog,
                            preferences,
                            saved,
                            container.catalog,
                            { id, num -> push(Route(Destination.Player, id, num)) },
                            vm::save,
                        )
                    3 ->
                        LibraryScreen(
                            catalog,
                            history,
                            saved,
                            container.ads,
                            { push(Route(Destination.History)) },
                            { push(Route(Destination.Saved)) },
                            { push(Route(Destination.Detail, it)) },
                        )
                    4 ->
                        SettingsScreen(
                            preferences,
                            BuildConfig.VERSION_NAME,
                            { push(Route(Destination.Language)) },
                            vm::update,
                            { push(Route(Destination.Policy, it)) },
                        )
                }
            }
        }
    }
}
