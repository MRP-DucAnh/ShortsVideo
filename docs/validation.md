# Kết quả kiểm chứng — 02/11/2026

| Kiểm tra | Kết quả |
|---|---|
| `:app:assembleDebug` | PASS — APK debug cài/mở được |
| `:app:assembleRelease` | PASS — R8 + resource shrinking, APK unsigned |
| `:core:domain:test` | PASS — 18 tests, 0 failures/errors |
| `:app:lintDebug` | PASS — không có error; còn warning dependency/unused resources |
| `FlowDramaJourneyTest` | PASS — 2 tests trên emulator API 37, chế độ máy bay |
| `PlaybackJourneyTest` | PASS — 1 test trên emulator API 37, nguồn MP4 thật |

Lần hồi quy cuối: offline journey pass 2 tests trong 49,58 giây; player journey pass 1 test trong 36,213 giây. Lint và domain tests chạy lại sau cùng đều pass.

Journey offline kiểm tra fixtures và onboarding -> Home -> Browse/save -> Library/Saved -> Search/no results -> History -> Settings/Language. Player test kiểm tra tập khóa/dialog, episode picker, playback options, đổi tốc độ, checkpoint và Resume sau Activity recreation.

Ngoài UI assertion, đọc SQLite sau khi process kiểm thử kết thúc xác nhận show 12692 có position 22332 ms và duration 122974 ms. Đây là vị trí/thời lượng thực từ player, không phải dữ liệu dựng cho screenshot.

## Môi trường và giới hạn

- Emulator Android API 37, minSdk app 24. Chưa xác minh trên thiết bị thật hoặc API 24.
- Espresso 3.8.0-alpha03 / Runner 1.8.0-alpha03 dùng riêng cho test vì bản cũ lỗi reflection InputManager trên API 37.
- Codec c2.goldfish.h264.decoder của emulator lỗi queueInputBuffer; đã xác minh video phát được với software codec. Thiết bị thật vẫn giữ lựa chọn decoder mặc định của Media3.
- Chưa chạy benchmark/Baseline Profile riêng, chưa so pixel toàn bộ mockup.
- Chưa publish/thử kill switch từ Firebase Console thật. Unit test policy không thay thế phép thử round-trip đó.
- Không suy luận những phần chưa thử đã đạt chỉ vì build pass.

## Chạy lại instrumentation

Build app/test APK và cài vào emulator. Journey onboarding cần app data sạch và chế độ máy bay để kiểm tra ads/network failure. Sau đó bật mạng lại và chạy PlaybackJourneyTest. Test player reset progress của show 12692 và cần Internet; không phải unit test kín.

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# Chỉ chạy trên emulator kiểm thử: pm clear xóa dữ liệu của app.
adb shell pm clear com.shorts.flowdrama
adb shell cmd connectivity airplane-mode enable
adb shell svc wifi disable
adb shell svc data disable
adb shell am instrument -w -e class com.shorts.flowdrama.FlowDramaJourneyTest com.shorts.flowdrama.test/androidx.test.runner.AndroidJUnitRunner
adb shell cmd connectivity airplane-mode disable
adb shell svc wifi enable
adb shell svc data enable
adb shell am instrument -w -e class com.shorts.flowdrama.PlaybackJourneyTest com.shorts.flowdrama.test/androidx.test.runner.AndroidJUnitRunner
```

Screenshots: `/sdcard/Android/data/com.shorts.flowdrama/files/screenshots/`. Việc khôi phục sau process kill đột ngột có thể mất tối đa khoảng chu kỳ checkpoint 5 giây; pause/thoát màn ghi checkpoint ngay. Chưa có test tự động kill process ở mọi thời điểm.
