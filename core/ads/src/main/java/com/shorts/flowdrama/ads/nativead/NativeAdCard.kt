package com.shorts.flowdrama.ads.nativead

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.shorts.flowdrama.ads.config.*

/** Keep this item in the feed while loading/failing so its measured height stays stable. */
@Composable
fun NativeAdCard(state: NativeAdState, attribution: String, modifier: Modifier = Modifier) {
    val place = state.placement ?: return
    if (!state.enabled) return
    Box(modifier.fillMaxWidth().height((place.template.heightDp + 32).dp)
        .clipToBounds().background(Color(android.graphics.Color.parseColor(place.style.fullBackground)))
        .testTag("ad:${state.placementName}").padding(16.dp)) {
        val ad = state.ad
        if (ad == null) NativePlaceholder(place)
        else key(ad, place.template, place.style, attribution) {
            AndroidView(
                modifier = Modifier.fillMaxSize().clipToBounds(),
                factory = { nativeTemplateView(it, place, ad, attribution) },
                onRelease = { it.destroy() },
            )
        }
    }
}

@Composable
private fun NativePlaceholder(place: AdPlacement) {
    Column(Modifier.fillMaxSize().clip(RoundedCornerShape(place.style.radiusDp.dp))
        .background(Color(android.graphics.Color.parseColor(place.style.background))).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val tint = Color(android.graphics.Color.parseColor(place.style.body)).copy(alpha = .12f)
        Box(Modifier.fillMaxWidth(.65f).height(14.dp).background(tint, RoundedCornerShape(6.dp)))
        Box(Modifier.fillMaxWidth(.9f).height(10.dp).background(tint, RoundedCornerShape(6.dp)))
    }
}

/** Same template builder, without network requests, SDK impressions or clickable ad assets. */
@Composable
fun NativeAdPreview(template: NativeTemplate, attribution: String, modifier: Modifier = Modifier,
    style: NativeAdStyle = NativeAdStyle(), creative: NativePreviewCreative = NativePreviewCreative()) {
    val place = remember(template, style) { AdPlacement("preview", AdFormat.Native, template = template, style = style) }
    key(template, style, creative, attribution) {
        AndroidView(
            modifier = modifier.fillMaxWidth().height(template.heightDp.dp).clipToBounds(),
            factory = { nativeTemplateView(it, place, null, attribution, creative) },
            onRelease = { it.destroy() },
        )
    }
}
