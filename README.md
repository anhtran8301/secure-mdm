# SecureMDM

Portfolio Mobile Device Management: Java reactive backend, React admin console và TypeScript device simulator. Demo tập trung vào tenant isolation, device PKI (CSR → X.509, proof-of-possession, thu hồi), rotation khóa ký lệnh, signed command queue và policy compliance; **mọi tác động trên thiết bị đều mô phỏng**.

## Chạy demo bằng Docker

Yêu cầu: Docker Compose v2, Node 24 (tạo khóa), khoảng 4 GB RAM trống. Chạy từ thư mục repo:

```sh
node scripts/keys.mjs
docker compose --env-file .local/demo.env up -d --build
```

Nâng cấp từ bản trước device PKI: chạy lại `node scripts/keys.mjs`. Lệnh này tạo thêm `device-ca.json` và giữ nguyên các khóa cũ. Thiết bị đã enroll bằng credential cũ phải enroll lại với state directory mới.

Mở **http://127.0.0.1:8081**. Backend: http://127.0.0.1:8080; MySQL chỉ bind loopback 3307. Đợi backend khởi động và migrations hoàn tất (`docker compose --env-file .local/demo.env logs backend`).

- Tổ chức: `alpha` hoặc `beta`.
- Email: `org_admin@demo.local`, `operator@demo.local`, `viewer@demo.local`.
- Mật khẩu: giá trị `DEMO_PASSWORD` trong **file local `.local/demo.env`**, được tạo ngẫu nhiên riêng trên máy bạn. Script chạy lại giữ nguyên khóa và mật khẩu.
- Demo seed chỉ hoạt động với profile `demo`. Bản chạy thông thường không tạo tài khoản/mật khẩu dùng chung.

Trong Enrollment, tạo token. Có thể chạy simulator bằng Docker:

```sh
# Nhập token mà không lưu vào shell history (bash/zsh):
read -s ENROLLMENT_TOKEN
export ENROLLMENT_TOKEN
docker compose --env-file .local/demo.env --profile simulator run --rm simulator enroll --state /state --uid interview-android --name 'Interview Android'
unset ENROLLMENT_TOKEN
docker compose --env-file .local/demo.env --profile simulator up -d simulator
```

Simulator chia sẻ loopback network của backend; state nằm trong volume riêng. Dừng demo: `docker compose --env-file .local/demo.env --profile simulator down` (giữ dữ liệu). Không dùng `down -v` trừ khi chủ động muốn xóa dữ liệu demo.

## Phát triển local

Yêu cầu thêm Java 21, Docker, Node 24, Corepack. **Yarn 1.22.22** được pin ở root, frontend và simulator; `yarn.lock` là lockfile duy nhất, không dùng `npm install`.

Lần đầu clone, chạy `corepack yarn install` ở root để cài Git hooks (Husky):

- `pre-commit` chạy lint-staged: Spotless (Google Java Style) và Checkstyle cho `be/*.java`, ESLint và Prettier cho `fe/`, Prettier cho `simulator/`.
- `commit-msg` chạy commitlint theo Conventional Commits, ví dụ `feat(be): ...`; scope hợp lệ: `be|fe|sim|e2e|ci|docs|ai|deps|repo`.

`.editorconfig` và `.vscode/` (gồm extension SonarLint, Checkstyle, ESLint, Prettier) giữ cấu hình IDE đồng nhất. Hướng dẫn cho AI agent nằm trong [AGENTS.md](AGENTS.md).

```sh
node scripts/keys.mjs
docker compose --env-file .local/demo.env up -d --wait mysql
cd be
./mvnw verify
cd ../fe
corepack yarn install --frozen-lockfile
cd ../simulator
corepack yarn install --frozen-lockfile
cd ..
node scripts/backend.mjs
```

Terminal khác: `cd fe && corepack yarn dev`, mở **http://127.0.0.1:3039**. Vite proxy `/api` tới backend 8080; không cần CORS. Backend launcher đọc cấu hình local, không in secrets. Nếu chưa cấu hình Java mặc định, đặt `JAVA_HOME` tới JDK 21.

Simulator local:

```sh
cd simulator
read -s ENROLLMENT_TOKEN
export ENROLLMENT_TOKEN
corepack yarn dev enroll --state ../.local/android-demo --uid interview-android --name 'Interview Android'
unset ENROLLMENT_TOKEN
corepack yarn dev run --state ../.local/android-demo
corepack yarn dev status --state ../.local/android-demo
```

Mỗi simulator dùng **state directory và UID riêng**. Restart dùng lại directory. Chứng chỉ thiết bị hết hạn/thu hồi: cấp enrollment token mới, chọn directory và UID mới; registry cũ được giữ để truy vết. UID/inventory không phải hardware attestation. Chỉ HTTPS được chấp nhận, ngoại lệ HTTP loopback cho demo.

## Kiểm thử

```sh
cd be && ./mvnw verify
cd ../fe && corepack yarn lint && corepack yarn build && corepack yarn test
cd ../simulator && corepack yarn typecheck && corepack yarn test && corepack yarn build
```

Backend tests tự khởi động MySQL 8.4 Testcontainers, sinh khóa tạm, seed dữ liệu test; không dùng database local. `verify` áp ngưỡng tổng **80% line / 70% branch** trên các module identity, device, command, policy và audit, gồm cả HTTP boundary. Báo cáo: `be/target/site/jacoco/index.html` và `be/target/surefire-reports`.

Fixture Java–TypeScript đã lưu tại `simulator/fixtures/java-commands.json` chỉ chứa public keys và command test. Để kiểm tra artifact mới sinh bởi Nimbus trong cùng lượt build: từ `simulator`, chạy `JAVA_FIXTURE=../be/target/java-commands.json corepack yarn test`.

E2E dùng database RAM riêng 3308, backend 8082 và frontend 3040:

```sh
node scripts/keys.mjs .local/e2e
docker compose -f compose.e2e.yaml --env-file .local/e2e/demo.env up -d --wait
cd fe
corepack yarn playwright install chromium
corepack yarn test:e2e
cd ..
docker compose -f compose.e2e.yaml --env-file .local/e2e/demo.env down
```

Chạy `be/./mvnw verify` và cài dependency frontend/simulator trước E2E. Playwright tự bật/tắt hai server; cần các port trống. HTML report và trace lỗi ở `fe/playwright-report`, `fe/test-results`. Đây là artifacts test có thể chứa credential demo tạm; không dùng tài khoản thật cho E2E.

## Tài liệu

- [Kế hoạch M0–M5](docs/SECUREMDM_PLAN.md)
- [Kiến trúc và hợp đồng API](docs/ARCHITECTURE.md)
- [Threat model](docs/THREAT_MODEL.md)
- [Kịch bản phỏng vấn 5–7 phút và rotation](docs/DEMO.md)
- [Bằng chứng kiểm thử và review](docs/VERIFICATION.md)

Phạm vi sau demo: Android Enterprise/iOS agent thật, key attestation và khóa trong Keystore/StrongBox/Secure Enclave, gia hạn chứng chỉ, FCM, mTLS, SCEP/EST, CRL/OCSP, KMS và cloud. Không triển khai remote wipe hoặc điều khiển thiết bị thật trong bản này. Triển khai ngoài máy cá nhân cần HTTPS, `SECURE_COOKIES=true`, khóa/secrets và cấu hình production riêng; Docker Compose hiện tại chỉ dành cho demo loopback.
