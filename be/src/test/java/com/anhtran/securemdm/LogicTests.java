package com.anhtran.securemdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anhtran.securemdm.exception.CommonException;
import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.security.CommandSigner;
import com.anhtran.securemdm.security.DeviceCertificateAuthority;
import com.anhtran.securemdm.security.DeviceProofs;
import com.anhtran.securemdm.security.JwtTokenProvider;
import com.anhtran.securemdm.security.SigningKeys;
import com.anhtran.securemdm.src.command.service.rule.CommandStateMachine;
import com.anhtran.securemdm.src.device.dto.model.Inventory;
import com.anhtran.securemdm.src.policy.dto.model.PolicyConfig;
import com.anhtran.securemdm.src.policy.service.rule.ComplianceEvaluator;
import com.anhtran.securemdm.utils.JsonUtils;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LogicTests {
  @TempDir Path dir;

  @Test
  void commandTransitionsAndPayloads() {
    for (String terminal : List.of("SUCCEEDED", "FAILED", "UNKNOWN", "EXPIRED", "CANCELLED")) {
      assertEquals(terminal, CommandStateMachine.transition(terminal, 1, 100, 90, 80L, 3));
    }
    assertEquals("EXPIRED", CommandStateMachine.transition("PENDING", 0, 100, 100, null, 3));
    assertEquals("UNKNOWN", CommandStateMachine.transition("PENDING", 1, 100, 100, null, 3));
    assertEquals("PENDING", CommandStateMachine.transition("IN_PROGRESS", 1, 80, 100, 80L, 3));
    assertEquals("UNKNOWN", CommandStateMachine.transition("IN_PROGRESS", 3, 80, 100, 80L, 3));
    assertEquals("IN_PROGRESS", CommandStateMachine.transition("IN_PROGRESS", 1, 70, 100, 80L, 3));
    assertEquals("IN_PROGRESS", CommandStateMachine.transition("IN_PROGRESS", 1, 70, 100, null, 3));
    assertEquals("PENDING", CommandStateMachine.transition("PENDING", 0, 70, 100, null, 3));
    for (String type : List.of("REPORT_STATUS", "SYNC_DEVICE", "LOCK_DEVICE_SIMULATED")) {
      CommandStateMachine.validate(type, Map.of());
      assertThrows(Exception.class, () -> CommandStateMachine.validate(type, Map.of("extra", 1)));
    }
    CommandStateMachine.validate("SHOW_MESSAGE", Map.of("message", "Hello"));
    for (Map<String, Object> bad :
        List.of(
            Map.<String, Object>of(),
            Map.<String, Object>of("message", ""),
            Map.<String, Object>of("message", " "),
            Map.<String, Object>of("message", 5),
            Map.<String, Object>of("message", "x".repeat(501)))) {
      assertThrows(Exception.class, () -> CommandStateMachine.validate("SHOW_MESSAGE", bad));
    }
    assertThrows(Exception.class, () -> CommandStateMachine.validate("SHELL", Map.of()));
    assertThrows(Exception.class, () -> CommandStateMachine.validate("REPORT_STATUS", null));
    CommandStateMachine.validateResult("SHOW_MESSAGE", "SUCCEEDED", Map.of("displayed", true));
    CommandStateMachine.validateResult("SYNC_DEVICE", "SUCCEEDED", Map.of("synced", true));
    CommandStateMachine.validateResult(
        "LOCK_DEVICE_SIMULATED", "SUCCEEDED", Map.of("locked", true));
    CommandStateMachine.validateResult(
        "REPORT_STATUS",
        "SUCCEEDED",
        Map.of(
            "inventory",
            Map.of(
                "screenLockEnabled",
                true,
                "diskEncrypted",
                true,
                "androidApiLevel",
                35,
                "installedPackages",
                List.of("com.example"))));
    CommandStateMachine.validateResult(
        "REPORT_STATUS", "FAILED", Map.of("errorCode", "SIMULATED_FAILURE"));
    for (String status : List.of("SUCCEEDED", "FAILED", "UNKNOWN")) {
      assertThrows(
          Exception.class,
          () -> CommandStateMachine.validateResult("REPORT_STATUS", status, Map.of()));
    }
    assertThrows(
        Exception.class,
        () ->
            CommandStateMachine.validateResult(
                "REPORT_STATUS", "FAILED", Map.of("errorCode", "secret text")));
    assertThrows(
        Exception.class,
        () ->
            CommandStateMachine.validateResult(
                "SYNC_DEVICE", "SUCCEEDED", Map.of("synced", false)));
  }

  @Test
  void complianceIsEvaluatedFromInventory() {
    var rule = new PolicyConfig(true, true, 34, List.of("com.bad"));
    assertEquals(
        List.of(
            "SCREEN_LOCK_REQUIRED",
            "DISK_ENCRYPTION_REQUIRED",
            "OS_VERSION_TOO_LOW",
            "BLOCKED_PACKAGE_INSTALLED"),
        ComplianceEvaluator.reasons(rule, new Inventory(false, false, 30, List.of("com.bad"))));
    assertTrue(
        ComplianceEvaluator.reasons(rule, new Inventory(true, true, 35, List.of("com.good")))
            .isEmpty());
    assertTrue(
        ComplianceEvaluator.reasons(
                new PolicyConfig(false, false, 1, List.of()),
                new Inventory(false, false, 1, List.of()))
            .isEmpty());
  }

  @Test
  void jsonStoredBeforeDiskEncryptionStillReads() {
    var inventory =
        JsonUtils.read(
            "{\"screenLockEnabled\":true,\"androidApiLevel\":35,\"installedPackages\":[]}",
            Inventory.class);
    assertFalse(inventory.diskEncrypted());
    var policy =
        JsonUtils.read(
            "{\"requireScreenLock\":true,\"minAndroidApiLevel\":34,\"blockedPackages\":[]}",
            PolicyConfig.class);
    assertFalse(policy.requireDiskEncryption());
  }

  @Test
  void accessAndCommandKeysAreSeparatedAndExportJavaInteropFixtures() throws Exception {
    var access = new ECKeyGenerator(Curve.P_256).keyID("access-test").generate();
    var one = new ECKeyGenerator(Curve.P_256).keyID("command-old-test").generate();
    final var two = new ECKeyGenerator(Curve.P_256).keyID("command-new-test").generate();
    Path a = dir.resolve("a.json");
    Path c = dir.resolve("c.json");
    Files.writeString(a, access.toJSONString());
    Files.writeString(c, one.toJSONString());
    Clock clock = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);
    var keys = new SigningKeys(a.toString(), c.toString());
    var tokens = new JwtTokenProvider(keys, clock);
    var actor = new Actor("tenant-test", "user-test", "ORG_ADMIN", "USER", "session-test");
    String jwt = tokens.issue(actor);
    assertEquals(actor.id(), tokens.verify(jwt).getSubject());
    assertThrows(Exception.class, () -> tokens.verify("invalid"));
    assertThrows(Exception.class, () -> new SigningKeys(a.toString(), a.toString()));
    assertThrows(
        Exception.class,
        () ->
            new JwtTokenProvider(
                    new SigningKeys(a.toString(), c.toString()),
                    Clock.offset(clock, Duration.ofMinutes(11)))
                .verify(jwt));
    String command =
        new CommandSigner(keys, clock)
            .sign(
                "command-one",
                "tenant-test",
                "device-test",
                "SHOW_MESSAGE",
                Map.of("message", "Java to TypeScript"),
                clock.millis() + 600000);
    assertThrows(Exception.class, () -> tokens.verify(command));
    var parsed = SignedJWT.parse(command);
    assertTrue(parsed.verify(new ECDSAVerifier(one.toPublicJWK())));
    assertFalse(parsed.verify(new ECDSAVerifier(access.toPublicJWK())));
    Files.writeString(c, two.toJSONString());
    final var rotated = new CommandSigner(new SigningKeys(a.toString(), c.toString()), clock);
    String next =
        rotated.sign(
            "command-two",
            "tenant-test",
            "device-test",
            "LOCK_DEVICE_SIMULATED",
            Map.of(),
            clock.millis() + 600000);
    Path c1 = dir.resolve("c1.json");
    Files.writeString(c1, one.toJSONString());
    var switched =
        new SigningKeys(a.toString(), List.of(c1.toString(), c.toString()), two.getKeyID());
    List<String> bundles = new CommandSigner(switched, clock).trustBundles();
    Map<String, Object> fixture =
        Map.of(
            "purpose",
            "TEST ONLY: Java Nimbus to TypeScript jose interoperability; public keys only",
            "at",
            "2030-01-01T00:00:01Z",
            "jwks",
            new JWKSet(List.of(one.toPublicJWK(), two.toPublicJWK())).toJSONObject(),
            "commands",
            List.of(command, next),
            "bundles",
            bundles);
    Files.createDirectories(Path.of("target"));
    Files.writeString(Path.of("target/java-commands.json"), JsonUtils.write(fixture));
  }

  @Test
  void commandKeySetSignsWithActiveKeyAndPublishesSignedTrustBundles() throws Exception {
    var access = new ECKeyGenerator(Curve.P_256).keyID("access-test").generate();
    var one = new ECKeyGenerator(Curve.P_256).keyID("command-1").generate();
    var two = new ECKeyGenerator(Curve.P_256).keyID("command-2").generate();
    Path a = dir.resolve("access.json");
    Path c1 = dir.resolve("command-1.json");
    Path c2 = dir.resolve("command-2.json");
    Files.writeString(a, access.toJSONString());
    Files.writeString(c1, one.toJSONString());
    Files.writeString(c2, two.toJSONString());
    List<String> both = List.of(c1.toString(), c2.toString());
    Clock clock = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);

    var publish = new SigningKeys(a.toString(), both, "command-1");
    assertEquals("command-1", publish.getCommandKey().getKeyID());
    var signer = new CommandSigner(publish, clock);
    String command = signer.sign("c", "t", "d", "SYNC_DEVICE", Map.of(), clock.millis() + 1000);
    assertEquals("command-1", SignedJWT.parse(command).getHeader().getKeyID());

    List<String> bundles = signer.trustBundles();
    assertEquals(2, bundles.size());
    for (int i = 0; i < 2; i++) {
      var bundle = SignedJWT.parse(bundles.get(i));
      var key = i == 0 ? one : two;
      assertEquals(key.getKeyID(), bundle.getHeader().getKeyID());
      assertEquals("mdm-trust+jwt", bundle.getHeader().getType().getType());
      assertTrue(bundle.verify(new ECDSAVerifier(key.toPublicJWK())));
      var published = JWKSet.parse(Map.of("keys", bundle.getJWTClaimsSet().getClaim("keys")));
      assertEquals(
          List.of("command-1", "command-2"),
          published.getKeys().stream().map(k -> k.getKeyID()).toList());
      assertTrue(published.getKeys().stream().noneMatch(k -> k.isPrivate()));
      assertTrue(
          bundle
              .getJWTClaimsSet()
              .getExpirationTime()
              .after(bundle.getJWTClaimsSet().getIssueTime()));
    }

    // Single key: active defaults to it. Several keys: active must be named and present.
    assertEquals(
        "command-2",
        new SigningKeys(a.toString(), List.of(c2.toString()), "").getCommandKey().getKeyID());
    assertThrows(Exception.class, () -> new SigningKeys(a.toString(), both, ""));
    assertThrows(Exception.class, () -> new SigningKeys(a.toString(), both, "command-3"));
    assertThrows(
        Exception.class,
        () -> new SigningKeys(a.toString(), List.of(c1.toString(), c1.toString()), "command-1"));
    assertThrows(
        Exception.class, () -> new SigningKeys(a.toString(), List.of(a.toString()), "access-test"));
  }

  @Test
  void deviceCaIssuesClientCertificatesOnlyFromValidP256Csrs() throws Exception {
    var access = new ECKeyGenerator(Curve.P_256).keyID("access-test").generate();
    var command = new ECKeyGenerator(Curve.P_256).keyID("command-test").generate();
    var caKey = new ECKeyGenerator(Curve.P_256).keyID("device-ca").generate();
    Path a = dir.resolve("ca-access.json");
    Path c = dir.resolve("ca-command.json");
    Files.writeString(a, access.toJSONString());
    Files.writeString(c, command.toJSONString());
    var keys = new SigningKeys(a.toString(), c.toString());
    var ca = new DeviceCertificateAuthority(caKey, keys);
    assertThrows(Exception.class, () -> new DeviceCertificateAuthority(command, keys));

    var device = TestDevices.newKey();
    long notBefore = 1_893_456_000_123L;
    var issued =
        ca.issue(
            TestDevices.csr(device),
            "tenant-test",
            "device-test",
            notBefore,
            notBefore + 86_400_000L);
    var factory = java.security.cert.CertificateFactory.getInstance("X.509");
    var cert =
        (java.security.cert.X509Certificate)
            factory.generateCertificate(
                new java.io.ByteArrayInputStream(
                    issued.pem().getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
    var caCert =
        (java.security.cert.X509Certificate)
            factory.generateCertificate(
                new java.io.ByteArrayInputStream(
                    ca.caCertificatePem().getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
    cert.verify(caCert.getPublicKey());
    caCert.verify(caCert.getPublicKey());
    assertEquals(caCert.getSubjectX500Principal(), cert.getIssuerX500Principal());
    assertTrue(caCert.getBasicConstraints() >= 0);
    assertTrue(caCert.getSerialNumber().toByteArray().length <= 20);
    assertTrue(cert.getSerialNumber().toByteArray().length <= 20);
    assertEquals(-1, cert.getBasicConstraints());
    assertEquals("CN=device-test,O=tenant-test", cert.getSubjectX500Principal().getName());
    assertEquals(List.of("1.3.6.1.5.5.7.3.2"), cert.getExtendedKeyUsage());
    assertTrue(cert.getKeyUsage()[0]);
    assertEquals(issued.serial(), cert.getSerialNumber().toString(16));
    assertTrue(cert.getSerialNumber().bitLength() > 64);
    assertEquals(1_893_456_000_000L, issued.notBefore());
    assertEquals(issued.notBefore(), cert.getNotBefore().getTime());
    assertEquals(issued.notAfter(), cert.getNotAfter().getTime());
    assertEquals(
        device.toPublicJWK().computeThumbprint(),
        com.nimbusds.jose.jwk.ECKey.parse(issued.publicKeyJwk()).computeThumbprint());
    assertFalse(issued.publicKeyJwk().contains("\"d\""));
    assertEquals(64, issued.fingerprint().length());

    var p384 = java.security.KeyPairGenerator.getInstance("EC");
    p384.initialize(new java.security.spec.ECGenParameterSpec("secp384r1"));
    assertThrows(
        CommonException.class,
        () -> ca.issue(TestDevices.csr(p384.generateKeyPair(), "CN=x"), "t", "d", 0, 1000));
    String good = TestDevices.csr(device);
    String tampered =
        good.replace(good.substring(80, 84), good.charAt(80) == 'A' ? "BBBB" : "AAAA");
    assertThrows(CommonException.class, () -> ca.issue(tampered, "t", "d", 0, 1000));
    assertThrows(CommonException.class, () -> ca.issue("not a csr", "t", "d", 0, 1000));
    String sha384 = TestDevices.csr(device.toKeyPair(), "CN=x", "SHA384withECDSA");
    assertThrows(CommonException.class, () -> ca.issue(sha384, "t", "d", 0, 1000));
  }

  @Test
  void deviceProofsBindKeyMethodTargetAndTime() throws Exception {
    var key = TestDevices.newKey();
    long now = 1_893_456_000_000L;
    String target = "/api/v1/device-agent/heartbeat";
    var ok =
        DeviceProofs.verify(
            DeviceProofs.parse(TestDevices.proof(key, "ab12", "POST", target, now, "jti-1")),
            key.toPublicJWK(),
            "POST",
            target,
            now);
    assertEquals("jti-1", ok.jti());
    assertEquals(now, ok.iat());
    assertEquals(
        "ab12",
        DeviceProofs.parse(TestDevices.proof(key, "ab12", "POST", target, now))
            .getHeader()
            .getKeyID());
    for (long iat : List.of(now - 60_000, now + 60_000)) {
      DeviceProofs.verify(
          DeviceProofs.parse(TestDevices.proof(key, "ab12", "POST", target, iat)),
          key.toPublicJWK(),
          "POST",
          target,
          now);
    }
    var other = TestDevices.newKey();
    List<String> rejected =
        List.of(
            TestDevices.proof(key, "ab12", "POST", target, now - 61_000),
            TestDevices.proof(key, "ab12", "POST", target, now + 61_000),
            TestDevices.proof(key, "ab12", "GET", target, now),
            TestDevices.proof(key, "ab12", "POST", "/api/v1/device-agent/commands/claim", now),
            TestDevices.proof(other, "ab12", "POST", target, now),
            TestDevices.proof(key, "ab12", "POST", target, now, ""));
    for (String proof : rejected) {
      assertThrows(
          CommonException.class,
          () ->
              DeviceProofs.verify(
                  DeviceProofs.parse(proof), key.toPublicJWK(), "POST", target, now));
    }
    String command =
        new CommandSigner(
                new SigningKeys(writeKeys().get(0), writeKeys().get(1)), Clock.systemUTC())
            .sign("c", "t", "d", "SYNC_DEVICE", Map.of(), System.currentTimeMillis() + 1000);
    assertThrows(CommonException.class, () -> DeviceProofs.parse(command));
    assertThrows(CommonException.class, () -> DeviceProofs.parse("garbage"));
  }

  private List<String> writeKeys() throws Exception {
    Path a = Files.createTempFile(dir, "a", ".json");
    Path c = Files.createTempFile(dir, "c", ".json");
    Files.writeString(a, new ECKeyGenerator(Curve.P_256).keyID("a").generate().toJSONString());
    Files.writeString(c, new ECKeyGenerator(Curve.P_256).keyID("c").generate().toJSONString());
    return List.of(a.toString(), c.toString());
  }
}
