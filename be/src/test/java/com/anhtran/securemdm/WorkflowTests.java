package com.anhtran.securemdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anhtran.securemdm.src.command.dto.request.CreateCommandRequest;
import com.anhtran.securemdm.src.command.service.CommandService;
import com.anhtran.securemdm.src.device.dto.request.EnrollDeviceRequest;
import com.anhtran.securemdm.src.device.service.EnrollmentService;
import com.anhtran.securemdm.src.identity.service.AuthService;
import com.anhtran.securemdm.utils.JsonUtils;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "mdm.demo-password=test-only-password-123",
      "logging.level.root=WARN",
      "logging.level.com.anhtran.securemdm.exception=OFF"
    })
@ActiveProfiles("demo")
@Import(WorkflowTests.TestTime.class)
class WorkflowTests extends SecureMdmApplicationTests {
  static class MutableClock extends Clock {
    Instant now = Instant.now();

    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    public Clock withZone(ZoneId z) {
      return this;
    }

    public Instant instant() {
      return now;
    }

    void advance(long seconds) {
      now = now.plusSeconds(seconds);
    }
  }

  @TestConfiguration
  static class TestTime {
    @Bean
    @Primary
    MutableClock testClock() {
      return new MutableClock();
    }
  }

  @LocalServerPort int port;
  @Autowired MutableClock clock;
  @Autowired CommandService commands;
  @Autowired EnrollmentService enrollments;
  @Autowired AuthService auth;
  WebTestClient http;

  long count(String sql, String arg) {
    return database.sql(sql).bind(0, arg).map(row -> row.get(0, Long.class)).one().block();
  }

  @BeforeEach
  void setup() {
    clock.now = Instant.now();
    http =
        WebTestClient.bindToServer()
            .baseUrl("http://127.0.0.1:" + port + "/api/v1")
            .responseTimeout(Duration.ofSeconds(20))
            .build();
  }

  class Client {
    String access;
    final Map<String, String> cookies = new HashMap<>();

    Map<String, Object> call(String method, String path, Object body, int status) {
      return call(method, path, body, status, Map.of());
    }

    Map<String, Object> call(
        String method, String path, Object body, int status, Map<String, String> headers) {
      var request =
          http.method(HttpMethod.valueOf(method)).uri(path).cookies(c -> cookies.forEach(c::add));
      if (access != null) {
        request.header("Authorization", "Bearer " + access);
      }
      if (cookies.containsKey("XSRF-TOKEN")) {
        request.header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"));
      }
      headers.forEach(request::header);
      if (body != null) {
        request.contentType(MediaType.APPLICATION_JSON).bodyValue(body);
      }
      var response =
          request.exchange().expectStatus().isEqualTo(status).expectBody().returnResult();
      response.getResponseCookies().forEach((k, v) -> cookies.put(k, v.getFirst().getValue()));
      byte[] bytes = response.getResponseBody();
      return bytes == null || bytes.length == 0
          ? Map.of()
          : JsonUtils.readObject(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
    }
  }

  Client login(String org, String role) {
    Client c = new Client();
    c.call("GET", "/auth/csrf", null, 200);
    var r =
        c.call(
            "POST",
            "/auth/login",
            Map.of(
                "organizationSlug",
                org,
                "email",
                role + "@demo.local",
                "password",
                "test-only-password-123"),
            200);
    c.access = (String) r.get("accessToken");
    return c;
  }

  Map<String, Object> enroll(Client a) {
    var t = a.call("POST", "/enrollment-tokens", null, 200);
    return new Client()
        .call(
            "POST",
            "/device-enrollments",
            enrollment(t.get("token"), UUID.randomUUID().toString()),
            200);
  }

  Map<String, Object> enrollment(Object token, String uid) {
    return Map.of("token", token, "deviceUid", uid, "name", "Test Android");
  }

  Client device(Map<String, Object> e) {
    Client c = new Client();
    c.access = (String) e.get("credential");
    return c;
  }

  Map<String, Object> inventory(boolean lock) {
    return Map.of(
        "screenLockEnabled",
        lock,
        "androidApiLevel",
        35,
        "installedPackages",
        List.of("com.example.browser"));
  }

  Map<String, Object> command(Client a, String id, String type) {
    return a.call(
        "POST",
        "/devices/" + id + "/commands",
        Map.of("type", type, "payload", Map.of()),
        200,
        Map.of("Idempotency-Key", UUID.randomUUID().toString()));
  }

  @Test
  void csrfRefreshReuseLogoutAndRoles() {
    new Client().call("POST", "/auth/login", Map.of(), 403);
    var a = login("alpha", "org_admin");
    var old = new HashMap<>(a.cookies);
    final String oldAccess = a.access;
    a.access = null;
    final var rotated = a.call("POST", "/auth/refresh", null, 200);
    assertNotEquals(old.get("mdm_refresh"), a.cookies.get("mdm_refresh"));
    Client replay = new Client();
    replay.cookies.putAll(old);
    replay.call("POST", "/auth/refresh", null, 401);
    a.access = (String) rotated.get("accessToken");
    a.call("GET", "/auth/me", null, 401);
    a.access = oldAccess;
    a.call("GET", "/auth/me", null, 401);
    var fresh = login("alpha", "org_admin");
    fresh.call("POST", "/auth/logout", null, 200);
    fresh.call("GET", "/auth/me", null, 401);
    final var viewer = login("alpha", "viewer");
    viewer.call("POST", "/enrollment-tokens", null, 403);
    viewer.call("GET", "/audit-logs", null, 200);
    viewer.call("GET", "/devices?size=101", null, 400);
    var operator = login("alpha", "operator");
    operator.call("POST", "/enrollment-tokens", null, 403);
    operator.call("GET", "/devices", null, 200);
  }

  @Test
  void enrollmentConcurrencyExpiryRevokeAndRollback() throws Exception {
    var a = login("alpha", "org_admin");
    var t = a.call("POST", "/enrollment-tokens", null, 200);
    Object body = enrollment(t.get("token"), UUID.randomUUID().toString());
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Integer> task =
          () ->
              http.post()
                  .uri("/device-enrollments")
                  .contentType(MediaType.APPLICATION_JSON)
                  .bodyValue(body)
                  .exchange()
                  .returnResult(byte[].class)
                  .getStatus()
                  .value();
      var jobs = executor.invokeAll(List.of(task, task));
      var statuses = List.of(jobs.get(0).get(), jobs.get(1).get());
      assertTrue(statuses.contains(200));
      assertTrue(statuses.contains(401));
    }
    var revoked = a.call("POST", "/enrollment-tokens", null, 200);
    a.call("POST", "/enrollment-tokens/" + revoked.get("id") + "/revoke", null, 200);
    new Client()
        .call("POST", "/device-enrollments", enrollment(revoked.get("token"), "revoked"), 401);
    var expired = a.call("POST", "/enrollment-tokens", null, 200);
    clock.advance(901);
    new Client()
        .call("POST", "/device-enrollments", enrollment(expired.get("token"), "expired"), 401);
    clock.now = Instant.now();
    var t1 = a.call("POST", "/enrollment-tokens", null, 200);
    var t2 = a.call("POST", "/enrollment-tokens", null, 200);
    String uid = UUID.randomUUID().toString();
    new Client().call("POST", "/device-enrollments", enrollment(t1.get("token"), uid), 200);
    new Client().call("POST", "/device-enrollments", enrollment(t2.get("token"), uid), 409);
    new Client()
        .call(
            "POST",
            "/device-enrollments",
            enrollment(t2.get("token"), UUID.randomUUID().toString()),
            200);
  }

  @Test
  void policyTenantIsolationStaleReportsAndRevoke() {
    var a = login("alpha", "org_admin");
    var b = login("beta", "org_admin");
    final var viewer = login("alpha", "viewer");
    var e = enroll(a);
    String id = (String) e.get("deviceId");
    var d = device(e);
    d.call("POST", "/device-agent/heartbeat", inventory(false), 200);
    assertEquals("ONLINE", a.call("GET", "/devices/" + id, null, 200).get("connectivity"));
    b.call("GET", "/devices/" + id, null, 404);
    b.call("POST", "/devices/" + id + "/revoke", null, 404);
    var config =
        Map.of(
            "requireScreenLock",
            true,
            "minAndroidApiLevel",
            34,
            "blockedPackages",
            List.of("com.example.blocked"));
    var p = a.call("POST", "/policies", Map.of("name", "Corporate Android", "config", config), 200);
    String version = (String) p.get("id");
    final String policy = (String) p.get("policyId");
    b.call("PUT", "/devices/" + id + "/policy-assignment", Map.of("versionId", version), 404);
    viewer.call("PUT", "/devices/" + id + "/policy-assignment", Map.of("versionId", version), 403);
    var other = enroll(b);
    b.call(
        "PUT",
        "/devices/" + other.get("deviceId") + "/policy-assignment",
        Map.of("versionId", version),
        404);
    a.call("PUT", "/devices/" + id + "/policy-assignment", Map.of("versionId", version), 200);
    var report = Map.of("revision", 1, "versionId", version, "inventory", inventory(false));
    assertEquals(
        "NON_COMPLIANT",
        d.call("POST", "/device-agent/policy-report", report, 200).get("compliance"));
    assertEquals(
        "COMPLIANT",
        d.call(
                "POST",
                "/device-agent/policy-report",
                Map.of("revision", 1, "versionId", version, "inventory", inventory(true)),
                200)
            .get("compliance"));
    var v2 = a.call("POST", "/policies/" + policy + "/versions", config, 200);
    assertEquals(2, ((Number) v2.get("version")).intValue());
    a.call("GET", "/policies/" + policy + "/versions", null, 200);
    b.call("GET", "/policies/" + policy + "/versions", null, 404);
    a.call("PUT", "/devices/" + id + "/policy-assignment", Map.of("versionId", v2.get("id")), 200);
    d.call("POST", "/device-agent/policy-report", report, 409);
    assertEquals("PENDING", a.call("GET", "/devices/" + id, null, 200).get("compliance"));
    a.call("DELETE", "/devices/" + id + "/policy-assignment", null, 200);
    d.call("POST", "/device-agent/policy-report", report, 409);
    assertEquals(
        3, ((Number) d.call("GET", "/device-agent/policy", null, 200).get("revision")).intValue());
    clock.advance(91);
    assertEquals("UNKNOWN", a.call("GET", "/devices/" + id, null, 200).get("compliance"));
    command(a, id, "REPORT_STATUS");
    d.call("POST", "/device-agent/commands/claim", null, 200);
    command(a, id, "SYNC_DEVICE");
    a.call("POST", "/devices/" + id + "/revoke", null, 200);
    d.call("POST", "/device-agent/heartbeat", inventory(true), 401);
    var rows = (List<?>) a.call("GET", "/devices/" + id + "/commands", null, 200).get("items");
    assertTrue(rows.stream().anyMatch(r -> ((Map<?, ?>) r).get("status").equals("UNKNOWN")));
    assertTrue(rows.stream().anyMatch(r -> ((Map<?, ?>) r).get("status").equals("CANCELLED")));
    a.call("GET", "/dashboard/overview", null, 200);
    a.call("GET", "/audit-logs?action=DEVICE_REVOKED", null, 200);
  }

  @Test
  void commandsIdempotencyLeaseResultAndExpiry() {
    var a = login("alpha", "org_admin");
    var e = enroll(a);
    String id = (String) e.get("deviceId");
    var d = device(e);
    var payload = Map.of("type", "LOCK_DEVICE_SIMULATED", "payload", Map.of());
    var headers = Map.of("Idempotency-Key", UUID.randomUUID().toString());
    var c = a.call("POST", "/devices/" + id + "/commands", payload, 200, headers);
    assertEquals(
        c.get("id"),
        a.call("POST", "/devices/" + id + "/commands", payload, 200, headers).get("id"));
    a.call(
        "POST",
        "/devices/" + id + "/commands",
        Map.of("type", "SYNC_DEVICE", "payload", Map.of()),
        409,
        headers);
    final var claim = d.call("POST", "/device-agent/commands/claim", null, 200);
    d.call("POST", "/device-agent/commands/claim", null, 204);
    clock.advance(31);
    commands.sweep().block();
    var retry = d.call("POST", "/device-agent/commands/claim", null, 200);
    assertNotEquals(claim.get("attemptId"), retry.get("attemptId"));
    d.call(
        "POST",
        "/device-agent/commands/" + c.get("id") + "/result",
        Map.of(
            "attemptId",
            claim.get("attemptId"),
            "status",
            "SUCCEEDED",
            "result",
            Map.of("locked", true)),
        409);
    var valid =
        Map.of(
            "attemptId",
            retry.get("attemptId"),
            "status",
            "SUCCEEDED",
            "result",
            Map.of("locked", true));
    d.call("POST", "/device-agent/commands/" + c.get("id") + "/result", valid, 200);
    d.call("POST", "/device-agent/commands/" + c.get("id") + "/result", valid, 200);
    command(a, id, "SYNC_DEVICE");
    clock.advance(601);
    commands.sweep().block();
    a.call("GET", "/auth/me", null, 401);
    a = login("alpha", "org_admin");
    var rows = (List<?>) a.call("GET", "/devices/" + id + "/commands", null, 200).get("items");
    assertTrue(rows.stream().anyMatch(r -> ((Map<?, ?>) r).get("status").equals("EXPIRED")));
    command(a, id, "REPORT_STATUS");
    for (int i = 0; i < 3; i++) {
      d.call("POST", "/device-agent/commands/claim", null, 200);
      clock.advance(31);
      commands.sweep().block();
    }
    d.call("POST", "/device-agent/commands/claim", null, 204);
    rows = (List<?>) a.call("GET", "/devices/" + id + "/commands", null, 200).get("items");
    assertTrue(rows.stream().anyMatch(r -> ((Map<?, ?>) r).get("status").equals("UNKNOWN")));
  }

  @Test
  void concurrentClaimsAndRefreshAreSerialized() throws Exception {
    var a = login("alpha", "org_admin");
    var e = enroll(a);
    String id = (String) e.get("deviceId");
    command(a, id, "REPORT_STATUS");
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Integer> claim =
          () ->
              http.post()
                  .uri("/device-agent/commands/claim")
                  .header("Authorization", "Bearer " + e.get("credential"))
                  .exchange()
                  .returnResult(byte[].class)
                  .getStatus()
                  .value();
      var claims = executor.invokeAll(List.of(claim, claim));
      assertEquals(Set.of(200, 204), Set.of(claims.get(0).get(), claims.get(1).get()));
      Callable<Integer> refresh =
          () ->
              http.post()
                  .uri("/auth/refresh")
                  .cookies(c -> a.cookies.forEach(c::add))
                  .header("X-XSRF-TOKEN", a.cookies.get("XSRF-TOKEN"))
                  .exchange()
                  .returnResult(byte[].class)
                  .getStatus()
                  .value();
      var sessions = executor.invokeAll(List.of(refresh, refresh));
      assertEquals(Set.of(200, 401), Set.of(sessions.get(0).get(), sessions.get(1).get()));
    }
    a.call("GET", "/auth/me", null, 401);
  }

  @Test
  void auditFailureRollsBackEnrollmentAndCommand() {
    var a = login("alpha", "org_admin");
    var token = a.call("POST", "/enrollment-tokens", null, 200);
    String uid = UUID.randomUUID().toString();
    // Force the final audit insert to violate VARCHAR length, after business inserts succeeded.
    assertThrows(
        Exception.class,
        () ->
            enrollments
                .enroll(
                    new EnrollDeviceRequest((String) token.get("token"), uid, "Rollback device"))
                .contextWrite(ctx -> ctx.put("correlationId", "x".repeat(1000)))
                .block());
    assertEquals(0, count("SELECT COUNT(*) FROM devices WHERE device_uid=?", uid));
    var e =
        new Client().call("POST", "/device-enrollments", enrollment(token.get("token"), uid), 200);
    String key = UUID.randomUUID().toString();
    var actor = auth.authenticate(a.access).block();
    assertThrows(
        Exception.class,
        () ->
            commands
                .create(
                    actor,
                    (String) e.get("deviceId"),
                    new CreateCommandRequest("REPORT_STATUS", Map.of()),
                    key)
                .contextWrite(ctx -> ctx.put("correlationId", "x".repeat(1000)))
                .block());
    assertEquals(0, count("SELECT COUNT(*) FROM device_commands WHERE idempotency_key=?", key));
    assertEquals(
        "PENDING",
        commands
            .create(
                actor,
                (String) e.get("deviceId"),
                new CreateCommandRequest("REPORT_STATUS", Map.of()),
                key)
            .block()
            .getStatus());
  }

  @Test
  void credentialExpiryAndTenantCommandsAudit() {
    var a = login("alpha", "org_admin");
    var b = login("beta", "org_admin");
    final var viewer = login("alpha", "viewer");
    var e = enroll(a);
    String id = (String) e.get("deviceId");
    b.call("GET", "/devices/" + id + "/commands", null, 404);
    b.call(
        "POST",
        "/devices/" + id + "/commands",
        Map.of("type", "REPORT_STATUS", "payload", Map.of()),
        404,
        Map.of("Idempotency-Key", "cross-tenant"));
    viewer.call(
        "POST",
        "/devices/" + id + "/commands",
        Map.of("type", "REPORT_STATUS", "payload", Map.of()),
        403,
        Map.of("Idempotency-Key", "viewer-write"));
    var rows = (List<?>) b.call("GET", "/audit-logs?size=100", null, 200).get("items");
    assertTrue(rows.stream().noneMatch(r -> id.equals(((Map<?, ?>) r).get("resourceId"))));
    clock.advance(2_592_001);
    device(e).call("POST", "/device-agent/heartbeat", inventory(true), 401);
  }

  @Test
  void fifoDoesNotDependOnTimestampTies() {
    var a = login("alpha", "org_admin");
    var e = enroll(a);
    String id = (String) e.get("deviceId");
    var d = device(e);
    var first = command(a, id, "SYNC_DEVICE");
    var second = command(a, id, "LOCK_DEVICE_SIMULATED");
    final var claim = d.call("POST", "/device-agent/commands/claim", null, 200);
    assertEquals(first.get("id"), claim.get("commandId"));
    d.call(
        "POST",
        "/device-agent/commands/" + first.get("id") + "/result",
        Map.of(
            "attemptId",
            claim.get("attemptId"),
            "status",
            "SUCCEEDED",
            "result",
            Map.of("synced", true)),
        200);
    assertEquals(
        second.get("id"),
        d.call("POST", "/device-agent/commands/claim", null, 200).get("commandId"));
  }
}
