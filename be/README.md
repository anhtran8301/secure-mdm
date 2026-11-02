# Secure MDM Backend

Java 21, Spring Boot 4.1.2, WebFlux, R2DBC MySQL và Liquibase. Swagger cung cấp tài liệu API; JaCoCo và SonarQube phục vụ kiểm tra chất lượng code.

## Chạy local

Yêu cầu Java 21 và Docker đang chạy. Các lệnh dưới đây chạy từ thư mục `be`.

```bash
cp .env.example .env
```

Điền `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD` bằng hai mật khẩu riêng trong `.env`. Nếu chạy SonarQube, điền thêm `SONAR_DB_PASSWORD`. File này không được commit. Có thể tạo từng mật khẩu bằng `openssl rand -hex 24`.

```bash
docker compose up -d --wait mysql
set -a
. ./.env
set +a
./mvnw spring-boot:run
```

Compose tự đọc `.env`; Spring Boot không tự đọc file này, nên cần export biến như trên hoặc cấu hình environment trong IDE. `.env` được source bởi shell nên chỉ dùng các phép gán biến, đặt giá trị đặc biệt trong dấu nháy. Nếu dùng SDKMAN, chạy `sdk env` để chọn Java 21 theo `.sdkmanrc`.

| Thành phần | Mặc định |
| --- | --- |
| Backend | `http://localhost:8080` |
| Health | `http://localhost:8080/actuator/health` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| MySQL | `localhost:3307/secure_mdm`, user `secure_mdm` |

`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` điều khiển cả kết nối R2DBC và Liquibase JDBC. `SERVER_PORT` đổi cổng HTTP. `API_DOCS_ENABLED=false` tắt Swagger UI và OpenAPI. Chưa có API nghiệp vụ nên danh sách operations ban đầu có thể rỗng.

MySQL tạo database/user khi volume được khởi tạo lần đầu. Đổi password trong `.env` không tự cập nhật tài khoản trong volume cũ; cần đổi password trong MySQL tương ứng. Liquibase cần database đã tồn tại và tài khoản có quyền DDL trong database đó.

## Exception handling

`advice/GlobalExceptionHandler` xử lý lỗi từ annotated controllers, gồm cả lỗi phát ra từ `Mono`/`Flux` trước khi response được gửi. Phần này tham khảo `simple-bank`, dùng các class trong package `com.anhtran.secure_mdm.common`:

```java
throw new CommonException("MDM01", "Device not found", HttpStatus.NOT_FOUND);

// Trong reactive pipeline:
return Mono.error(new CommonException("MDM02", "Device already enrolled", HttpStatus.CONFLICT));

throw new ValidateException("RQ01", Map.of("name", "Name is required"), HttpStatus.BAD_REQUEST);
```

`CommonException` trả đúng HTTP status cùng JSON `{ "code": "MDM01", "message": "Device not found", "status": "NOT_FOUND" }`. Chỉ truyền thông báo nghiệp vụ được phép hiển thị cho client vào exception này.

`ValidateException` giữ contract của simple-bank: trả trực tiếp map `{ "name": "Name is required" }` với status đã chỉ định; code được giữ trong exception nhưng không đưa vào response map. Validation `@Valid` trên request body cũng trả HTTP 400 với map lỗi theo field, gộp nhiều thông báo của cùng field. Lỗi cấp object dùng tên object làm key.

Các lỗi HTTP của Spring như JSON sai định dạng, media type không hỗ trợ và `ResponseStatusException` giữ status/headers, dùng response ProblemDetail mặc định của WebFlux. Chi tiết lỗi HTTP 5xx được ẩn. Lỗi ngoài dự kiến trả HTTP 500 với code `9999`, message `Internal server error`; stack trace chỉ ghi vào log server.

Test riêng cho advice không cần database hoặc Docker:

```bash
./mvnw -Dtest=GlobalExceptionHandlerTests verify
```

## Migration

`src/main/resources/db/changelog/changelog-master.xml` là điểm vào duy nhất, ban đầu chưa có changeset nghiệp vụ. Thêm file đánh số như `001-create-tables.xml`, `002-init-data.xml`, `003-add-indexes.xml` và include theo thứ tự từ master. SQL dài đặt trong `db/scripts/sql` rồi tham chiếu bằng `sqlFile`.

Giữ nguyên changeset đã chạy; mọi thay đổi tiếp theo dùng changeset mới. Không dùng `runAlways` cho tạo schema. Liquibase chạy trước khi ứng dụng sẵn sàng; migration lỗi khiến startup thất bại.

## Test và coverage

```bash
./mvnw clean verify
```

Testcontainers tự tạo MySQL 8.4 với cổng và credentials riêng, không dùng MySQL local hoặc `.env`. Docker phải hoạt động; test không tự bỏ qua khi Docker thiếu. Changelog trong test resources include master rồi tạo dữ liệu kiểm thử, được đọc lại qua R2DBC. Test HTTP kiểm tra health, OpenAPI, Swagger assets và trạng thái tắt tài liệu.

Báo cáo JaCoCo: `target/site/jacoco/index.html` và `target/site/jacoco/jacoco.xml`. JaCoCo chạy cùng Maven lifecycle; build/test không cần SonarQube và chưa áp ngưỡng coverage.

## SonarQube local

Điền `SONAR_DB_PASSWORD` trong `.env`, sau đó:

```bash
docker compose --profile quality up -d sonarqube
docker compose --profile quality logs -f sonarqube
```

Đợi server sẵn sàng rồi mở `http://localhost:9000`. Đăng nhập local lần đầu bằng `admin`/`admin`, đổi mật khẩu, tạo project key `secure-mdm-be` (tên `Secure MDM Backend`) và tạo project analysis token. PostgreSQL chỉ phục vụ SonarQube, không expose ra host. Các volume lưu database và dữ liệu SonarQube qua các lần restart.

Export `SONAR_TOKEN` vào shell từ nguồn riêng, không ghi token vào repo hoặc `.env.example`. Scanner nhận `SONAR_HOST_URL` từ môi trường:

```bash
export SONAR_HOST_URL=http://localhost:9000
./mvnw clean verify
./mvnw sonar:sonar
```

Nếu đổi `SONAR_PORT`, cập nhật `SONAR_HOST_URL` tương ứng. Scanner chạy từ `be`, phân tích backend và import XML coverage do lệnh `verify` vừa tạo. Sau scan, kiểm tra Background Tasks đã xử lý xong và dashboard có coverage. Scanner gửi thành công chưa đồng nghĩa Quality Gate đạt; mặc định không chờ Quality Gate và không ràng buộc ngưỡng coverage.

Profile `quality` chạy độc lập với MySQL của ứng dụng. Docker cần đủ tài nguyên cho SonarQube/Elasticsearch (tối thiểu khoảng 4 GB RAM cho SonarQube); xem log khi container không sẵn sàng. Image `sonarqube:community` theo mẫu simple-bank là tag cập nhật, cần pin version trước khi dùng cho môi trường dùng chung lâu dài.

```bash
docker compose --profile quality stop
```

Lệnh trên giữ volume. Chỉ xóa volume khi chủ động muốn mất dữ liệu local; không dùng `down -v` để restart thông thường.
