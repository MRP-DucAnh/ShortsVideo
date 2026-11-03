package com.shorts.flowdrama.ads

import com.shorts.flowdrama.ads.config.AdFormat

enum class AdPlacementEventType { Click, Impression }
data class AdPlacementEvent(val placement: String, val format: AdFormat, val type: AdPlacementEventType)
