package com.anhtran.securemdm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;
import reactor.test.StepVerifier;

@SpringBootTest
class SecureMdmApplicationTests {

  static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

  static {
    MYSQL.start();
  }

  static final java.nio.file.Path KEYS;

  static {
    try {
      KEYS = java.nio.file.Files.createTempDirectory("mdm-test-keys");
      for (String name : java.util.List.of("access", "command", "device-ca")) {
        java.nio.file.Files.writeString(
            KEYS.resolve(name + ".json"),
            new com.nimbusds.jose.jwk.gen.ECKeyGenerator(com.nimbusds.jose.jwk.Curve.P_256)
                .keyID(name)
                .generate()
                .toJSONString());
      }
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    properties.add("mdm.access-key", () -> KEYS.resolve("access.json").toString());
    properties.add("mdm.command-keys", () -> KEYS.resolve("command.json").toString());
    properties.add("mdm.device-ca-key", () -> KEYS.resolve("device-ca.json").toString());
    properties.add("debug", () -> "false");
    properties.add("mdm.scheduling-enabled", () -> "false");
    properties.add(
        "spring.r2dbc.url",
        () ->
            "r2dbc:mysql://"
                + MYSQL.getHost()
                + ":"
                + MYSQL.getMappedPort(3306)
                + "/"
                + MYSQL.getDatabaseName());
    properties.add("spring.r2dbc.username", MYSQL::getUsername);
    properties.add("spring.r2dbc.password", MYSQL::getPassword);
    properties.add("spring.liquibase.url", MYSQL::getJdbcUrl);
    properties.add("spring.liquibase.user", MYSQL::getUsername);
    properties.add("spring.liquibase.password", MYSQL::getPassword);
  }

  @Autowired DatabaseClient database;

  @Test
  void contextLoads() {
    StepVerifier.create(
            database
                .sql("SELECT COUNT(*) AS n FROM DATABASECHANGELOGLOCK")
                .map(row -> row.get("n", Long.class))
                .one())
        .expectNext(1L)
        .verifyComplete();
  }
}
