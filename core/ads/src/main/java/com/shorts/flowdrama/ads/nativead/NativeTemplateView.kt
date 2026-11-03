package com.shorts.flowdrama.ads.nativead

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.gms.ads.nativead.*
import com.shorts.flowdrama.ads.config.*

/** Android SDK asset registration stays inside this bridge; the public API is Compose. */
internal fun nativeTemplateView(
    context: Context, placement: AdPlacement, ad: NativeAd?, attribution: String,
    preview: NativePreviewCreative? = null,
): NativeAdView = NativeTemplateBuilder(context, placement, ad, attribution, preview).build()

data class NativePreviewCreative(
    val headline: String = "Discover your next story",
    val body: String = "A native ad preview. No ad requests or tracked clicks.",
    val callToAction: String = "Learn more",
)

private class NativeTemplateBuilder(
    private val context: Context,
    private val placement: AdPlacement,
    private val ad: NativeAd?,
    private val attribution: String,
    private val preview: NativePreviewCreative?,
) {
    private val density = context.resources.displayMetrics.density
    private val style = placement.style
    private val template = placement.template
    private val host = NativeAdView(context)
    private val match = ViewGroup.LayoutParams.MATCH_PARENT
    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
    private fun dp(value: Int) = (value * density).toInt()
    private fun column() = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun shape(color: String, radius: Int, border: String? = null) = GradientDrawable().apply {
        setColor(Color.parseColor(color)); cornerRadius = dp(radius).toFloat()
        if (border != null) setStroke(dp(1), Color.parseColor(border))
    }

    fun build(): NativeAdView {
        host.clipChildren = true
        host.clipToPadding = true
        host.background = shape(style.background, style.radiusDp, style.border)
        host.clipToOutline = true
        val root = column().apply { setPadding(dp(12), dp(10), dp(12), dp(10)) }
        root.addView(text(attribution, 10f, style.body, 1).apply {
            setPadding(0, 0, dp(24), dp(4))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        })
        val button = cta()
        if (template.cta == CtaPosition.Top) root.addView(button, LinearLayout.LayoutParams(match, dp(44)))
        when (template.layout) {
            NativeLayout.Compact, NativeLayout.Long, NativeLayout.Popup -> compact(root, button)
            NativeLayout.MediaLeft, NativeLayout.MediaRight -> sideMedia(root)
            NativeLayout.MediaTop, NativeLayout.Full -> {
                root.addView(detailsRow(button), LinearLayout.LayoutParams(match, wrap))
                root.addView(media(), LinearLayout.LayoutParams(match, 0, 1f).apply { topMargin = dp(6); bottomMargin = dp(6) })
            }
            NativeLayout.InterstitialV1 -> {
                root.addView(media(), LinearLayout.LayoutParams(match, 0, 1f).apply { bottomMargin = dp(12) })
                root.addView(detailsRow(button), LinearLayout.LayoutParams(match, wrap))
            }
            NativeLayout.InterstitialV2 -> {
                root.addView(detailsRow(button), LinearLayout.LayoutParams(match, wrap))
                root.addView(media(), LinearLayout.LayoutParams(match, 0, 1f).apply { topMargin = dp(12) })
            }
        }
        if (template.cta == CtaPosition.Bottom) root.addView(button, LinearLayout.LayoutParams(match, dp(44)).apply { topMargin = dp(6) })
        host.addView(root, FrameLayout.LayoutParams(match, match))
        ad?.let(host::setNativeAd)
        return host
    }

    private fun compact(root: LinearLayout, button: Button) {
        val content = detailsRow(button)
        if (template.layout == NativeLayout.Popup) {
            content.gravity = Gravity.CENTER
            root.gravity = Gravity.CENTER_HORIZONTAL
        }
        root.addView(content, LinearLayout.LayoutParams(match, 0, 1f))
    }

    private fun detailsRow(button: Button): LinearLayout = row().apply {
        val small = template == NativeTemplate.MiniCtaRight
        val icon = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = shape(style.ctaBackground, 12)
            clipToOutline = true
            setImageDrawable(ad?.icon?.drawable)
            visibility = if (ad?.icon == null && preview == null) View.GONE else View.VISIBLE
        }
        host.iconView = icon
        addView(icon, LinearLayout.LayoutParams(dp(if (small) 36 else 48), dp(if (small) 36 else 48)).apply { marginEnd = dp(8) })
        addView(details(), LinearLayout.LayoutParams(0, wrap, 1f))
        if (template.cta == CtaPosition.Right || template.cta == CtaPosition.RightTop) {
            if (template.cta == CtaPosition.RightTop) gravity = Gravity.TOP
            addView(button, LinearLayout.LayoutParams(dp(if (small) 88 else 96), dp(44)).apply { marginStart = dp(8) })
        }
    }

    private fun details(): LinearLayout = column().apply {
        val headline = text(ad?.headline ?: preview?.headline.orEmpty(), 16f, style.headline,
            if (template == NativeTemplate.MiniCtaRight) 1 else 2).apply { setTypeface(typeface, Typeface.BOLD) }
        host.headlineView = headline
        addView(headline)
        if (template != NativeTemplate.MiniCtaRight) {
            val body = text(ad?.body ?: preview?.body.orEmpty(), 12f, style.body, 2).apply {
                visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
            }
            host.bodyView = body
            addView(body)
            val advertiser = text(ad?.advertiser.orEmpty(), 11f, style.body, 1).apply {
                visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
            }
            host.advertiserView = advertiser
            addView(advertiser)
            ad?.starRating?.let { stars ->
                val rating = RatingBar(context, null, android.R.attr.ratingBarStyleSmall).apply {
                    numStars = 5; stepSize = .5f; rating = stars.toFloat(); setIsIndicator(true)
                }
                host.starRatingView = rating
                addView(rating, LinearLayout.LayoutParams(wrap, wrap))
            }
        }
    }

    private fun sideMedia(root: LinearLayout) {
        val side = row()
        val details = details()
        val media = media()
        val mediaParams = LinearLayout.LayoutParams(dp(128), match).apply {
            if (template.layout == NativeLayout.MediaLeft) marginEnd = dp(10) else marginStart = dp(10)
        }
        if (template.layout == NativeLayout.MediaLeft) side.addView(media, mediaParams)
        side.addView(details, LinearLayout.LayoutParams(0, wrap, 1f))
        if (template.layout == NativeLayout.MediaRight) side.addView(media, mediaParams)
        root.addView(side, LinearLayout.LayoutParams(match, 0, 1f))
    }

    private fun media(): View {
        if (preview != null) return FrameLayout(context).apply {
            background = shape(style.ctaBackground, 12)
            addView(text("▶", 36f, style.ctaText, 1), FrameLayout.LayoutParams(wrap, wrap, Gravity.CENTER))
        }
        return MediaView(context).apply {
            setBackgroundColor(Color.parseColor(style.mediaBackground))
            host.mediaView = this
            mediaContent = ad?.mediaContent
            setImageScaleType(ImageView.ScaleType.CENTER_CROP)
        }
    }

    private fun cta() = Button(context).apply {
        text = ad?.callToAction ?: preview?.callToAction.orEmpty()
        visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
        isAllCaps = false
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        setTextColor(Color.parseColor(style.ctaText))
        setTypeface(typeface, Typeface.BOLD)
        textSize = 14f
        if (template.cta == CtaPosition.Right || template.cta == CtaPosition.RightTop) {
            maxLines = 2
            if (Build.VERSION.SDK_INT >= 26) {
                setAutoSizeTextTypeUniformWithConfiguration(10, 14, 1, TypedValue.COMPLEX_UNIT_SP)
            } else textSize = 12f
        }
        minHeight = 0; minimumHeight = 0
        setPadding(dp(8), 0, dp(8), 0)
        background = shape(style.ctaBackground, style.ctaRadiusDp, style.ctaBorder)
        host.callToActionView = this
    }

    private fun text(value: String, sp: Float, color: String, lines: Int) = TextView(context).apply {
        text = value; textSize = sp; setTextColor(Color.parseColor(color))
        maxLines = lines; ellipsize = TextUtils.TruncateAt.END
        layoutParams = LinearLayout.LayoutParams(match, wrap)
    }
}
