# Ads và Firebase Remote Config

Firebase project trong `app/google-services.json` do chủ dự án cung cấp. Package: `com.shorts.flowdrama`. Version hiện tại: `1.0.0`.

## Ba placement

| Key | Loại | Điểm hiển thị |
|---|---|---|
| interstitial_onboarding | Interstitial test | Chỉ lúc kết thúc onboarding, nếu đã có ad; không đợi tải |
| native_home | Native test | Sau rail chính/Continue watching của Home |
| banner_library | Banner test | Trong danh sách Library; chỉ có kích thước khi tải thành công |

App ID và cả ba unit ID đều là ID demo của Google. Chưa dùng rewarded và chưa thêm unlock flow. Không có quảng cáo trong player/Reels/Browse preview. Ad load lỗi bỏ slot, callback show lỗi vẫn chuyển tiếp. NativeAd/AdView được destroy khi rời UI hoặc config thay đổi.

## Cấu hình Console

Tạo parameter **String** tên `ads_config`, value là JSON:

```json
{
  "enable": true,
  "disabled_versions": [],
  "placements": {
    "interstitial_onboarding": { "enable": true },
    "native_home": { "enable": true },
    "banner_library": { "enable": true }
  }
}
```

Thứ tự ưu tiên: `enable` toàn app -> versionName khớp chính xác -> placement -> SDK ready/lifecycle. Placement thiếu bị tắt. Config malformed quay về defaults nhúng sẵn. Lần chạy đầu có defaults xác định; fetch lỗi không chặn splash.

Fetch lúc khởi động và khi Activity vào foreground. Debug `minimumFetchInterval=0`, release 3600 giây; fetch timeout 8 giây. Không poll liên tục. UI quan sát config và gỡ ads khi config bị tắt. Cached activated config tiếp tục dùng nếu offline.

## Test nhanh

1. Publish JSON với `disabled_versions: ["1.0.0"]`, vẫn để cả ba placement true.
2. Đóng/mở app với Internet. Sau fetch thành công, Home/Library không có ad, kết thúc onboarding không hiện interstitial.
3. Xóa version đó, đặt `enable:false`: mọi placement vẫn tắt.
4. Bật app, chỉ tắt `native_home`: Library banner vẫn được phép.
5. Tắt mạng và chạy app mới: metadata fixtures hiển thị, ads lỗi không tạo khung trống, video chưa cache có thông báo thử lại.

Unit test xác nhận thứ tự ưu tiên và so chuỗi version chính xác. Việc publish parameter trên Console của chủ project chưa được thực hiện trong workspace; cần kiểm chứng round-trip Firebase riêng, không suy diễn thành công chỉ từ unit test.

Tài liệu SDK: https://developers.google.com/admob/android/quick-start và https://firebase.google.com/docs/remote-config/get-started?platform=android.
