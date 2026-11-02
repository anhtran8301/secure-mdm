# SecureMDM Backend

Java 21, Spring Boot 4.1.2, WebFlux, R2DBC/MySQL 8.4 và Liquibase. Backend là một deployable duy nhất, tổ chức theo module nghiệp vụ (`identity`, `device`, `policy`, `command`, `audit`, `dashboard`) nằm trong thư mục `securemdm/src`, mỗi module có các layer `controller`, `service` (interface + impl), `repository`, `entity`, `dto`, `mapper` riêng; chỉ `config`, `security`, `exception`, `utils`, `common` dùng chung. Quy ước chi tiết nằm trong [AGENTS.md](AGENTS.md); lý do chọn kiến trúc nằm trong [ADR](../docs/adr/).

Hướng dẫn chạy toàn bộ ứng dụng, tài khoản demo, keys, Docker và E2E nằm trong [README gốc](../README.md). Hợp đồng API và transaction nằm trong [ARCHITECTURE](../docs/ARCHITECTURE.md).

## Kiểm thử

```sh
./mvnw verify
```

Cần Java 21 và Docker đang chạy. Tests tự tạo MySQL Testcontainers với database/credentials riêng, không kết nối MySQL local. Khóa EC test được sinh vào thư mục tạm; không cần `.local` hoặc secrets để chạy tests. `WorkflowTests` dùng WebTestClient với HTTP server thật, controllable Clock và profile demo với password chỉ dành cho test.

- `ArchitectureTests`: không có vòng phụ thuộc giữa các module, package dùng chung không import module, controller không gọi repository.
- `GlobalExceptionHandlerTests`: CommonException, field validation, HTTP errors và sanitized 500.
- `SecureMdmApplicationTests`: context + Liquibase migration trên MySQL thật.
- `WorkflowTests`: CSRF, refresh reuse/concurrency/logout, RBAC, tenant isolation, enrollment one-use/concurrency/expiry/revoke, credential expiry, audit rollback, claim concurrency/FIFO/lease/TTL/result dedup, policy version/revision/compliance và revoke.
- `LogicTests`: command payload/result schemas, state transitions, compliance và ES256/key separation; xuất `target/java-commands.json` cho simulator interoperability.

`verify` sinh JaCoCo HTML/XML tại `target/site/jacoco` và kiểm tra tối thiểu 80% line / 70% branch trên các package `*/service`, `*/repository`, `security`. Không skip Testcontainers khi Docker thiếu; đó là verification chưa hoàn tất.

`verify` còn chạy Checkstyle (`google_checks.xml`, vi phạm sẽ làm build fail) và `spotless:check`. Có thể format lại toàn bộ bằng `./mvnw spotless:apply`. SonarLint chạy trong IDE ở connected mode tới SonarQube local (profile `quality` của `be/compose.yaml`), còn `./mvnw sonar:sonar` gửi kết quả Checkstyle và JaCoCo lên đó.

Chạy nhanh advice tests khi chỉ sửa error contract: `./mvnw -Dtest=GlobalExceptionHandlerTests test`. Lệnh này không thay thế full verify/coverage gate.

## Chạy backend local

Từ root chạy `node scripts/keys.mjs`, khởi động MySQL theo README, build jar qua `./mvnw verify`, rồi `node scripts/backend.mjs`. Launcher đọc `.local/demo.env`, dùng profile demo và private keys ngoài git. Khởi động thông thường cần DB_PASSWORD, ACCESS_KEY_FILE, COMMAND_KEY_FILE; không tự tạo admin account.

`be/compose.yaml` cũ chỉ phục vụ MySQL/SonarQube tùy chọn. Dùng root `compose.yaml` cho demo đầy đủ; không bật đồng thời hai MySQL trên cùng port3307. SonarQube/Strix không phải điều kiện chạy demo và không tự chạy trong workflow này.
