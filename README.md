# FlowDrama — Short Drama Series

Ứng dụng Kotlin + Jetpack Compose, minSdk 24. Bản triển khai đầu dựa trên `flowdrama-android-mockup.html` của đề, dùng đúng palette Sunset Flow. Không dùng `flowdrama-concept-mockup.html`.

## Mở và build

Mở project bằng Android Studio với SDK 37 và JDK phù hợp Gradle wrapper (daemon toolchain 25 được cấu hình sẵn). `app/google-services.json` của chủ dự án đã có và package khớp `com.shorts.flowdrama`. Gradle tải dependencies qua Internet; không có bước sửa source thủ công. JDK 17 dùng cho bytecode Kotlin/JVM của domain, Gradle có thể tự resolve toolchain.

```powershell
# Nếu terminal chưa có Java, trỏ JAVA_HOME đến JBR Android Studio của máy.
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :core:domain:test
.\gradlew.bat :app:assembleDebugAndroidTest
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`. Không có ad unit production. Metadata fixtures được bundle nên lần mở đầu không phụ thuộc mạng; video vẫn cần Internet nếu chưa cache.

## Cấu trúc và mức hoàn thiện

- `app`: composition root, ViewModel ứng dụng và Navigation 3.
- `core/{domain,data,designsystem,player,ads}`: domain thuần Kotlin và các adapter Android độc lập.
- `feature/{onboarding,home,search,browse,reels,detail,playback,library,settings}`: các luồng UI.

Xem [kiến trúc](docs/architecture.md), [ma trận đầy đủ 23 mục UI](docs/screen-matrix.md), [ads và cách test Remote Config](docs/ads-and-remote-config.md), [thư viện/giấy phép](docs/licenses.md).

Các luồng chính đã có code và build được; không tuyên bố 23 mục đều đã polish hoặc kiểm chứng toàn bộ. Chưa có lịch notification thật, URL legal chính thức, Baseline Profile, kiểm thử benchmark, full watched-episode tracking và adaptive quality khi buffering. Light theme là bộ màu tự xây do mockup thiếu token.

## Dữ liệu và state

API đúng base URL của đề. Parser giữ localization/source maps mở, Long counters và status lạ. Tag rail/category tách biệt, hero lookup theo ID. Grid tin danh sách tập thực tế, giữ thứ tự mảng, khóa theo isFree. Tập title rỗng vẫn hiển thị số. Cache metadata có refresh background giới hạn; tránh poll host. Chi tiết fallback ở tài liệu kiến trúc.

History/Saved/progress lưu SQLite; preferences lưu DataStore. Riêng lựa chọn khung giờ nhắc xem và cờ cho phép nhắc xem lưu SharedPreferences. Progress checkpoint định kỳ và khi lifecycle dừng. Arabic có RTL và bản dịch các nhãn chính; các chuỗi mô tả dài/metadata còn fallback English. Onboarding đã có đầy đủ 58 resources ở cả 10 locale theo thiết kế, có Arabic RTL. Ngoài onboarding, tám locale ngoài en/ar vẫn còn nhãn UI fallback English; nội dung dùng bản dịch API nếu có. Cần hoàn thiện LQA/bản dịch trước khi coi đa ngôn ngữ là hoàn tất.

Luồng khóa chỉ hiển thị trạng thái và dialog giải thích, không có coin, paywall hoặc cơ chế mở khóa.

## Quảng cáo / Firebase

Module ads Compose hỗ trợ banner, native (19 templates), interstitial, rewarded, rewarded-interstitial và app-open. ID fallback nằm ở `core/ads/src/main/assets/ads/ads_ids_default.json`; các parameter Firebase mặc định ở `app/src/main/res/xml/remote_config_defaults.xml`. Remote Config `ad_unit_ids` ghi đè ID; các nhóm placement chọn template/style/enable. Kill switch global và exact-version thắng placement. Xem [hướng dẫn gắn ads và cấu hình](core/ads/README.md).

Firebase Console chưa được chỉnh/publish bởi assistant. Round-trip đổi cấu hình thật vẫn cần xác minh, dù policy đã có unit tests.

## Kiểm chứng và AI

Đã dùng Codex để phân tích đề, nghiên cứu android/skills, xây dựng code và kiểm thử. Những quyết định khác thiết kế/đề được ghi trong tài liệu thay vì coi là spec gốc. Thời gian: đợt triển khai đầu trong phiên làm việc ngày 02/11/2026; chưa có số liệu tổng giờ phát triển đáng tin cậy.

Unit tests tập trung kill switch, exact version, fallback ngôn ngữ/quality, Long và Day Cycle. Instrumentation tests có fixtures edge cases và journey onboarding/discovery/library/settings; test player riêng yêu cầu Internet và onboarding đã hoàn tất. Kết quả chạy thực tế được cập nhật ở [validation](docs/validation.md).
