package com.anhtran.securemdm.utils;

import java.util.UUID;

public final class IdUtils {
  private IdUtils() {}

  public static String newId() {
    return UUID.randomUUID().toString();
  }
}
