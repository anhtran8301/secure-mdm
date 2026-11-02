package com.anhtran.securemdm.utils;

import com.anhtran.securemdm.exception.CommonException;

public final class PageUtils {
  private PageUtils() {}

  public static int size(int size) {
    if (size < 1 || size > AppConstants.MAX_PAGE_SIZE) {
      throw CommonException.badRequest("Size must be between 1 and " + AppConstants.MAX_PAGE_SIZE);
    }
    return size;
  }

  public static long offset(int page, int size) {
    if (page < 0) {
      throw CommonException.badRequest("Page must be non-negative");
    }
    return (long) page * size(size);
  }

  /** Escapes LIKE wildcards; queries must declare {@code ESCAPE '!'}. */
  public static String containsPattern(String search) {
    return "%" + search.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
  }
}
