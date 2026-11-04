# Ads và Firebase Remote Config

Hướng dẫn đầy đủ, ví dụ gắn Compose và danh sách template: [core/ads/README.md](../core/ads/README.md).

- ID fallback key–value: `core/ads/src/main/assets/ads/ads_ids_default.json`.
- Firebase defaults: `app/src/main/res/xml/remote_config_defaults.xml`.
- Remote override IDs: parameter `ad_unit_ids`; JSON lỗi giữ cấu hình hợp lệ gần nhất.
- Các nhóm placement: `banner_native_ad_places`, `rewarded_and_inter_ad_places`, `app_open_ad_places`.
- `ads_config` vẫn hỗ trợ global enable, disabled_versions và placement kill switch.

Đã có adapter banner, native, interstitial, rewarded, rewarded interstitial và app-open; native có 19 template theo project tham chiếu. Banner và native dùng state cấp màn, giữ chiều cao slot trong khi tải/fail để tránh nhảy khi vuốt. NativeAdView được giới hạn bounds. Bảng Native Ad Validator của SDK được tắt ở debug manifest để không phủ lên status bar.

Các điểm ban đầu: `home_feed_after_continue_watching`, `library_recently_watched_bottom`, `onboarding_step4_notifications_complete`. Rewarded đã gắn với prompt mở tập khóa ở Detail/Player. Thêm interstitial ở player_exit_to_previous_screen khi Back sau 30 giây. App-open `splash_app_open` preload trong Splash cho người dùng đã hoàn tất onboarding; khi Splash và dữ liệu sẵn sàng, app thử hiển thị một lần lúc Activity đang RESUMED. Đóng/lỗi/không sẵn sàng/bị tắt thì vào Home, không kéo dài Splash để đợi quảng cáo. Không gắn quảng cáo vào mọi lần ON_START để tránh bật khi đang xem phim hoặc quay lại từ quảng cáo khác.

Các fallback ID là demo ID chính thức của Google. Không copy production ID từ project tham chiếu. AdMob application ID vẫn cấu hình qua AndroidManifest. Chưa thay đổi parameter trên Firebase Console của chủ project; kiểm tra hiện tại dùng packaged defaults và SDK test ads.

Resume: `resume_app_open` preload sau onboarding, chỉ thử hiển thị khi Activity quay lại RESUMED sau ít nhất 30 giây ở nền. Không áp dụng ở Splash/onboarding/Playback, khi đang mở unlock dialog hoặc khi quay lại từ quảng cáo fullscreen khác. Hook bỏ qua lần lifecycle đồng bộ đầu tiên; quảng cáo chưa sẵn sàng không hiển thị muộn. Firebase override `app_open_ad_places` phải chứa cả placement splash và resume nếu muốn bật cả hai.
