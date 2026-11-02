# Verification và review

Kiểm chứng trên macOS, Java 21, Node 24.16.0, Yarn 1.22.22, Docker 29.5.2; ngày 2026-11-02. Không dùng cloud credentials, không commit/push/deploy hoặc chạy Strix.

| Phần                          | Kết quả quan sát                                                                                                                 |
| ----------------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| Backend `./mvnw clean verify` | **22 tests pass**, không skip; Testcontainers MySQL 8.4 thật, database độc lập                                                   |
| JaCoCo gate                   | **96.10% line (789/821), 73.89% branch (167/226)** trong các module mới; đạt ngưỡng 80% / 70%                                    |
| Frontend                      | `yarn lint`, `yarn build`, **5 Vitest/RTL tests pass**, Prettier check pass                                                      |
| Simulator                     | typecheck/build pass; **6 tests pass**, gồm Java Nimbus → jose, rotation và SYNC_DEVICE                                          |
| E2E                           | **4 Playwright scenarios pass**, database riêng 3308, backend8082, Vite3040                                                      |
| Dependency reproducibility    | Yarn frozen-lockfile install pass cho frontend và simulator                                                                      |
| Docker                        | Build frontend/backend/simulator từ Dockerfiles thành công; MySQL healthy; backend `/actuator/health` UP; nginx phục vụ console  |
| Docker simulator              | `enroll`, `run --once`, `status` thành công, giữ identity trong volume                                                           |
| Browser QA                    | Dashboard Docker8081 ở 1440/768/390px: không page JS error, không unexpected HTTP error; không page horizontal overflow          |
| Playwright MCP                | Kiểm tra login sai credential ở 390px: hiển thị error, không overflow; 401 refresh khi chưa login và 401 sai password là dự kiến |
| Taste                         | Source lint 0 FAIL/0 WARN; production login inspector 0 FAIL/1 WARN (khoảng trống có chủ ý của form đăng nhập)                   |
| Git diff                      | `git diff --check` pass; secrets, screenshots, simulator state và generated keys bị ignore                                       |

## Acceptance scenarios đã kiểm chứng

- CSRF missing →403 JSON; login thật, rotating refresh, reuse thu hồi family, logout mất quyền ngay.
- Hai refresh đồng thời: một200, một401; access session bị thu hồi. Hai tab UI reload đồng thời vẫn dùng refresh tuần tự; logout đồng bộ sang tab còn lại.
- ORG_ADMIN / OPERATOR / VIEWER và tenant isolation cho device read/write, assignment/version, command và audit.
- Hai enrollment cùng token chỉ một thành công; expired/revoked token bị từ chối. Duplicate UID rollback token consumption; enrollment mới bằng token đó còn dùng được.
- Cố ý làm final audit insert thất bại: không để lại device/command, token enrollment vẫn dùng được.
- Hai claim đồng thời chỉ một lease; FIFO vẫn đúng khi clock/timestamp trùng; lease retry tăng attempt; stale result409, same accepted result idempotent; TTL chưa giaoEXPIRED, retries cạnUNKNOWN; revoke phân biệtCANCELLED/UNKNOWN.
- Policy version bất biến, assignment revision tăng khi assign/unassign, stale/no-assignment report409; compliance tính từ inventory; offlineUNKNOWN; simulator không tự nâng OS.
- ES256 command hợp lệ và hai verification keys qua rotation; sửa payload, wrong tenant/device/issuer/typ/key/algorithm/expiry/ID đều bị từ chối.
- State persistence, file permissions0600, process lock và dedup message; crash sau persist trước result, restart nhận attempt mới nhưng message không lặp.
- UI toàn luồng login → enrollment → simulator heartbeat → create/assign policy → signed SHOW_MESSAGE → SUCCEEDED → audit → reload/refresh → logout.

## Review và các sửa lỗi đáng chú ý

Áp dụng secure-mdm-dev, Ponytail, React Best Practices, Composition Patterns, Taste, Web Design Guidelines và Code Review Expert. Review tập trung authorization/tenant, transaction boundaries, idempotency, refresh concurrency, signature trust, crash recovery và UI states.

- Serialize namespace idempotency theo user trước device lock để cùng key trên hai device không gặp unique-constraint race.
- Thêm sequence FIFO vì timestamp + UUID không biểu diễn insertion order khi timestamp trùng.
- Refresh reuse revocation commit trước401; chặn tự retry sau kết quả mạng không xác định.
- So sánh EC thumbprint để hai file chứa cùng key nhưng khác `kid` không vượt qua key-separation check.
- Kiểm tra command ID của envelope transport khớp signed jti.
- SYNC_DEVICE thực sự đồng bộ policy/inventory ngay, kể cả giữa hai heartbeat.
- Chỉnh generic return types cho cả javac và compiler IDE; build clean không phụ thuộc class do IDE sinh.
- Audit mobile chuyển sang description list, đủ trường và không bẻ UUID thành từng ký tự; thêm skip navigation, label forms, role errors, contrast tốt hơn và semantic links/buttons.
- UI mới dùng MUI direct imports, lazy routes, Query cache invalidation; không thêm state management framework hay component abstraction không cần thiết.

Direction Taste: **Framed sheet**, seed760839080, giữ palette/type MUI của repo. Câu hỏi chính của overview là thiết bị nào đang kết nối và command nào cần theo dõi. Data đều lấy API; không có KPI giả. Screenshot review đã kiểm tra cả trạng thái trống và dữ liệu thực, desktop/tablet/mobile. Khung trắng và navigation trung tính được giữ; status màu phục vụ dữ liệu.

## Giới hạn bằng chứng

CI workflow đã tạo tại `.github/workflows/verify.yml`; các lệnh tương ứng đã chạy local, **chưa chạy trên GitHub runner** vì không push. Playwright headless Chromium là browser được kiểm tra; chưa kiểm thử Safari/Firefox. Vite có warning bundle chính ~574kB (~181kB gzip); build vẫn thành công, các màn nghiệp vụ được lazy-load. Không có claim pentest/certification, hardware attestation hoặc MDM trên thiết bị thật.

Artifacts tái tạo: `be/target/surefire-reports`, `be/target/site/jacoco`, `fe/playwright-report`, `fe/test-results`. Screenshot và log QA local nằm trong `.local`, không đưa vào git; trace test có thể chứa credential test tạm thời. Demo Docker và một simulator đã đăng ký được giữ chạy ở loopback8081 để có thể mở kiểm tra trực tiếp. Database E2E tạm đã được dừng sau kiểm thử.
