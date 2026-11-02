# Quyết định triển khai

1. Domain Kotlin thuần, constructor injection và composition; chưa dùng Hilt/Room. SQLite nằm sau repository interface, có migration schema v1 -> v2.
2. Local-first metadata giúp mở app offline; dữ liệu mạng cập nhật snapshot cold launch kế tiếp. API chưa có ngày phát hành nên Newest giữ thứ tự catalogue.
3. Khóa theo isFree, số và thứ tự tập theo actual episodes; không có unlock flow hay rewarded mở khóa. Phim 12485 có toàn bộ 44 tập free; các phim khác có khóa, không dùng giả định tất cả phim khóa sau tập 6.
4. Copy onboarding theo mockup được giữ dù mâu thuẫn với trạng thái khóa trong dữ liệu.
5. Dark tokens lấy đúng HTML; Light tự xây. Không dùng dynamic color.
6. UI hiện có English và bản dịch nhãn Arabic/RTL; 8 lựa chọn locale khác vẫn có UI fallback English. Nội dung/audio chọn theo map API; audio language tách riêng UI language.
7. Like là state trong phiên xem, không gửi backend vì API chỉ đọc. Saved/History/progress lưu bền vững.
8. Không giả lời hứa tự giảm 480p khi API chỉ có 720p. Adaptive quality, lịch notification, URL legal, đầy đủ bản dịch và Baseline Profile chưa hoàn thiện.
