# Kịch bản phỏng vấn 5–7 phút

1. **0:00–0:45 — Scope/architecture:** mở sơ đồ; giải thích modular monolith + WebFlux/R2DBC. Simulator không phải Android Device Owner, không hardware attestation.
2. **0:45–1:45 — Identity/enrollment:** login alpha/admin. Tạo token chỉ thấy plaintext một lần, enroll CLI, run và xem ONLINE/heartbeat. Giải thích hash credential, one-use token và transaction.
3. **1:45–2:45 — Policy:** tạo policy screen lock + min API 34 + một package bị chặn. Gán version 1, quan sát PENDING → COMPLIANT. Tạo version 2 min API 36 rồi gán; simulator API 35 không tự nâng OS nên NON_COMPLIANT.
4. **2:45–3:45 — Signed queue:** SHOW_MESSAGE, xem PENDING → IN_PROGRESS → SUCCEEDED và audit. Nêu sự khác biệt signature authenticity và transport confidentiality. Private key chỉ ở server; trusted public keys provision trước.
5. **3:45–5:00 — Recovery:** dừng run, gửi SHOW_MESSAGE mới; chạy `run --once --crash-after-result` (exit 75), chờ lease hết rồi chạy lại. `status` cho thấy message một lần, command attempts tăng. Mất kết quả cuối cùng có thể UNKNOWN, không diễn giải là FAILED.
6. **5:00–6:00 — Boundaries:** viewer chỉ đọc; beta gọi device của alpha trả404. Revoke credential, simulator nhận401. Audit có correlation ID, không secret.
7. **6:00–7:00 — Evidence/tradeoffs:** mở JaCoCo và Playwright report. Trình bày tests concurrency, rollback, stale revision, tampered JWS; nhấn mạnh chưa mTLS/attestation/real agent.

## Chế độ simulator

Từ `simulator/`, dùng cùng `--state ../.local/android-demo` và không chạy hai process cùng directory:

```sh
corepack yarn dev run --state ../.local/android-demo --once
corepack yarn dev run --state ../.local/android-demo --once --crash-after-result
corepack yarn dev run --state ../.local/android-demo --once --fail
corepack yarn dev run --state ../.local/android-demo --offline-ms 95000
corepack yarn dev status --state ../.local/android-demo
```

`--fail` tạo FAILED với mã SIMULATED_FAILURE; `--offline-ms` trì hoãn kết nối để dashboard chuyển OFFLINE/UNKNOWN. Crash mode lưu state+result trước khi dừng; restart trả cached outcome cho attempt mới. Nếu không gửi result sau 3 attempts hoặc TTL thì UNKNOWN; command chưa giao hết TTL là EXPIRED. Thu hồi chỉ ngăn request về sau, không rollback tác động trước đó.

## Signing-key rotation

`scripts/keys.mjs` tạo command-1/command-2 và JWKS public chứa cả hai. Đây là **pre-distribution**: đưa JWKS này tới simulator trước khi đổi signer.

1. Đang dùng `command-1.json`: gửi lệnh A, để pending.
2. Kiểm tra trusted JWKS của mọi simulator đã có `command-2` (không chép private JWK sang simulator).
3. Với Docker: `COMMAND_KEY_NAME=command-2.json docker compose --env-file .local/demo.env up -d --no-deps --force-recreate backend`. Với local launcher, dừng và chạy backend với command key mới qua cấu hình riêng (launcher mặc định command-1; có thể chạy jar trực tiếp với `COMMAND_KEY_FILE`).
4. Gửi lệnh B. Simulator chấp nhận cả A (key cũ) và B (key mới); fixture Java–TypeScript cũng kiểm tra hai chữ ký.
5. Giữ key1 trong trust store đến khi tất cả command cũ terminal/hết TTL. Sau đó mới bỏ public key1. Không xóa private key bằng script tự động. Bản demo chưa có administrative rotation API/KMS.

## Chuẩn bị trước buổi demo

Chạy README từ checkout sạch, mở `.local/demo.env` để lấy mật khẩu, giữ các server ở loopback. Chạy tests trước, không cố cài dependencies trong buổi demo. Có thể trình bày `fe/playwright-report/index.html` cho luồng recovery và tenant isolation thay vì chờ timeout trực tiếp.
