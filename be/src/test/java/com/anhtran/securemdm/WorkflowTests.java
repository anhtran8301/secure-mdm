package com.anhtran.securemdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anhtran.securemdm.security.SigningKeys;
import com.anhtran.securemdm.src.command.dto.request.CreateCommandRequest;
import com.anhtran.securemdm.src.command.service.CommandService;
import com.anhtran.securemdm.src.device.dto.request.EnrollDeviceRequest;
import com.anhtran.securemdm.src.device.service.DeviceService;
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
  @Autowired SigningKeys keys;
  @Autowired DeviceService deviceService;
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
    com.nimbusds.jose.jwk.ECKey deviceKey;
    String serial;
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
      if (deviceKey != null) {
        request.header(
            "Authorization",
            "DeviceProof "
                + TestDevices.proof(deviceKey, serial, method, "/api/v1" + path, clock.millis()));
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

  /** Enrolls a fresh test device; the result also carries its private key under "key". */
  Map<String, Object> enroll(Client a) {
    var t = a.call("POST", "/enrollment-tokens", null, 200);
    var key = TestDevices.newKey();
    var e =
        new HashMap<>(
            new Client()
                .call(
                    "POST",
                    "/device-enrollments",
                    enrollment(t.get("token"), UUID.randomUUID().toString(), TestDevices.csr(key)),
                    200));
    e.put("key", key);
    return e;
  }

  Map<String, Object> enrollment(Object token, String uid) {
    return enrollment(token, uid, TestDevices.csr(TestDevices.newKey()));
  }

  Map<String, Object> enrollment(Object token, String uid, String csr) {
    return Map.of("token", token, "deviceUid", uid, "name", "Test Android", "csr", csr);
  }

  Client device(Map<String, Object> e) {
    Client c = new Client();
    c.deviceKey = (com.nimbusds.jose.jwk.ECKey) e.get("key");
    c.serial = (String) e.get("serial");
    return c;
  }

  String proofHeader(Map<String, Object> e, String method, String path) {
    return "DeviceProof "
        + TestDevices.proof(
            (com.nimbusds.jose.jwk.ECKey) e.get("key"),
            (String) e.get("serial"),
            method,
            "/api/v1" + path,
            clock.millis());
  }

  Map<String, Object> inventory(boolean lock) {
    return Map.of(
        "screenLockEnabled",
        lock,
        "diskEncrypted",
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
            "requireDiskEncryption",
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
    var certificate = (Map<?, ?>) a.call("GET", "/devices/" + id, null, 200).get("certificate");
    assertEquals(e.get("serial"), certificate.get("serial"));
    assertEquals(false, certificate.get("revoked"));
    assertEquals(64, ((String) certificate.get("fingerprint")).length());
    assertEquals(e.get("expiresAt"), certificate.get("notAfter"));
    a.call("POST", "/devices/" + id + "/revoke", null, 200);
    d.call("POST", "/device-agent/heartbeat", inventory(true), 401);
    assertEquals(
        true,
        ((Map<?, ?>) a.call("GET", "/devices/" + id, null, 200).get("certificate")).get("revoked"));
    assertEquals(
        1,
        count(
            "SELECT COUNT(*) FROM device_certificates WHERE device_id=? AND revoked_at IS NOT NULL",
            id));
    var revokedCert =
        (List<?>)
            a.call("GET", "/audit-logs?action=DEVICE_CERTIFICATE_REVOKED&size=100", null, 200)
                .get("items");
    assertTrue(
        revokedCert.stream()
            .anyMatch(r -> e.get("serial").equals(((Map<?, ?>) r).get("resourceId"))));
    var rows = (List<?>) a.call("GET", "/devices/" + id + "/commands", null, 200).get("items");
    assertTrue(rows.stream().anyMatch(r -> ((Map<?, ?>) r).get("status").equals("UNKNOWN")));
    assertTrue(rows.stream().anyMatch(r -> ((Map<?, ?>) r).get("status").equals("CANCELLED")));
    a.call("GET", "/dashboard/overview", null, 200);
    a.call("GET", "/audit-logs?action=DEVICE_REVOKED", null, 200);
    // Revoking again revokes no certificate, so it adds no certificate audit row.
    a.call("POST", "/devices/" + id + "/revoke", null, 200);
    assertEquals(
        1,
        count(
            "SELECT COUNT(*) FROM audit_logs WHERE action='DEVICE_CERTIFICATE_REVOKED'"
                + " AND resource_id=?",
            (String) e.get("serial")));
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
                  .header("Authorization", proofHeader(e, "POST", "/device-agent/commands/claim"))
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
                    new EnrollDeviceRequest(
                        (String) token.get("token"),
                        uid,
                        "Rollback device",
                        TestDevices.csr(TestDevices.newKey())))
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
  void enrollmentRequiresValidP256Csr() throws Exception {
    var a = login("alpha", "org_admin");
    var t = a.call("POST", "/enrollment-tokens", null, 200);
    String uid = UUID.randomUUID().toString();
    var p384 = java.security.KeyPairGenerator.getInstance("EC");
    p384.initialize(new java.security.spec.ECGenParameterSpec("secp384r1"));
    for (String csr : List.of("not a csr", TestDevices.csr(p384.generateKeyPair(), "CN=device"))) {
      assertEquals(
          "Invalid CSR",
          new Client()
              .call("POST", "/device-enrollments", enrollment(t.get("token"), uid, csr), 400)
              .get("message"));
    }
    // The rejected attempts left the token unused.
    var key = TestDevices.newKey();
    var e =
        new Client()
            .call(
                "POST",
                "/device-enrollments",
                enrollment(t.get("token"), uid, TestDevices.csr(key)),
                200);
    assertTrue(((String) e.get("certificate")).startsWith("-----BEGIN CERTIFICATE-----"));
    assertTrue(((String) e.get("caCertificate")).startsWith("-----BEGIN CERTIFICATE-----"));
    assertTrue(e.get("credential") == null);
    var issued =
        (List<?>)
            a.call("GET", "/audit-logs?action=DEVICE_CERTIFICATE_ISSUED&size=100", null, 200)
                .get("items");
    assertTrue(
        issued.stream().anyMatch(r -> e.get("serial").equals(((Map<?, ?>) r).get("resourceId"))));
  }

  @Test
  void deviceProofsBindKeyRequestTimeAndAreSingleUse() throws Exception {
    clock.now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    var a = login("alpha", "org_admin");
    var e = enroll(a);
    var key = (com.nimbusds.jose.jwk.ECKey) e.get("key");
    String serial = (String) e.get("serial");
    String heartbeat = "/api/v1/device-agent/heartbeat";
    long now = clock.millis();
    // Accepted: inside the window, both edges included.
    for (long iat : List.of(now, now - 60_000, now + 60_000)) {
      heartbeat(TestDevices.proof(key, serial, "POST", heartbeat, iat), 200);
    }
    // Rejected: outside the window, wrong method or target, wrong key, unknown serial, bearer.
    var other = TestDevices.newKey();
    for (String proof :
        List.of(
            TestDevices.proof(key, serial, "POST", heartbeat, now - 61_000),
            TestDevices.proof(key, serial, "POST", heartbeat, now + 61_000),
            TestDevices.proof(key, serial, "GET", heartbeat, now),
            TestDevices.proof(key, serial, "POST", "/api/v1/device-agent/commands/claim", now),
            TestDevices.proof(other, serial, "POST", heartbeat, now),
            TestDevices.proof(key, "00ff", "POST", heartbeat, now))) {
      heartbeat(proof, 401);
    }
    http.post()
        .uri("/device-agent/heartbeat")
        .header("Authorization", "Bearer d_not-a-device-credential")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(inventory(true))
        .exchange()
        .expectStatus()
        .isUnauthorized();

    // Single use: a replay fails, also after the first use failed in the business layer.
    String once = TestDevices.proof(key, serial, "POST", heartbeat, now);
    heartbeat(once, 200);
    heartbeat(once, 401);
    String result = "/device-agent/commands/" + UUID.randomUUID() + "/result";
    String failing = TestDevices.proof(key, serial, "POST", "/api/v1" + result, now);
    var body = Map.of("attemptId", "x", "status", "SUCCEEDED", "result", Map.of());
    for (int status : List.of(404, 401)) {
      http.post()
          .uri(result)
          .header("Authorization", "DeviceProof " + failing)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(body)
          .exchange()
          .expectStatus()
          .isEqualTo(status);
    }
    // Concurrent use of one proof: exactly one request gets through.
    String shared = TestDevices.proof(key, serial, "POST", heartbeat, now);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Integer> task = () -> heartbeatStatus(shared);
      var jobs = executor.invokeAll(List.of(task, task, task));
      var statuses = new java.util.ArrayList<Integer>();
      for (var job : jobs) {
        statuses.add(job.get());
      }
      assertEquals(1, statuses.stream().filter(code -> code == 200).count());
      assertEquals(2, statuses.stream().filter(code -> code == 401).count());
    }
    // Purging old ids never reopens the window: a future-dated proof stays used.
    String future = TestDevices.proof(key, serial, "POST", heartbeat, now + 60_000);
    heartbeat(future, 200);
    clock.advance(61);
    deviceService.purgeProofReplays().block();
    heartbeat(future, 401);
    clock.advance(60);
    deviceService.purgeProofReplays().block();
    heartbeat(future, 401);
  }

  @Test
  void deviceCertificateValidityBounds() {
    clock.now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    var a = login("alpha", "org_admin");
    var e = enroll(a);
    var d = device(e);
    long start = clock.now.getEpochSecond();
    clock.now = Instant.ofEpochSecond(start - 1);
    d.call("POST", "/device-agent/heartbeat", inventory(true), 401);
    clock.now = Instant.ofEpochSecond(start);
    d.call("POST", "/device-agent/heartbeat", inventory(true), 200);
    clock.now = Instant.ofEpochSecond(start + 2_592_000 - 1);
    d.call("POST", "/device-agent/heartbeat", inventory(true), 200);
    clock.now = Instant.ofEpochSecond(start + 2_592_000);
    d.call("POST", "/device-agent/heartbeat", inventory(true), 401);
  }

  void heartbeat(String proof, int status) {
    assertEquals(status, heartbeatStatus(proof));
  }

  int heartbeatStatus(String proof) {
    return http.post()
        .uri("/device-agent/heartbeat")
        .header("Authorization", "DeviceProof " + proof)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(inventory(true))
        .exchange()
        .returnResult(byte[].class)
        .getStatus()
        .value();
  }

  @Test
  void deviceFetchesSignedCommandKeys() throws Exception {
    var a = login("alpha", "ORG_ADMIN");
    var d = device(enroll(a));
    @SuppressWarnings("unchecked")
    var bundles =
        (List<String>) d.call("GET", "/device-agent/command-keys", null, 200).get("bundles");
    assertEquals(1, bundles.size());
    var bundle = com.nimbusds.jwt.SignedJWT.parse(bundles.get(0));
    assertEquals("mdm-trust+jwt", bundle.getHeader().getType().getType());
    assertTrue(
        bundle.verify(
            new com.nimbusds.jose.crypto.ECDSAVerifier(keys.getCommandKey().toPublicJWK())));
    a.call("GET", "/device-agent/command-keys", null, 403);
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
