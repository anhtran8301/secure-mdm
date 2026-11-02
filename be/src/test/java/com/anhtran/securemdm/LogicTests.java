package com.anhtran.securemdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.security.CommandSigner;
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
    var rule = new PolicyConfig(true, 34, List.of("com.bad"));
    assertEquals(
        3, ComplianceEvaluator.reasons(rule, new Inventory(false, 30, List.of("com.bad"))).size());
    assertTrue(
        ComplianceEvaluator.reasons(rule, new Inventory(true, 35, List.of("com.good"))).isEmpty());
    assertTrue(
        ComplianceEvaluator.reasons(
                new PolicyConfig(false, 1, List.of()), new Inventory(false, 1, List.of()))
            .isEmpty());
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
    Map<String, Object> fixture =
        Map.of(
            "purpose",
            "TEST ONLY: Java Nimbus to TypeScript jose interoperability; public keys only",
            "at",
            "2030-01-01T00:00:01Z",
            "jwks",
            new JWKSet(List.of(one.toPublicJWK(), two.toPublicJWK())).toJSONObject(),
            "commands",
            List.of(command, next));
    Files.createDirectories(Path.of("target"));
    Files.writeString(Path.of("target/java-commands.json"), JsonUtils.write(fixture));
  }
}
