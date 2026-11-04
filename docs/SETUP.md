# Hướng dẫn cài đặt và chạy SecureMDM

[← README](../README.md)

Tài liệu này dành cho người muốn chạy demo hoặc phát triển tiếp. Mọi lệnh chạy từ thư mục gốc của repo, trừ khi ghi khác. Docker Compose hiện tại chỉ dành cho **demo trên loopback**, không dùng để triển khai ra Internet.

## 1. Yêu cầu môi trường

| Công cụ                    | Phiên bản                               | Dùng cho                                                           |
| -------------------------- | --------------------------------------- | ------------------------------------------------------------------ |
| Docker + Docker Compose v2 | Docker Engine có Compose v2             | MySQL, chạy demo, Testcontainers trong test backend, E2E           |
| Java (JDK)                 | 21                                      | Backend (Maven Wrapper `./mvnw` có sẵn, không cần cài Maven)       |
| Node.js                    | 24                                      | Tạo khóa (`scripts/keys.mjs`), frontend, simulator                 |
| Corepack + Yarn            | Yarn 1.22.22 (pin trong `package.json`) | Cài dependency. Dùng `corepack yarn`, **không dùng `npm install`** |
| RAM trống                  | khoảng 4 GB                             | Chạy đủ MySQL, backend, frontend                                   |

Nếu Java mặc định không phải 21, đặt `JAVA_HOME` trỏ tới JDK 21.

## 2. Tạo khóa và biến môi trường

```sh
node scripts/keys.mjs
```

Script tạo thư mục `.local/` (đã nằm trong `.gitignore`) gồm:

| File                  | Nội dung                                                                                    |
| --------------------- | ------------------------------------------------------------------------------------------- |
| `access.json`         | Khóa riêng EC P-256 ký access token của admin                                               |
| `command-1.json`      | Khóa riêng EC P-256 ký lệnh gửi thiết bị (khóa active mặc định)                             |
| `command-2.json`      | Khóa ký lệnh thứ hai, chỉ dùng khi demo xoay khóa ([DEMO.md](DEMO.md#signing-key-rotation)) |
| `device-ca.json`      | Khóa riêng EC P-256 của Device CA nội bộ                                                    |
| `command-public.json` | Public key của command key, cấp sẵn cho simulator làm trust anchor                          |
| `demo.env`            | Mật khẩu database và mật khẩu tài khoản demo, sinh ngẫu nhiên trên máy bạn                  |

File khóa được ghi với quyền `0600`. Chạy lại script sẽ giữ nguyên khóa và mật khẩu đã có, chỉ tạo phần còn thiếu. Ví dụ, nâng cấp từ bản trước khi có Device PKI sẽ chỉ sinh thêm `device-ca.json`. **Không commit, không chia sẻ thư mục `.local/`.**

Biến môi trường chính của backend (Compose đã đặt sẵn):

| Biến                                            | Ý nghĩa                                                                         | Mặc định                               |
| ----------------------------------------------- | ------------------------------------------------------------------------------- | -------------------------------------- |
| `DB_HOST`, `DB_PORT`, `DB_PASSWORD`             | Kết nối MySQL                                                                   | `localhost`, `3307`, lấy từ `demo.env` |
| `ACCESS_KEY_FILE`                               | Đường dẫn khóa ký access token                                                  | `../.local/access.json`                |
| `COMMAND_KEY_FILES`                             | Danh sách file command key (phân tách bằng dấu phẩy)                            | `../.local/command-1.json`             |
| `COMMAND_ACTIVE_KID`                            | `kid` của command key đang ký; bắt buộc khi có nhiều hơn một khóa               | trống                                  |
| `DEVICE_CA_KEY_FILE`                            | Khóa của Device CA                                                              | `../.local/device-ca.json`             |
| `SECURE_COOKIES`                                | Gắn cờ `Secure` cho cookie refresh; chỉ đặt `false` khi demo HTTP trên loopback | `true`                                 |
| `SPRING_PROFILES_ACTIVE=demo` + `DEMO_PASSWORD` | Seed hai tổ chức và tài khoản demo; profile thường không seed                   | không bật                              |
| `API_DOCS_ENABLED`                              | Bật hoặc tắt Swagger UI                                                         | bật                                    |

Tham số hàng đợi lệnh (Spring relaxed binding từ `mdm.*`): `MDM_LEASE_MS` (30 s), `MDM_MAX_ATTEMPTS` (3), `MDM_COMMAND_TTL_MS` (10 phút), `MDM_OFFLINE_MS` (90 s).

Backend kiểm tra khóa ngay khi khởi động. Các khóa phải là khóa riêng P-256 có `kid`, và access key, command key, Device CA key phải khác nhau (so bằng JWK thumbprint). Vi phạm thì backend dừng ngay, không chạy với cấu hình sai.

## 3. Chạy demo bằng Docker

```sh
node scripts/keys.mjs
docker compose --env-file .local/demo.env up -d --build
```

| Dịch vụ               | Địa chỉ                                                                         |
| --------------------- | ------------------------------------------------------------------------------- |
| Admin console (nginx) | http://127.0.0.1:8081                                                           |
| Backend API           | http://127.0.0.1:8080 (Swagger: `/swagger-ui.html`, health: `/actuator/health`) |
| MySQL 8.4             | `127.0.0.1:3307` (chỉ bind loopback)                                            |

Database migration (Liquibase) chạy khi backend khởi động. Theo dõi bằng `docker compose --env-file .local/demo.env logs backend`.

Đăng nhập:

- Tổ chức: `alpha` hoặc `beta`.
- Email: `org_admin@demo.local`, `operator@demo.local` hoặc `viewer@demo.local`.
- Mật khẩu: giá trị `DEMO_PASSWORD` trong file local `.local/demo.env`.

### Chạy simulator thiết bị bằng Docker

Vào màn **Enrollment** trên console, tạo enrollment token, rồi chạy:

```sh
# Nhập token mà không lưu vào shell history (bash/zsh)
read -s ENROLLMENT_TOKEN
export ENROLLMENT_TOKEN
docker compose --env-file .local/demo.env --profile simulator run --rm simulator \
  enroll --state /state --uid interview-android --name 'Interview Android'
unset ENROLLMENT_TOKEN
docker compose --env-file .local/demo.env --profile simulator up -d simulator
```

Dừng demo nhưng giữ dữ liệu: `docker compose --env-file .local/demo.env --profile simulator down`. Chỉ thêm `-v` khi thật sự muốn xóa dữ liệu demo.

## 4. Phát triển local

Cài Git hooks (Husky) một lần sau khi clone:

```sh
corepack yarn install
```

- `pre-commit` chạy lint-staged: Spotless + Checkstyle cho Java, ESLint + Prettier cho `fe/`, Prettier cho `simulator/`.
- `commit-msg` chạy commitlint theo Conventional Commits, scope hợp lệ: `be|fe|sim|e2e|ci|docs|ai|deps|repo`.

### Database và backend

```sh
node scripts/keys.mjs
docker compose --env-file .local/demo.env up -d --wait mysql
node scripts/backend.mjs
```

`scripts/backend.mjs` đọc cấu hình trong `.local/` rồi chạy backend ở cổng 8080 mà không in secret ra màn hình.

### Frontend

```sh
cd fe
corepack yarn install --frozen-lockfile
corepack yarn dev
```

Mở http://127.0.0.1:3039. Vite proxy `/api` tới backend 8080, nên không cần cấu hình CORS. Trình duyệt phải hỗ trợ Web Locks API.

### Simulator

```sh
cd simulator
corepack yarn install --frozen-lockfile
read -s ENROLLMENT_TOKEN
export ENROLLMENT_TOKEN
corepack yarn dev enroll --state ../.local/android-demo --uid interview-android --name 'Interview Android'
unset ENROLLMENT_TOKEN
corepack yarn dev run --state ../.local/android-demo
corepack yarn dev status --state ../.local/android-demo
```

Chế độ dùng cho demo: `run --once`, `--crash-after-result` (dừng sau khi lưu kết quả, trước khi báo server), `--fail` (trả FAILED), `--offline-ms <ms>` (giả lập mất kết nối). Xem [DEMO.md](DEMO.md).

Lưu ý:

- Mỗi simulator dùng **một state directory và một UID riêng**. Không chạy hai process trên cùng directory (có file lock).
- Chứng chỉ thiết bị hết hạn (30 ngày) hoặc bị thu hồi: cấp token mới, rồi enroll lại với directory và UID mới.
- Simulator chỉ chấp nhận HTTPS, trừ HTTP tới loopback dành cho demo.

## 5. Chạy test

| Phạm vi               | Lệnh                                                                                                  | Ghi chú                                                                                                         |
| --------------------- | ----------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| Backend               | `cd be && ./mvnw verify`                                                                              | Cần Docker (Testcontainers tự bật MySQL 8.4). Chạy kèm Spotless check, Checkstyle, JaCoCo 80% line / 70% branch |
| Backend, một lớp test | `cd be && ./mvnw -Dtest=WorkflowTests verify`                                                         |                                                                                                                 |
| Định dạng Java        | `cd be && ./mvnw spotless:apply`                                                                      |                                                                                                                 |
| Frontend              | `cd fe && corepack yarn lint && corepack yarn typecheck && corepack yarn test && corepack yarn build` | Vitest + Testing Library                                                                                        |
| Simulator             | `cd simulator && corepack yarn typecheck && corepack yarn test`                                       | Có test tương thích chữ ký Java (Nimbus) → TypeScript (`jose`)                                                  |

Báo cáo backend: `be/target/site/jacoco/index.html`, `be/target/surefire-reports`.

Để kiểm tra fixture Java sinh mới trong cùng lượt build: từ `simulator/`, chạy `JAVA_FIXTURE=../be/target/java-commands.json corepack yarn test`.

### E2E (Playwright)

E2E dùng môi trường riêng: MySQL 3308 (tmpfs), backend 8082, frontend 3040.

```sh
node scripts/keys.mjs .local/e2e
docker compose -f compose.e2e.yaml --env-file .local/e2e/demo.env up -d --wait
cd fe
corepack yarn playwright install chromium
corepack yarn test:e2e
cd ..
docker compose -f compose.e2e.yaml --env-file .local/e2e/demo.env down
```

Chạy `./mvnw verify` trong `be/` và cài dependency của frontend và simulator trước khi chạy E2E. Playwright tự bật và tắt backend và frontend, nên các cổng trên phải đang trống. Report và trace nằm ở `fe/playwright-report`, `fe/test-results`. Các file này có thể chứa credential demo tạm thời, nên không đưa chúng lên mạng.

## 6. Lưu ý khi triển khai ngoài máy cá nhân

Cấu hình demo **không** đủ cho production. Tối thiểu cần:

- chạy sau HTTPS và đặt `SECURE_COOKIES=true`;
- tạo khóa và secret riêng, lý tưởng là giữ trong KMS hoặc HSM;
- không bật profile `demo`;
- thêm rate limit và chính sách lưu trữ dữ liệu.

Xem [THREAT_MODEL.md](THREAT_MODEL.md).
