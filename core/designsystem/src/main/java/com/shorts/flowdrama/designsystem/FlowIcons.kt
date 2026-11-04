package com.shorts.flowdrama.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** SVG geometry from flowdrama-android-mockup.html; shared by all app screens. */
object FlowIcons {
    val Notifications by lazy { icon("Notifications", 1.8f,
            "M18 8.5a6 6 0 1 0-12 0c0 6-2.5 7.5-2.5 7.5h17S18 14.5 18 8.5z",
            "M13.7 20a2 2 0 0 1-3.4 0",
    ) }
    val Home by lazy { icon("Home", 1.7f,
            "M3 10.5 12 3l9 7.5",
            "M5 9.5V21h14V9.5",
    ) }
    val Reels by lazy { icon("Reels", 1.7f,
            "M9.0,3.0h6.0a3.0,3.0 0 0,1 3.0,3.0v12.0a3.0,3.0 0 0,1 -3.0,3.0h-6.0a3.0,3.0 0 0,1 -3.0,-3.0v-12.0a3.0,3.0 0 0,1 3.0,-3.0z",
            "m10.5 9.5 4 2.5-4 2.5z",
    ) }
    val Browse by lazy { icon("Browse", 1.7f,
            "M5.0,3.5h4.0a1.5,1.5 0 0,1 1.5,1.5v4.0a1.5,1.5 0 0,1 -1.5,1.5h-4.0a1.5,1.5 0 0,1 -1.5,-1.5v-4.0a1.5,1.5 0 0,1 1.5,-1.5z",
            "M15.0,3.5h4.0a1.5,1.5 0 0,1 1.5,1.5v4.0a1.5,1.5 0 0,1 -1.5,1.5h-4.0a1.5,1.5 0 0,1 -1.5,-1.5v-4.0a1.5,1.5 0 0,1 1.5,-1.5z",
            "M5.0,13.5h4.0a1.5,1.5 0 0,1 1.5,1.5v4.0a1.5,1.5 0 0,1 -1.5,1.5h-4.0a1.5,1.5 0 0,1 -1.5,-1.5v-4.0a1.5,1.5 0 0,1 1.5,-1.5z",
            "M15.0,13.5h4.0a1.5,1.5 0 0,1 1.5,1.5v4.0a1.5,1.5 0 0,1 -1.5,1.5h-4.0a1.5,1.5 0 0,1 -1.5,-1.5v-4.0a1.5,1.5 0 0,1 1.5,-1.5z",
    ) }
    val Bookmark by lazy { icon("Bookmark", 1.7f,
            "M6.5 3.5h11v17l-5.5-4-5.5 4z",
    ) }
    val BookmarkFilled by lazy { icon("BookmarkFilled", 1.7f,
            "M6.5 3.5h11v17l-5.5-4-5.5 4z",
        filled = true,
    ) }
    val Settings by lazy { icon("Settings", 1.7f,
            "M9.0,12.0a3.0,3.0 0 1,0 6.0,0a3.0,3.0 0 1,0 -6.0,0",
            "M19.4 15a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-2.7 1.1V21a2 2 0 1 1-4 0v-.1A1.6 1.6 0 0 0 7.5 19.4l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1A1.6 1.6 0 0 0 3 14a2 2 0 1 1 0-4h.1A1.6 1.6 0 0 0 4.6 7.5l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1A1.6 1.6 0 0 0 10 3a2 2 0 1 1 4 0v.1a1.6 1.6 0 0 0 2.7 1.1l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1A1.6 1.6 0 0 0 21 10a2 2 0 1 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1z",
    ) }
    val ArrowBack by lazy { icon("ArrowBack", 2f,
            "M19 12H5",
            "m11 18-6-6 6-6",
    ) }
    val Search by lazy { icon("Search", 1.8f,
            "M4.0,11.0a7.0,7.0 0 1,0 14.0,0a7.0,7.0 0 1,0 -14.0,0",
            "M16.5 16.5 21 21",
    ) }
    val Heart by lazy { icon("Heart", 1.7f,
            "M12 20s-7-4.6-7-9.6A3.9 3.9 0 0 1 12 7.6a3.9 3.9 0 0 1 7 2.8C19 15.4 12 20 12 20z",
    ) }
    val HeartFilled by lazy { icon("HeartFilled", 1.7f,
            "M12 20s-7-4.6-7-9.6A3.9 3.9 0 0 1 12 7.6a3.9 3.9 0 0 1 7 2.8C19 15.4 12 20 12 20z",
        filled = true,
    ) }
    val Lock by lazy { icon("Lock", 1.8f,
            "M7.0,11.0h10.0a2.0,2.0 0 0,1 2.0,2.0v5.0a2.0,2.0 0 0,1 -2.0,2.0h-10.0a2.0,2.0 0 0,1 -2.0,-2.0v-5.0a2.0,2.0 0 0,1 2.0,-2.0z",
            "M8 11V8a4 4 0 0 1 8 0v3",
    ) }
    val Episodes by lazy { icon("Episodes", 1.7f,
            "M6.0,4.0h12.0a2.5,2.5 0 0,1 2.5,2.5v11.0a2.5,2.5 0 0,1 -2.5,2.5h-12.0a2.5,2.5 0 0,1 -2.5,-2.5v-11.0a2.5,2.5 0 0,1 2.5,-2.5z",
            "M3.5 9.5h17M9 9.5V20",
    ) }
    val Audio by lazy { icon("Audio", 1.7f,
            "M5 9v6h4l5 4V5L9 9H5z",
            "M17 9.5a4 4 0 0 1 0 5",
    ) }
    val History by lazy { icon("History", 1.7f,
            "M3.5,12.0a8.5,8.5 0 1,0 17.0,0a8.5,8.5 0 1,0 -17.0,0",
            "M12 7.5V12l3 2",
    ) }
    val WifiOff by lazy {
        icon("WifiOff", 1.5f,
            "M2 8.5A15 15 0 0 1 22 8.5",
            "M5.5 12.5a10 10 0 0 1 13 0",
            "M9 16.5a5 5 0 0 1 6 0",
            "M11.0,20.0a1.0,1.0 0 1,0 2.0,0a1.0,1.0 0 1,0 -2.0,0",
            "M3 3l18 18",
        )
    }

    // Controls represented by text/CSS or absent in the HTML: matching round-stroke style.
    val Play by lazy { icon("Play", 1.7f, "M8 5l11 7-11 7z") }
    val Pause by lazy { icon("Pause", 1.7f, "M8 5v14M16 5v14") }
    val Add by lazy { icon("Add", 1.7f, "M12 5v14M5 12h14") }
    val Close by lazy { icon("Close", 1.7f, "M6 6l12 12M18 6L6 18") }
    val Check by lazy { icon("Check", 1.7f, "M5 12l4 4L19 6") }
    val ChevronRight by lazy { icon("ChevronRight", 1.7f, "M9 6l6 6-6 6") }
    val ExpandMore by lazy { icon("ExpandMore", 1.7f, "M6 9l6 6 6-6") }
    val ExpandLess by lazy { icon("ExpandLess", 1.7f, "M6 15l6-6 6 6") }
    val Menu by lazy { icon("Menu", 1.7f, "M4 6h16M4 12h16M4 18h16") }
    val MoreHoriz by lazy { icon("MoreHoriz", 3f, "M5 12h.01M12 12h.01M19 12h.01") }

    private fun icon(name: String, width: Float, vararg paths: String, filled: Boolean = false): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEachIndexed { index, data ->
                addPath(
                    pathData = PathParser().parsePathString(data).toNodes(),
                    fill = if (filled) SolidColor(Color.Black) else null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = if (name == "WifiOff" && index == 4) 1.9f else width,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
}
