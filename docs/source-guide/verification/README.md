# Bằng chứng kiểm chứng cleanup

## Local gates

Gate cuối trên source từ checkpoint FINAL: 1.067 unit/web và 666 integration, tổng 1.733 test; không failure/error/skip. Bốn shard integration có 176/119/143/228 test. Xem [kết quả](../test-results.json), [cấu trúc](structure-verification.json) và [hash source đã chạy test](tested-input-sha256.json).

Log thô và checkpoint ZIP được giữ ngoài Git trên máy triển khai cleanup. Trường `log` trong JSON là tên bằng chứng local, không phải file được commit. Không đưa dữ liệu runtime, cấu hình database, tài khoản demo, hoặc log chứa payload vào repository.

CI của Pull Request chạy lại unit/web và bốn shard PostgreSQL Testcontainers trên GitHub. Kết quả CI phải được đọc trên PR; báo cáo local không thay thế trạng thái CI.

Manifest SHA-256 phản ánh byte của source local đã kiểm chứng; Git có thể chuẩn hóa LF/CRLF theo cấu hình checkout. Ngoài chuẩn hóa newline, source/test/config của nhánh giữ nguyên checkpoint FINAL.
