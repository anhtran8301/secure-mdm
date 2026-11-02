package com.anhtran.securemdm.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.Map;

/** Canonical JSON (sorted map keys) so request hashes and result comparisons are stable. */
public final class JsonUtils {
  private static final ObjectMapper MAPPER =
      new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

  private JsonUtils() {}

  public static String write(Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JSON value", e);
    }
  }

  public static Map<String, Object> readObject(String value) {
    return read(value, new TypeReference<>() {});
  }

  public static <T> T read(String value, Class<T> type) {
    if (value == null) {
      return null;
    }
    try {
      return MAPPER.readValue(value, type);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JSON", e);
    }
  }

  public static <T> T read(String value, TypeReference<T> type) {
    if (value == null) {
      return null;
    }
    try {
      return MAPPER.readValue(value, type);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JSON", e);
    }
  }
}
