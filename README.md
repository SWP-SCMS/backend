# SCMS Backend

Backend API của Sports Center Management System. Frontend chỉ gọi Spring Boot API dưới prefix
`/api/v1`; frontend không truy vấn Supabase trực tiếp và SCMS không dùng Supabase Auth.

## Yêu cầu

- Java 21
- Docker Desktop hoặc Docker Engine đang chạy khi chạy test
- PostgreSQL cho môi trường local/production

## Cấu hình

Ứng dụng có bốn lớp cấu hình:

- `application.properties`: cấu hình chung, UTC, Flyway và `ddl-auto=validate`.
- `local`: kết nối PostgreSQL local và chỉ profile này import tùy chọn file `.env` ở repository root.
- `test`: datasource được PostgreSQL 17 Testcontainers cung cấp; không đọc `.env`.
- `prod`: chỉ nhận cấu hình từ environment variables của môi trường triển khai.

Các biến môi trường:

| Biến | Mục đích |
| --- | --- |
| `DB_URL` | JDBC URL của PostgreSQL |
| `DB_USERNAME` | Tài khoản database |
| `DB_PASSWORD` | Mật khẩu database |
| `PORT` | Cổng HTTP, mặc định `8080` |
| `CORS_ALLOWED_ORIGINS` | Danh sách origin phân cách bằng dấu phẩy |
| `JWT_SECRET` | Secret ký JWT HS256, bắt buộc có ít nhất 32 byte |

Spring Boot không tự đọc `.env` chỉ vì file tồn tại. Profile `local` chủ động dùng Spring Config Data để
import `optional:file:.env[.properties]`. Tạo file local từ `.env.example`, thay toàn bộ placeholder, rồi chạy:

```powershell
Copy-Item .env.example .env
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run
```

Hoặc đặt trực tiếp các environment variable tương ứng thay vì dùng `.env`. Không commit `.env` hoặc bất kỳ
secret thật nào. Khi triển khai, đặt `SPRING_PROFILES_ACTIVE=prod`; profile production không import `.env`.

## Authentication

Contract US01 được chốt tại `src/main/resources/openapi/us01-authentication.yaml`:

- `POST /api/v1/auth/login`: nhận `identifier` (email hoặc số điện thoại canonical) và `password`, trả access token
  cùng thông tin account tối thiểu. Email được trim/lowercase; số điện thoại được trim trước khi lookup.
- `POST /api/v1/auth/refresh`: đọc và rotate refresh token từ cookie.
- `POST /api/v1/auth/logout`: thu hồi phiên hiện tại và luôn xóa cookie; endpoint có tính idempotent.

Access token là JWT có thời hạn 15 phút và được gửi bằng `Authorization: Bearer <token>`. Mỗi request được bảo vệ
xác minh chữ ký/thời hạn JWT, sau đó kiểm tra Account từ claim `sub` vẫn tồn tại và `ACTIVE`; access token đã phát
hành bị từ chối ngay khi Account chuyển sang `SUSPENDED` hoặc `INACTIVE`. Refresh token có thời hạn 7 ngày, chỉ xuất
hiện trong cookie `refresh_token` với `HttpOnly`, `Path=/api/v1/auth`, `SameSite=Lax`; `Secure=false` ở local/test và
`Secure=true` ở production. Chỉ account `ACTIVE` có thể login hoặc refresh.

## Kiểm thử

Đảm bảo Docker daemon đang chạy, sau đó:

```powershell
.\mvnw.cmd test
```

Test khởi động PostgreSQL 17 sạch, chạy Flyway V1–V6, để Hibernate validate schema, và kiểm tra persistence,
authentication, refresh-token rotation, cookie policy, security/CORS cùng ProblemDetail error handling. Test không
kết nối Supabase.

## Ghi chú triển khai bắt buộc

- Database production là PostgreSQL trên Supabase, nhưng chỉ backend được phép truy cập database.
- Cơ chế provision tài khoản Manager đầu tiên phải được quyết định trước deployment. Foundation không seed
  username hoặc password mặc định.
- Quyền hiện tại của role Supabase `anon` và `authenticated` đang quá rộng, đồng thời RLS đang tắt. Việc thu
  hẹp quyền và bật/chốt chính sách RLS là security follow-up bắt buộc trước deployment; foundation này không
  thay đổi quyền Supabase và không thêm migration riêng cho provider.
