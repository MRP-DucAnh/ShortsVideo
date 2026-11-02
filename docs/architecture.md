# Kiến trúc FlowDrama

## Ranh giới

- `core:domain`: Kotlin/JVM thuần. Model, repository interface và các policy có thể kiểm thử không cần Android.
- `core:data`: parser API, OkHttp, metadata cache, SQLite và DataStore. Feature không biết DTO, SQL hoặc HTTP.
- `core:designsystem`: palette Sunset Flow, gradient, typography, khoảng cách, các primitive và component dùng chung. Không biết Firebase hay Activity.
- `core:player`: Media3, lifecycle, video cache 64 MiB, preload 1 MiB đầu video kế tiếp. Player được giải phóng khi rời composition; không giữ Activity trong ViewModel.
- `core:ads`: Firebase Remote Config và các adapter AdMob; policy cho phép quảng cáo nằm ở domain.
- `feature:*`: UI Compose theo luồng sản phẩm. `Route/Screen` nhận state và callback; lớp ViewModel điều phối tác vụ và persistence.
- `app`: composition root tạo dependency bằng constructor injection, Navigation 3 và shell 5 tab.

Chiều phụ thuộc: `app -> feature -> domain`, `data -> domain`. Feature không phụ thuộc `core:data`. Các adapter SDK phụ thuộc contract/policy domain. Không có service locator trong feature và không có BaseViewModel chứa hành vi không liên quan.

## OOP

Dùng interface tại ranh giới có implementation thay thế thực tế: repository dữ liệu và preferences. Dùng composition cho resolver, policy và UI. Không tạo interface cho mọi class, không kế thừa để chia sẻ một vài dòng code.

Bản đầu sử dụng constructor injection thay Hilt và SQLiteOpenHelper thay Room. Các lựa chọn này giảm dependency xử lý annotation và giữ code persistence gọn. Đây là thay đổi so với đề xuất ban đầu; repository contract cho phép thay bằng Hilt/Room mà không đổi feature. Các module network/database/datastore/remoteconfig nhỏ được gom vào adapter hiện tại, chưa tạo hàng loạt module rỗng.

## State

- Preferences lưu DataStore: ngôn ngữ, theo máy, onboarding, mood/tag IDs, theme, Wi-Fi preview, speed và quality.
- SQLite lưu bookmark theo show và progress gồm số tập, vị trí ms, thời lượng, thời điểm cập nhật.
- StateFlow là nguồn state của ViewModel. UI collect theo lifecycle.
- Navigation 3 lưu serializable NavKey; mỗi tab có SaveableStateProvider riêng để giữ state cuộn và filter khi đổi tab.
- Tiến độ player đọc trong component timeline riêng. Ghi định kỳ 5 giây, checkpoint khi dừng/rời player. Process bị kill đột ngột có thể mất tối đa khoảng chu kỳ checkpoint.
- History hiện giữ tập gần nhất cho mỗi show; chưa có bảng đánh dấu tất cả tập đã xem.

## Dữ liệu

Startup đọc metadata cache hoặc fixtures nhúng sẵn để không chờ HTTP/Firebase/ads. Một resource cũ hơn 6 giờ được refresh tối đa một lần mỗi process. Snapshot mạng đã parse thành công được lưu và dùng từ lần cold launch kế tiếp, tránh thay grid trong lúc người dùng đang thao tác. Video vẫn stream từ URL thật; fixtures không phải video offline.

Title/synopsis/hook: ngôn ngữ chọn -> mã ngôn ngữ cơ sở -> ngôn ngữ fallback -> en -> entry không rỗng đầu tiên. Resolver cho phép caller truyền defaultLanguage; các component UI mặc định dùng en theo catalogue hiện tại. Sources dùng cùng thứ tự ngôn ngữ, giữ quality key mở. Với Auto hoặc quality thiếu, chọn rendition có độ phân giải số lớn nhất hiện có. Đây là chọn rendition ban đầu, không phải adaptive bitrate cho MP4.

Home rail dùng `isCategory=false`; Browse dùng `true`; đều sort `orderPriority`. Hero lookup theo ID. Grid tập dùng mảng episode thực tế và giữ thứ tự; không tạo tập giả từ episodeCount. Đếm lượt dùng Long. Status giữ String mở.

## Hiệu năng và hướng dẫn đã đọc

- https://github.com/android/skills/tree/main/jetpack-compose/jetpack-compose-performance
- https://github.com/android/skills/tree/main/navigation/navigation-3
- https://github.com/android/skills/tree/main/system/edge-to-edge

Lazy lists có key/contentType, không collect Flow trong item, không gắn Stable/Immutable hàng loạt. Player chỉ thuộc màn đang hiển thị. Preload có giới hạn và có hủy. API/SQL chạy ngoài main. Animation skeleton chưa triển khai nên không có shimmer ảnh hưởng Reduce Motion.

R8/resource shrink bật ở release. Chưa tạo Baseline Profile và chưa đo jank/macrobenchmark; không tuyên bố đã tối ưu hiệu năng bằng số liệu.

## Merchandising rails

Fixtures không gắn trending/popular/newest vào show.tagIds. Vì vậy không filter shows theo các ID merchandising. BuildHomeRails tạo section từ tags isCategory=false, theo orderPriority; Trending/Popular dùng viewCount làm proxy, Newest và rail lạ giữ thứ tự catalogue vì API không có ngày phát hành. Đây là heuristic cục bộ được ghi rõ, không phải bảng xếp hạng hay thời gian phát hành từ backend.

Finished trong History chỉ đúng khi tập cuối của mảng actual episodes đã xem >=95%. SQLite schema v2 lưu is_last_episode, có migration từ v1. Không suy luận tập cuối từ episodeCount của catalogue.

## Decoder trên emulator

Log thực tế trên emulator API 37 ghi lỗi queueInputBuffer của c2.goldfish.h264.decoder khi đọc H.264. Player ưu tiên codec software khi Build.HARDWARE là goldfish/ranchu; thiết bị thật giữ lựa chọn hardware-first của Media3. Decoder fallback được bật. Đây là tương thích môi trường kiểm thử, không phải kết quả benchmark hiệu năng.
