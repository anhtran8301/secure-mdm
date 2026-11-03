package com.anhtran.securemdm.utils;

public final class AppConstants {
  public static final String DEFAULT_PAGE_NUMBER = "0";
  public static final String DEFAULT_PAGE_SIZE = "20";
  public static final int MAX_PAGE_SIZE = 100;

  public static final String API_PREFIX = "/api/v1";
  public static final String REFRESH_COOKIE = "mdm_refresh";
  public static final String CORRELATION_ID = "correlationId";

  public static final String REFRESH_TOKEN_PREFIX = "r_";
  public static final String ENROLLMENT_TOKEN_PREFIX = "e_";

  public static final long SESSION_TTL_MS = 604_800_000L;
  public static final long ACCESS_TOKEN_TTL_MS = 600_000L;
  public static final long ENROLLMENT_TOKEN_TTL_MS = 900_000L;
  public static final long DEVICE_CERTIFICATE_TTL_MS = 2_592_000_000L;

  private AppConstants() {}
}
