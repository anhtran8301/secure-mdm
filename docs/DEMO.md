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

Quy trình theo [ADR 0006](adr/0006-command-key-rotation-signed-trust-bundle.md). Simulator chỉ được cấp sẵn public key `command-1` (`command-public.json`). Các khóa sau đó, nó học qua `GET /device-agent/command-keys` mỗi lần heartbeat. Endpoint này trả cùng một danh sách khóa, ký riêng bằng từng khóa đang publish. Simulator chỉ nhận bản ký bởi khóa nó đã tin, và lưu trust mới vào `<state>/trust.json`.

1. **Publish**: chạy backend với `COMMAND_KEY_FILES=/keys/command-1.json,/keys/command-2.json COMMAND_ACTIVE_KID=command-1`. Gửi lệnh A, để pending. Đợi một heartbeat (15 s), rồi kiểm tra `trust.json` của simulator đã có `command-2`.
2. **Switch**: đổi `COMMAND_ACTIVE_KID=command-2`, giữ nguyên hai file khóa, rồi recreate backend (`docker compose --env-file .local/demo.env up -d --no-deps --force-recreate backend`). Gửi lệnh B. Simulator chấp nhận cả A (khóa cũ) lẫn B (khóa mới). Thiết bị bỏ lỡ bước 1 vẫn bắt kịp qua bundle ký bằng `command-1`.
3. **Retire**: chỉ sau ít nhất `mdm.command-ttl-ms` (10 phút) kể từ bước 2, khi mọi lệnh ký bằng `command-1` đã hết hạn, mới đặt `COMMAND_KEY_FILES=/keys/command-2.json`. Thiết bị không heartbeat lần nào trong khoảng từ bước 1 tới bước 3 sẽ phải được cấp lại trust.

Không có API quản trị để rotation hay KMS. Private key không bao giờ rời backend.

## Chuẩn bị trước buổi demo

Chạy README từ checkout sạch, mở `.local/demo.env` để lấy mật khẩu, giữ các server ở loopback. Chạy tests trước, không cố cài dependencies trong buổi demo. Có thể trình bày `fe/playwright-report/index.html` cho luồng recovery và tenant isolation thay vì chờ timeout trực tiếp.
