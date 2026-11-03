# Ads và Firebase Remote Config

Hướng dẫn đầy đủ, ví dụ gắn Compose và danh sách template: [core/ads/README.md](../core/ads/README.md).

- ID fallback key–value: `core/ads/src/main/assets/ads/ads_ids_default.json`.
- Firebase defaults: `app/src/main/res/xml/remote_config_defaults.xml`.
- Remote override IDs: parameter `ad_unit_ids`; JSON lỗi giữ cấu hình hợp lệ gần nhất.
- Các nhóm placement: `banner_native_ad_places`, `rewarded_and_inter_ad_places`, `app_open_ad_places`.
- `ads_config` vẫn hỗ trợ global enable, disabled_versions và placement kill switch.

Đã có adapter banner, native, interstitial, rewarded, rewarded interstitial và app-open; native có 19 template theo project tham chiếu. Banner và native dùng state cấp màn, giữ chiều cao slot trong khi tải/fail để tránh nhảy khi vuốt. NativeAdView được giới hạn bounds. Bảng Native Ad Validator của SDK được tắt ở debug manifest để không phủ lên status bar.

Các điểm ban đầu: `home_feed_after_continue_watching`, `library_recently_watched_bottom`, `onboarding_step4_notifications_complete`. Rewarded đã gắn với prompt mở tập khóa ở Detail/Player. Thêm interstitial ở player_exit_to_previous_screen khi Back sau 30 giây. App-open chưa tự động gắn.

Các fallback ID là demo ID chính thức của Google. Không copy production ID từ project tham chiếu. AdMob application ID vẫn cấu hình qua AndroidManifest. Chưa thay đổi parameter trên Firebase Console của chủ project; kiểm tra hiện tại dùng packaged defaults và SDK test ads.
