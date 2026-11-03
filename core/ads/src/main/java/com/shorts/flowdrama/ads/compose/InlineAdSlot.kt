package com.shorts.flowdrama.ads.compose

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorts.flowdrama.ads.*
import com.shorts.flowdrama.ads.banner.*
import com.shorts.flowdrama.ads.config.*
import com.shorts.flowdrama.ads.nativead.*

@Stable
class InlineAdState internal constructor(
    val native: NativeAdState,
    val banner: BannerAdState,
) {
    var format by mutableStateOf<AdFormat?>(null)
        internal set
    val status: AdLoadStatus get() = when (format) {
        AdFormat.Native -> native.status
        AdFormat.Banner -> banner.status
        else -> AdLoadStatus.Disabled
    }
    fun reload() { if (format == AdFormat.Native) native.reload() else banner.reload() }
}

/** One placement, either banner or native. Keep the state outside virtualized list items. */
@Composable
fun rememberInlineAdState(controller: AdsController, placement: String, widthDp: Int? = null): InlineAdState {
    val config by controller.config.collectAsStateWithLifecycle()
    val native = rememberNativeAdState(controller, placement)
    val banner = rememberBannerAdState(controller, placement, widthDp)
    val state = remember(native, banner) { InlineAdState(native, banner) }
    val format = config.placements[placement]?.format
    SideEffect { state.format = format }
    return state
}

@Composable
fun InlineAdSlot(state: InlineAdState, attribution: String, modifier: Modifier = Modifier) {
    when (state.format) {
        AdFormat.Native -> NativeAdCard(state.native, attribution, modifier)
        AdFormat.Banner -> BannerAd(state.banner, modifier)
        else -> Unit
    }
}
