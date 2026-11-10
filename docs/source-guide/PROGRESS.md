# Tiến trình thực tế

Baseline: `9c08093505df43da557b7fa6ff91478f4bef770d`.

| Giai đoạn | Trạng thái | Bằng chứng |
|---|---|---|
| G0 bản sao/manifest | Đạt | 1.114 file, SHA-256 lưu ngoài bản sao; không có .git/.env |
| G0 unit/web | Đạt | 1.065 test, 0 failure/error/skip; baseline-copy-unit.log |
| G0 integration | Đạt | 104 lớp, 665 test; bốn shard 139/147/238/141; 0 failure/error/skip |
| G1 index và movement map | Đạt coverage API | 74 file/75 controller classes, 331 mapping, 330 HTTP operation; khớp OpenAPI sinh thực tế |
| G2 scheduling | Đạt | Format 254 test; sau move 1.066 unit/web (gồm export contract), 74 integration; 0 OpenAPI diff, 2.896 method body không đổi; checkpoint G2-scheduling |
| G2 training | Đạt | Format 185 test; sau move 1.067 unit/web và 33 integration; 2.896 method không đổi; checkpoint G2-training |
| G2 membership | Đạt | Format 251 test; 1.067 unit/web và 186 integration đạt; 2.896 method không đổi; checkpoint G2-membership |
| G2 course | Đạt | Format 152 test; 1.067 unit/web và 173 integration đạt; 2.896 method không đổi; checkpoint G2-course |
| G2 emailverification | Đạt | 1.067 unit/web và 14 integration đạt; checkpoint G2-emailverification |
| G2 toàn bộ/format | Đạt | 64 wildcard import được thay; format 185 file, 2.896 method giữ nguyên; 1.067 unit/web đạt; checkpoint G2-all |
| G3 scheduling | Đạt | 4 helper, 28 method body/return type/throws giữ nguyên; 1.067 unit/web + 74 integration đạt; checkpoint G3-scheduling |
| G3 course management | Đạt | 47 method đối chiếu lúc extraction; sau đó rút ngắn 3 delegate snapshot nội bộ; 1.067 unit/web + 173 integration đạt; checkpoint G3-course-management |
| G3 payment | Đạt | 27 method giữ nguyên; 1.067 unit/web + 112 integration liên quan đạt; checkpoint G3-payment |
| G3 withdrawal | Đạt | 15 method giữ nguyên; 1.067 unit/web + 82 integration liên quan đạt; checkpoint G3-withdrawal |
| G3 transfer | Đạt | 29 method giữ nguyên; 1.067 unit/web + 147 integration liên quan đạt; checkpoint G3-transfer |
| G3 refund | Đạt | 21 method đối chiếu; 1.067 unit/web + 108 integration liên quan đạt; checkpoint G3-refund |
| G3 schedule change | Đạt | 13 method đối chiếu; 1.067 unit/web + 100 integration liên quan đạt; checkpoint G3-schedulechange |
| G4 scheduling | Đạt | SQL và mapper chuyển sang repository; 1.067 unit/web + 74 integration đạt; checkpoint G4-scheduling |
| G4 training | Đạt | 1.067 unit/web + 33 integration đạt; checkpoint G4-training |
| G4 membership | Đạt | 76 lời gọi SQL/mapper/binding đối chiếu; 1.067 unit/web + 186 integration domain + 60 integration liên miền đạt; checkpoint G4-membership |
| G4 batch rollback characterization | Đạt | Test PostgreSQL mới đạt trên cả source gốc và bản refactor; checkpoint G4-batchproof |
| G4 course | Đạt | 207 lời gọi SQL/mapper/binding đối chiếu; 1.067 unit/web + 173 integration domain + 78 integration liên miền; checkpoint G4-course |
| G4 emailverification | Đạt | 68 lời gọi SQL đối chiếu; 1.067 unit/web + 14 integration domain + 10 authentication; checkpoint G4-emailverification |
| G4 identity/manager | Đạt | 6 lời gọi SQL đối chiếu; 1.067 unit/web + 108 integration identity + 6 manager; checkpoint G4-identity-manager |
| G5 security/error | Đạt | 135 matcher giữ thứ tự, 63 handler chuyển dựng response sang 7 helper; toàn bộ gate cuối đạt; checkpoint G5-final-gates |
| G6 tài liệu | Đạt kiểm tra tự động | API/class/dòng, demo lễ tân và persistence đã đồng bộ; liên kết/symbol hợp lệ; 10/10 câu tra cứu tự động tìm đúng controller/service |
| Gate cuối | Đạt | 1.067 unit/web + 666 integration, 0 failure/error/skip; HTTP full context/OpenAPI khớp baseline; hash/HEAD/Git bản gốc nguyên trạng |
| Bản chốt local | Đạt | checkpoint-FINAL.zip và manifest SHA-256; không deploy; frontend và diễn tập của người dùng cần kiểm tra riêng |

Evidence nằm ở `local-evidence`. Bản gốc `backend-latest` giữ nguyên. Checkpoint gốc: checkpoint-00-baseline.zip. Tổng baseline: 1.730 test, không có failure/error/skip. Shard 0 có thông báo Surefire cưỡng bức kết thúc JVM sau thời gian chờ thoát, dù test và Maven exit code đều thành công; giữ log để đối chiếu sau refactor.
