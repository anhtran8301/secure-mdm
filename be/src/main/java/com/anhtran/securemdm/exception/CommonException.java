package com.anhtran.securemdm.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CommonException extends RuntimeException {
  private final String code;
  private final HttpStatus status;

  public CommonException(String code, String message, HttpStatus status) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public CommonException(ErrorCode error, String message) {
    this(error.getCode(), message, error.getStatus());
  }

  public CommonException(ErrorCode error) {
    this(error, error.getDefaultMessage());
  }

  public static CommonException badRequest(String message) {
    return new CommonException(ErrorCode.BAD_REQUEST, message);
  }

  public static CommonException unauthorized() {
    return new CommonException(ErrorCode.UNAUTHORIZED);
  }

  public static CommonException forbidden(String message) {
    return new CommonException(ErrorCode.FORBIDDEN, message);
  }

  public static CommonException notFound() {
    return new CommonException(ErrorCode.NOT_FOUND);
  }

  public static CommonException conflict(String message) {
    return new CommonException(ErrorCode.CONFLICT, message);
  }
}
