# Ads cho Jetpack Compose

Module tham khảo cấu trúc `Greensoft-Logo-Generate/core/ads`, nhưng tách cấu hình, vòng đời request và template để dùng trong Compose. Không dùng Activity/Fragment riêng cho từng màn. Home và Library dùng cùng API như các màn khác.

## Gắn native vào màn

```kotlin
// Đặt state ở cấp màn, bên ngoài LazyColumn/LazyRow.
val feedAd = rememberInlineAdState(ads, AdPlacements.HomeFeed)
LazyColumn {
    item(key = "native") {
        InlineAdSlot(feedAd, attribution = homeString(R.string.home_ad_attribution))
    }
}
```

Đổi layout bằng `native_template_size` của placement trong Remote Config. Không cần sửa Home hoặc tạo HomeNativeAd. Mỗi instance hiển thị dùng một state riêng; không gắn cùng một native state vào hai view đồng thời. `state.status` có Idle/Loading/Loaded/Failed/Disabled; `state.reload()` cho phép tải lại theo hành động của app.

NativeAdCard giữ chiều cao bằng chiều cao template + 32dp padding cả khi tải hoặc no-fill; placeholder không có nội dung quảng cáo giả và không ghi impression. Nó không thu về 0 khi callback về. Request/ad được giữ ở scope màn và hủy khi scope kết thúc hoặc unit ID đổi. View có clip bounds và SDK asset registration để click, impression và AdChoices do SDK xử lý.

## Nguồn ID và Firebase Remote Config

1. `src/main/assets/ads/ads_ids_default.json`: JSON **key–value** chứa ID mặc định của banner/interstitial/native/rewarded/rewarded-interstitial/app-open và các placement mẫu.
2. `app/src/main/res/xml/remote_config_defaults.xml`: giá trị mặc định của các nhóm Remote Config; placement tham chiếu `ad_id_key` nên không phải lặp ID ở XML.
3. Firebase Remote Config `ad_unit_ids`: ghi đè từng key trong JSON. Các key không được ghi đè vẫn dùng fallback đóng gói. Xóa key trên Firebase để khôi phục fallback.
4. Nếu placement có `ad_id` trực tiếp, giá trị đó ưu tiên hơn `ad_id_key`.

Ví dụ giá trị parameter `ad_unit_ids` trên Firebase:

```json
{
  "home_feed_after_continue_watching": "ca-app-pub-1234567890123456/1234567890",
  "library_recently_watched_bottom": "ca-app-pub-1234567890123456/2345678901",
  "onboarding_step4_notifications_complete": "ca-app-pub-1234567890123456/3456789012",
  "rewarded": "ca-app-pub-1234567890123456/4567890123",
  "rewarded_interstitial": "ca-app-pub-1234567890123456/5678901234",
  "app_open": "ca-app-pub-1234567890123456/6789012345"
}
```

Các số trên chỉ minh họa cấu trúc. JSON mặc định trong repo dùng demo ID chính thức của Google. Không lấy production ID từ project tham chiếu. AdMob **application ID** (`~`) vẫn đặt trong AndroidManifest; đây không phải ad unit ID (`/`) và không đổi được qua Remote Config sau SDK initialization.

Khởi tạo một lần tại composition root:

```kotlin
val ads = AdsController(application, versionName, debug, appScope,
    defaultsResource = R.xml.remote_config_defaults)
```

`ads.refresh()` fetch + activate. Bản debug có minimum fetch interval 0; release 3600 giây. Không chờ Firebase/network để mở màn. JSON lỗi giữ giá trị hợp lệ gần nhất của parameter đó; các parameter hợp lệ khác vẫn áp dụng. `ads.invalidConfigParameters` cho biết tên parameter lỗi.

`ad_id: ""`, ID map có value rỗng, `is_enable: false`, tắt global hoặc disabled version đều ngừng placement. Đổi ID hủy request cũ và không nhận callback cũ vào state mới. Fullscreen cache được kiểm tra ID/format trước khi show.

### Parameter cùng cấu trúc với project tham chiếu

| Parameter | Nội dung hỗ trợ |
| --- | --- |
| `ads_config` | `enable`, `disabled_versions`, `placements.<name>.enable` (tương thích config trước) |
| `ad_unit_ids` | Object key–value ID overrides |
| `banner_native_ad_places` | Array placement banner và native |
| `banner_native_ad_places_2` | Danh sách ưu tiên nếu không rỗng; `[]` dùng lại danh sách chính |
| `rewarded_and_inter_ad_places` | Array interstitial, rewarded, rewarded_interstitial |
| `app_open_ad_places` | Array app_open |
| `banner_ad_config`, `native_ad_config` | Retry policy |
| `interstitial_ad_config` | Retry, `time_interval`, `ads_per_session`, `time_per_session` |
| `rewarded_ad_config`, `rewarded_interstitial_ad_config`, `app_open_ad_config` | Retry và giới hạn thời gian/session nếu được đặt |

Mỗi array thay thế **cả nhóm**: thêm placement mới thì giữ lại những placement khác vẫn cần. Array rỗng tắt cả nhóm. Parameter lỗi không xóa nhóm đang chạy.

```json
[
  {
    "place_name": "home_feed_after_continue_watching",
    "ad_id_key": "home_feed_after_continue_watching",
    "ad_type": "native",
    "is_enable": true,
    "native_template_size": "medium_media_left",
    "background_color": "#241812",
    "primary_text_color": "#FFFFFF",
    "body_text_color": "#C9BCAE",
    "background_cta": "#FF7A3C",
    "cta_text_color": "#1A0E05",
    "border_color": "#4A3325",
    "background_radius": 16,
    "cta_radius_dp": 24
  },
  {
    "place_name": "library_recently_watched_bottom",
    "ad_id_key": "library_recently_watched_bottom",
    "ad_type": "banner",
    "banner_size": "anchored",
    "is_collapsible": false,
    "is_enable": true
  }
]
```

Fullscreen placement còn có `is_auto_load_after_dismiss` và `is_ignore_interval` (không bỏ qua cap số lượt/session). Retry dùng `is_enable_retry`, `max_retry_count` (0–5), `retry_interval_second_list` (0–60 giây mỗi lần); mỗi request timeout 15 giây. Cấu hình không hợp lệ không làm crash màn.

## Native templates

Có đủ 19 key từ `NativeTemplateSize` của project tham chiếu. Template dùng chung asset bridge SDK, không nhân bản request code. Height dưới đây chưa gồm padding 32dp của NativeAdCard.

| Enum / key | Bố cục | Height |
| --- | --- | --- |
| Small / `small` | Icon + nội dung + CTA phải | 148dp |
| SmallCtaTop / `small_cta_top` | CTA trên | 168dp |
| SmallCtaBottom / `small_cta_bottom` | CTA dưới, mặc định Home | 168dp |
| SmallCtaRight / `small_cta_right` | CTA phải, gọn | 120dp |
| MiniCtaRight / `mini_cta_right` | Icon, headline một dòng, CTA phải | 88dp |
| SmallLong / `small_long` | Nội dung cao hơn, CTA dưới | 192dp |
| SmallForPopup / `small_for_popup` | Nội dung căn giữa cho popup | 216dp |
| Medium / `medium` | Media + nội dung, CTA dưới | 320dp |
| MediumCtaTop / `medium_cta_top` | Media + CTA trên | 320dp |
| MediumCtaBottom / `medium_cta_bottom` | Media + CTA dưới | 320dp |
| MediumCtaRightTop / `medium_cta_right_top` | CTA phải, căn trên | 300dp |
| MediumCtaRight / `medium_cta_right` | CTA phải, căn giữa | 300dp |
| MediumMediaRight / `medium_media_right` | Media phải | 220dp |
| MediumMediaLeft / `medium_media_left` | Media trái | 220dp |
| FullCtaBottom / `full_cta_bottom` | Media lớn, CTA dưới | 520dp |
| FullCtaTop / `full_cta_top` | Media lớn, CTA trên | 520dp |
| FullCtaRight / `full_cta_right` | Media lớn, CTA phải | 520dp |
| FullInterstitialV1 / `full_interstitial_v1` | Media trước, nội dung/CTA sau | 560dp |
| FullInterstitialV2 / `full_interstitial_v2` | CTA/nội dung trước, media sau | 560dp |

Xem `NativeTemplatePreviews.kt` trong Android Studio, `NativeTemplateGallery()` cho debug screen, hoặc `NativeAdPreview(NativeTemplate.Medium, "Ad · Preview")` để duyệt mẫu mà không request ads. Full native có thể đặt trong `NativeAdDialog(state, attribution, closeLabel, onDismiss)`; luôn có nút đóng, không giả lập SDK interstitial.

## Banner

```kotlin
val banner = rememberInlineAdState(ads, AdPlacements.LibraryBottom)
// Gắn vào Column, Scaffold.bottomBar hoặc LazyColumn.item.
InlineAdSlot(banner, attribution = localizedAdLabel)
```

`banner_size`: anchored adaptive, inline adaptive (max height 100dp), standard_medium (300x250), standard_large (320x100). `anchored_large` dùng 320x100 để tương thích SDK 24.7 hiện tại (API large adaptive của bản tham chiếu không có ở SDK này). Truyền `widthDp` vào rememberBannerAdState nếu vùng đặt hẹp hơn màn. `is_collapsible` gửi SDK extras bottom; mặc định false. Banner giữ một AdView ở scope màn, pause/resume theo lifecycle và destroy khi rời màn.

## Interstitial, rewarded, rewarded interstitial và app-open

```kotlin
// Trong Compose; tự đợi SDK ready và cập nhật khi config thay đổi.
PreloadAdEffect(ads, AdPlacements.OnboardingComplete)

// Tại điểm chuyển màn đã chọn:
ads.showInterstitial(activity, AdPlacements.OnboardingComplete) { result ->
    continueNavigation() // callback đúng một lần, kể cả no-fill/disabled/busy
}

PreloadAdEffect(ads, AdPlacements.DetailEpisodeUnlockRewarded)
// Khi user đồng ý xem quảng cáo:
ads.showRewarded(activity, AdPlacements.DetailEpisodeUnlockRewarded,
    onReward = { reward -> grantReward(reward.type, reward.amount) },
    onComplete = { result -> closeLoadingUi() })

PreloadAdEffect(ads, AdPlacements.SplashAppOpen)
ads.showAppOpen(activity, AdPlacements.SplashAppOpen) { result -> continueStartup() }
```

Cũng có `ads.preload(name)` cho caller không dùng Compose; request đợi SDK ready. Các API show không khóa điều hướng để đợi quảng cáo chưa tải. `AdShowResult` phân biệt Dismissed, Disabled, NotReady, Busy, RateLimited, ActivityUnavailable, Failed. Fullscreen dùng chung khóa hiển thị và frequency cap; app-open hết hạn sau 4 giờ, các định dạng khác sau 1 giờ.

Reward chỉ phát từ SDK `OnUserEarnedRewardListener`, tối đa một lần mỗi lượt. Dismiss/no-fill không phát reward. Rewarded interstitial dùng `RewardedInterstitialPrompt` (title, description, watchLabel, skipLabel do app dịch) hoặc intro tương đương, rồi gọi `showRewardedInterstitial`.

`AppOpenAdEffect(ads, activity, name, enabled = eligible)` là hook tùy chọn cho foreground trên màn loading phù hợp; không tự gắn vào toàn bộ app và bỏ qua lần quay lại từ fullscreen ads. Home/Library/onboarding có các điểm đặt ban đầu; rewarded mở tập khóa đã gắn ở Detail/Player, interstitial sau 30 giây khi rời Player. App-open vẫn chỉ là API dự phòng.

## Ngôn ngữ và kiểm tra

Native attribution, close, prompt title/body/watch/skip do caller truyền chuỗi đã dịch theo app locale. Headline/body/CTA của quảng cáo lấy từ SDK, không thay bằng text của app. Home đã truyền nhãn quảng cáo qua resources 10 ngôn ngữ.

Unit tests kiểm tra fallback/override, JSON lỗi, tắt placement/version, nhóm đủ 6 formats, retry và frequency caps. Instrumentation `AdTemplatesTest` render 19 mẫu, tải native/banner thật với demo ID và preload 4 formats fullscreen. `HomeScrollTest` kiểm tra chiều cao slot qua các lần cuộn ra/vào. Ảnh QA lưu trong external files `ad-template-review/` của bản debug.

Tham khảo: [AdMob native](https://developers.google.com/admob/android/native/advanced), [demo IDs](https://developers.google.com/admob/android/test-ads), [Firebase defaults/activation](https://firebase.google.com/docs/remote-config/get-started?platform=android).

## Cập nhật tương thích Logo Maker

Cùng `place_name` có thể đổi `ad_type` giữa `native` và `banner` qua `InlineAdSlot`, không đổi code màn. Khi dùng `ad_id` trực tiếp, phải đổi ID đúng định dạng cùng lúc đổi type; ID native không dùng làm ID banner. API rememberNativeAdState/NativeAdCard và rememberBannerAdState/BannerAd vẫn dành cho các điểm muốn cố định định dạng.

`banner_native_ad_places_2` không rỗng được ưu tiên toàn bộ, không merge với danh sách chính. Để `[]` khi chưa dùng. Nếu `_2` lỗi JSON hoặc field không hợp lệ thì báo invalidParameters và dùng danh sách chính hợp lệ. Cả hai lỗi thì giữ cấu hình trước đó.

Các field từ XML Logo Maker đã được đối chiếu: place_name, ad_id, ad_type, banner_size, is_collapsible, native_template_size, is_enable, is_tracking_click, background_color, background_full_color, primary_text_color, body_text_color, background_cta, border_color, cta_radius_dp. Các field mở rộng cũng được hỗ trợ: is_tracking_show, cta_text_color, cta_border_color, media_background_color, background_radius. `cta_radius` cũ vẫn là alias; `cta_radius_dp` ưu tiên.

`background_full_color` tô nền ngoài card và dialog, `background_color` tô card; CTA có nền, chữ, viền và radius riêng. Callback SDK click/impression phát events khi cờ tương ứng bật; app forward sang Firebase Analytics `ad_placement_click`/`ad_placement_impression` với ad_placement/ad_format. Cờ false tắt custom event đó, không thay hành vi click của SDK.

Các value Firebase để copy riêng nằm ở `docs/firebase-ads-values/*.json`; file import đầy đủ ở `docs/firebase-remote-config-ads.json`. Value xuất ra dùng ad_id trực tiếp như Logo Maker, nên ưu tiên hơn ad_unit_ids. XML trong app dùng ad_id_key để giữ JSON ID fallback tập trung. Firebase chưa tạo parameter vẫn dùng fallback này.

## Đổi loại fullscreen tại cùng placement

Dùng API động khi muốn điều khiển định dạng bằng Firebase:

```kotlin
PreloadAdEffect(ads, AdPlacements.OnboardingComplete)
ads.showFullscreenAd(activity, AdPlacements.OnboardingComplete,
    onReward = { reward -> grantReward(reward.type, reward.amount) },
    onComplete = { result -> continueNavigation() })
```

Giữ `place_name`, đổi đồng thời `ad_type` và `ad_id` trong `rewarded_and_inter_ad_places`. Hỗ trợ interstitial ↔ rewarded ↔ rewarded_interstitial; app-open cũng có thể được resolve khi placement thuộc app_open_ad_places. Vị trí app-open vẫn chỉ nên được gắn ở luồng loading/startup đã chọn.

`showFullscreenAd` lấy loại mới tại thời điểm show. Cache/request cũ bị bỏ khi loại hoặc ID đổi; nếu loại mới chưa preload xong, trả NotReady, không hiển thị nhầm ad cũ hoặc chặn điều hướng. Frequency cap dùng policy của loại thực tế đang hiển thị.

`onReward` chỉ chạy cho rewarded/rewarded_interstitial khi SDK xác nhận earned reward, tối đa một lần. Interstitial không phát reward. Không dùng onComplete/Dismissed thay cho onReward để cấp thưởng. Caller vẫn chịu trách nhiệm nội dung đồng ý xem/giới thiệu thưởng phù hợp với placement; RewardedInterstitialPrompt là component có sẵn cho luồng này.

Các API showInterstitial/showRewarded/showRewardedInterstitial vẫn cố định định dạng cho caller cần hợp đồng đó. Điểm hoàn tất onboarding và RewardedInterstitialPrompt trong app đã chuyển sang API động.

## Flow ads đang gắn

- Home: home_feed_after_continue_watching, inline theo Remote Config.
- Library: library_recently_watched_bottom, inline theo Remote Config.
- Kết thúc Step 4: onboarding_step4_notifications_complete.
- Back khỏi Player sau 30 giây: player_exit_to_previous_screen (auto interstitial, chịu cap; không ép xem rewarded ở điểm tự động này).
- Tập khóa trên Detail: detail_episode_unlock_rewarded.
- Tập khóa trong Player: player_episode_picker_unlock_rewarded.

Hai điểm unlock có prompt 10 ngôn ngữ, pause video phía sau, chỉ ghi quyền xem khi SDK earned reward. Quyền truy cập cho đúng show/episode lưu DataStore. No-fill/bỏ qua không cấp quyền. Không dùng ad completion làm bằng chứng nhận thưởng.
