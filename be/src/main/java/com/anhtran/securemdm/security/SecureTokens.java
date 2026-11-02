package com.anhtran.securemdm.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Random opaque tokens (stored only as SHA-256 hashes) and their hashing. */
public final class SecureTokens {
  private static final SecureRandom RANDOM = new SecureRandom();

  private SecureTokens() {}

  public static String generate(String prefix) {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
