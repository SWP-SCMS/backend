# Chạy backend sau cleanup

Dùng Java 21, Maven wrapper trong repository và PostgreSQL riêng. Không dùng database production để thử refactor. Mở terminal tại thư mục clone.

## PostgreSQL demo

Mở Docker Desktop rồi chạy PowerShell:

```powershell
java -version
$demoDbPassword = [guid]::NewGuid().ToString('N')
docker run --name scms-cleanup-local --rm -d -p 127.0.0.1:55433:5432 -e POSTGRES_DB=scms_cleanup -e POSTGRES_USER=scms_cleanup -e "POSTGRES_PASSWORD=$demoDbPassword" postgres:17
docker exec scms-cleanup-local pg_isready -U scms_cleanup -d scms_cleanup
```

Chờ `pg_isready` báo nhận kết nối. Nếu tên container/cổng đã dùng, chọn tên/cổng khác và sửa DB_URL tương ứng. Database demo trên bị xóa khi dừng container vì dùng `--rm`.

## Backend

Trong cùng terminal để giữ biến mật khẩu vừa tạo:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:DB_URL = 'jdbc:postgresql://127.0.0.1:55433/scms_cleanup'
$env:DB_USERNAME = 'scms_cleanup'
$env:DB_PASSWORD = $demoDbPassword
$env:JWT_SECRET = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
$env:PORT = '8080'
$env:CORS_ALLOWED_ORIGINS = 'http://localhost:5175'
$env:INITIAL_MANAGER_ENABLED = 'false'
$env:EMAIL_VERIFICATION_ENABLED = 'false'
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=-Duser.timezone=UTC"
```

UTC tránh lỗi PostgreSQL container không nhận alias `Asia/Saigon` từ JVM Windows, đồng thời thống nhất với test hiện có.

- Swagger: http://localhost:8080/api/v1/docs
- OpenAPI JSON: http://localhost:8080/api/v1/v3/api-docs
- Prefix nghiệp vụ: `/api/v1`; `v3` trong URL tài liệu là phiên bản OpenAPI.

Database mới chưa có tài khoản. Dùng cơ chế provision Manager đầu tiên trong [README](../../README.md), rồi tắt provisioning sau khi tạo. Frontend cần `VITE_API_BASE_URL=http://localhost:8080/api/v1`; nếu cổng frontend khác 5175, cập nhật CORS_ALLOWED_ORIGINS.

Ctrl+C dừng backend. `docker stop scms-cleanup-local` dừng và xóa database demo vừa tạo.

## Kiểm thử và khôi phục

Docker daemon phải hoạt động. `./mvnw test` (PowerShell: `.\mvnw.cmd test`) chạy test; CI tách unit/web và bốn shard integration. Xem [.github/workflows/ci.yml](../../.github/workflows/ci.yml) và [VALIDATION.md](VALIDATION.md).

Checkpoint ZIP và log local không được commit; [verification](verification/README.md) giải thích bằng chứng công bố. Khi cần đối chiếu, checkout commit/nhánh riêng trong một clone mới để tránh ghi đè thay đổi đang làm. Baseline trước cleanup là `9c08093505df43da557b7fa6ff91478f4bef770d`.
