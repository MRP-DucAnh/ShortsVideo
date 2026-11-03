# Cặp key–value để tạo trên Firebase

Tất cả parameter dùng kiểu **String**, value là nguyên nội dung file JSON cùng tên trong thư mục này. Không dán wrapper `parameters` của file import vào một parameter.

| Key | File chứa value |
| --- | --- |
| ads_config | ads_config.json |
| ad_unit_ids | ad_unit_ids.json |
| banner_native_ad_places | banner_native_ad_places.json |
| banner_native_ad_places_2 | banner_native_ad_places_2.json |
| rewarded_and_inter_ad_places | rewarded_and_inter_ad_places.json |
| app_open_ad_places | app_open_ad_places.json |
| banner_ad_config | banner_ad_config.json |
| native_ad_config | native_ad_config.json |
| interstitial_ad_config | interstitial_ad_config.json |
| rewarded_ad_config | rewarded_ad_config.json |
| rewarded_interstitial_ad_config | rewarded_interstitial_ad_config.json |
| app_open_ad_config | app_open_ad_config.json |

`banner_native_ad_places_2` để [] mặc định. Nếu không rỗng, danh sách này thay toàn bộ danh sách chính, giống Logo Maker.

Place name mô tả vị trí, không chứa định dạng banner/native để có thể đổi type:

| Place name | Điểm đặt |
| --- | --- |
| home_feed_after_continue_watching | Home, sau rail Continue watching (hoặc rail chính nếu chưa có lịch sử) |
| library_recently_watched_bottom | Library, dưới danh sách Recently watched |
| onboarding_step4_notifications_complete | Kết thúc Step 4 onboarding |
| detail_episode_unlock_rewarded | Detail: xem rewarded để mở đúng tập đang khóa |
| detail_episode_unlock_rewarded_interstitial | Reserved: rewarded interstitial mở tập trên Detail, chưa gắn UI |
| splash_app_open | Reserved: app-open tại Splash, chưa gắn UI |

Đổi `ad_type` của inline placement giữa native/banner và thay `ad_id` đúng định dạng tương ứng. Đổi màu CTA qua background_cta/cta_text_color/cta_border_color, bo góc qua cta_radius_dp. Các field style áp dụng cho native; banner do SDK dựng creative.

File value dùng demo ad_id trực tiếp. Khi dùng chúng, hãy đổi ID ngay trong object placement; ad_id ưu tiên hơn ad_unit_ids. Nếu muốn chỉ quản lý ID tập trung, xóa ad_id và thêm ad_id_key, ví dụ "native" hoặc "banner", rồi cấu hình key đó trong ad_unit_ids.

Cấu hình đã publish dùng place_name cũ cần thay bằng tên mới ở trên. Giữ toàn bộ các object vẫn cần trong array khi sửa. Không cần tạo placement dự phòng nếu chưa dùng. App-open và placement rewarded-interstitial riêng vẫn là dự phòng.

Hai placement mới cần giữ trong array rewarded_and_inter_ad_places khi publish:

- player_exit_to_previous_screen: interstitial khi Back khỏi Player sau ít nhất 30 giây, không hiện ngay sau một prompt/quảng cáo khác. Không chờ no-fill và có frequency cap.
- player_episode_picker_unlock_rewarded: prompt rewarded mở tập khóa từ danh sách tập hoặc cuối tập trong Player.

Các điểm unlock chỉ chấp nhận rewarded/rewarded_interstitial để không hứa phần thưởng với loại quảng cáo không có earned-reward callback. Thành công lưu showId:number vào DataStore và mở tập đó; bỏ qua hoặc no-fill không mở tập. Nút Retry khi lưu lỗi thử ghi lại, không yêu cầu xem thêm ads.
