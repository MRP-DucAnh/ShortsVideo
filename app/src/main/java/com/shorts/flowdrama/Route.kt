package com.shorts.flowdrama

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
enum class Destination {
    Splash,
    LanguageOnboard,
    Language,
    Intro,
    Main,
    Search,
    Detail,
    Player,
    History,
    Saved,
    Policy,
}

@Serializable
data class Route(val screen: Destination, val id: String = "", val number: Int = -1) : NavKey
