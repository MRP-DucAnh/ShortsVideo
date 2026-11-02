# Ma trận 23 mục UI

Đã có implementation cho các mục dưới đây; mức độ hoàn thiện khác nhau. File HTML được cung cấp là chuẩn bố cục/màu, không dùng concept-mockup.

| Mã | Implementation | Mức hiện tại / giới hạn |
|---|---|---|
| SCR-0001 | SplashScreen | Logo trên wordmark, tracking 6sp, glow cam 14%, badge bo tròn; thoát khi metadata/preferences sẵn sàng |
| SCR-0002 | OnboardingScreen1Language | 10 lựa chọn, item 48dp bo tròn/viền, selected cam/chữ đen, gradient dọc, lưu DataStore |
| SCR-0003 | OnboardingScreen2ValueProposition | Text/pills thu gọn, progress trên CTA; ảnh nền tạm ẩn theo yêu cầu |
| SCR-0004 | OnboardingScreen3TastePicker | Emoji đúng mockup, press/selection animation, multi-select và de-select, FINE-TUNE đủ genre, Skip dưới Continue |
| SCR-0005 | OnboardingScreen4Notifications | Chọn/bỏ chọn độc lập 3 khung giờ; SharedPreferences lưu lựa chọn và cờ enabled; chưa có lịch push/reminder |
| SCR-0101 | HomeScreen | Day Cycle, hero theo ID, hai daypart cards, rails và Continue watching |
| SCR-0201 | SearchScreen | Debounce 300 ms; rỗng, gợi ý genre, kết quả và không kết quả |
| SCR-0202 | BrowseScreen | Dock preview muted, grid 4 cột, filter không mất selected preview, Wi-Fi toggle |
| SCR-0203 | ReelsScreen | Pager dọc, player trang hiện tại, preload next 1 MiB, shortcut speed |
| SCR-0301 | DetailScreen | Metadata, synopsis mở rộng, Save/Resume, actual episode grid và khóa |
| SCR-0302 | PlaybackScreen | MP4 thật, seek, speed, pause/resume, auto-next tập free, checkpoint |
| SCR-0303 | EpisodeGrid trong sheet | Grid 5 cột, đang phát/khóa; chưa lưu watched-state cho mọi tập |
| SCR-0304 | Playback options sheet | Speed, các quality/language có trong source; không tạo quality giả |
| SCR-0401 | LibraryScreen | History/Saved counters, recent và banner khi có fill |
| SCR-0402 | HistoryScreen | Watching/Finished, xóa một/xóa hết có confirm, persistence |
| SCR-0403 | SavedScreen | Bookmark cấp show, lọc status, bỏ lưu; API chưa cung cấp dấu fresh episode |
| SCR-0501 | SettingsScreen | Language, Dark/Light, Wi-Fi preview, policies/version; Light tự xây |
| SCR-0502 | SettingsLanguageScreen | Theo máy hoặc chọn, cập nhật tức thì, Arabic RTL |
| SCR-0601 | LoadingState | Skeleton tĩnh, chưa shimmer; metadata local-first thường chuyển rất nhanh |
| SCR-0602 | SavedScreen empty | Empty state và CTA sang Browse |
| SCR-0603 | BlankState network error | Retry khi metadata không đọc được; bundled fixtures cho phép dùng offline |
| SCR-0604 | VideoSurface | Spinner/error/retry thật; chưa tự hạ rendition khi buffering; không giả claim đã về 480p |
| SCR-0605 | SnackbarHost | Save/remove feedback màu success; thời lượng Material Short, chưa ép đúng 2 giây |

SCR-0305 không triển khai. Tap tập khóa hiển thị dialog giải thích; đây là quyết định bổ sung, không gắn rewarded mở khóa.

Giao diện đang là bản đầu có thể chạy, chưa xác nhận pixel parity toàn bộ 23 mục. Bộ màu dark lấy đúng token HTML. Artwork thật thay placeholders; poster lỗi dùng nền dự phòng. Chưa có bộ screenshot comparison tự động.

Copy onboarding “Every episode open” được giữ theo mockup, dù fixtures có tập khóa. Đây là mâu thuẫn sản phẩm của đầu vào; app vẫn tuân thủ isFree và không phát tập khóa.

Wi-Fi preview hiện kiểm tra tại thời điểm composition; chưa có observer chuyên biệt để dừng preview ngay khi thiết bị đổi mạng. Đây là phần cần hoàn thiện cùng kiểm thử lifecycle/network nâng cao.
