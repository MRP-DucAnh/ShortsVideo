# Firebase Remote Config — cặp key/value FlowDrama

Mỗi key là parameter String. Copy JSON vào Default value rồi Publish. Demo IDs của Google. `_2` để [] nếu dùng danh sách chính.

## Key: `ads_config`

Value:

```json
{
  "enable": true,
  "disabled_versions": [],
  "placements": {}
}
```

## Key: `ad_unit_ids`

Value:

```json
{
  "banner": "ca-app-pub-3940256099942544/9214589741",
  "interstitial": "ca-app-pub-3940256099942544/1033173712",
  "native": "ca-app-pub-3940256099942544/2247696110",
  "rewarded": "ca-app-pub-3940256099942544/5224354917",
  "rewarded_interstitial": "ca-app-pub-3940256099942544/5354046379",
  "app_open": "ca-app-pub-3940256099942544/9257395921",
  "home_feed_after_continue_watching": "ca-app-pub-3940256099942544/2247696110",
  "library_recently_watched_bottom": "ca-app-pub-3940256099942544/9214589741",
  "onboarding_step4_notifications_complete": "ca-app-pub-3940256099942544/1033173712"
}
```

## Key: `banner_native_ad_places`

Value:

```json
[
  {
    "place_name": "home_feed_after_continue_watching",
    "ad_type": "native",
    "is_enable": true,
    "native_template_size": "small_cta_bottom",
    "is_tracking_click": true,
    "is_tracking_show": true,
    "banner_size": "anchored",
    "is_collapsible": false,
    "background_color": "#241812",
    "background_full_color": "#160F0A",
    "primary_text_color": "#FFFFFF",
    "body_text_color": "#C9BCAE",
    "background_cta": "#FF7A3C",
    "cta_text_color": "#1A0E05",
    "cta_border_color": "#FF7A3C",
    "border_color": "#4A3325",
    "media_background_color": "#160F0A",
    "background_radius": 16,
    "cta_radius_dp": 24,
    "ad_id": "ca-app-pub-3940256099942544/2247696110"
  },
  {
    "place_name": "library_recently_watched_bottom",
    "ad_type": "banner",
    "is_enable": true,
    "banner_size": "anchored",
    "is_collapsible": false,
    "is_tracking_click": true,
    "is_tracking_show": true,
    "native_template_size": "small_cta_bottom",
    "background_color": "#241812",
    "background_full_color": "#160F0A",
    "primary_text_color": "#FFFFFF",
    "body_text_color": "#C9BCAE",
    "background_cta": "#FF7A3C",
    "cta_text_color": "#1A0E05",
    "cta_border_color": "#FF7A3C",
    "border_color": "#4A3325",
    "media_background_color": "#160F0A",
    "background_radius": 16,
    "cta_radius_dp": 24,
    "ad_id": "ca-app-pub-3940256099942544/9214589741"
  }
]
```

## Key: `banner_native_ad_places_2`

Value:

```json
[]
```

## Key: `rewarded_and_inter_ad_places`

Value:

```json
[
  {
    "place_name": "onboarding_step4_notifications_complete",
    "ad_type": "interstitial",
    "is_enable": true,
    "is_tracking_click": true,
    "is_tracking_show": true,
    "is_auto_load_after_dismiss": false,
    "is_ignore_interval": false,
    "ad_id": "ca-app-pub-3940256099942544/1033173712"
  },
  {
    "place_name": "detail_episode_unlock_rewarded",
    "ad_type": "rewarded",
    "is_enable": true,
    "is_tracking_click": true,
    "is_tracking_show": true,
    "is_auto_load_after_dismiss": false,
    "is_ignore_interval": false,
    "ad_id": "ca-app-pub-3940256099942544/5224354917"
  },
  {
    "place_name": "detail_episode_unlock_rewarded_interstitial",
    "ad_type": "rewarded_interstitial",
    "is_enable": true,
    "is_tracking_click": true,
    "is_tracking_show": true,
    "is_auto_load_after_dismiss": false,
    "is_ignore_interval": false,
    "ad_id": "ca-app-pub-3940256099942544/5354046379"
  },
  {
    "place_name": "player_exit_to_previous_screen",
    "ad_type": "interstitial",
    "is_enable": true,
    "is_tracking_click": true,
    "is_tracking_show": true,
    "is_auto_load_after_dismiss": true,
    "is_ignore_interval": false,
    "ad_id": "ca-app-pub-3940256099942544/1033173712"
  },
  {
    "place_name": "player_episode_picker_unlock_rewarded",
    "ad_type": "rewarded",
    "is_enable": true,
    "is_tracking_click": true,
    "is_tracking_show": true,
    "is_auto_load_after_dismiss": true,
    "is_ignore_interval": false,
    "ad_id": "ca-app-pub-3940256099942544/5224354917"
  }
]
```

## Key: `app_open_ad_places`

Value:

```json
[
  {
    "place_name": "splash_app_open",
    "ad_type": "app_open",
    "is_enable": true,
    "is_tracking_click": true,
    "is_tracking_show": true,
    "ad_id": "ca-app-pub-3940256099942544/9257395921"
  },
  {
    "place_name": "resume_app_open",
    "ad_type": "app_open",
    "is_enable": true,
    "is_tracking_click": true,
    "is_tracking_show": true,
    "ad_id": "ca-app-pub-3940256099942544/9257395921"
  }
]
```

## Key: `banner_ad_config`

Value:

```json
{
  "is_enable_retry": true,
  "max_retry_count": 2,
  "retry_interval_second_list": [
    1,
    2,
    3
  ]
}
```

## Key: `native_ad_config`

Value:

```json
{
  "is_enable_retry": true,
  "max_retry_count": 2,
  "retry_interval_second_list": [
    1,
    2,
    3
  ]
}
```

## Key: `interstitial_ad_config`

Value:

```json
{
  "is_enable_retry": true,
  "max_retry_count": 2,
  "retry_interval_second_list": [
    1,
    2,
    3
  ],
  "time_interval": 15,
  "ads_per_session": 5,
  "time_per_session": 600
}
```

## Key: `rewarded_ad_config`

Value:

```json
{
  "is_enable_retry": true,
  "max_retry_count": 2,
  "retry_interval_second_list": [
    1,
    2,
    3
  ]
}
```

## Key: `rewarded_interstitial_ad_config`

Value:

```json
{
  "is_enable_retry": true,
  "max_retry_count": 2,
  "retry_interval_second_list": [
    1,
    2,
    3
  ]
}
```

## Key: `app_open_ad_config`

Value:

```json
{
  "is_enable_retry": true,
  "max_retry_count": 2,
  "retry_interval_second_list": [
    1,
    2,
    3
  ],
  "time_interval": 15
}
```

Home/Library dùng inline slot. Interstitial Player exit chỉ sau 30 giây trên Player và chịu frequency cap. Detail/Player có prompt rewarded mở đúng tập khóa; mở tập được lưu trên thiết bị. Rewarded interstitial có thể dùng thay rewarded tại hai điểm mở tập. Tắt hoặc cấu hình loại không có reward tại các điểm này không cấp quyền xem. App-open và placement detail_episode_unlock_rewarded_interstitial riêng vẫn là dự phòng.
