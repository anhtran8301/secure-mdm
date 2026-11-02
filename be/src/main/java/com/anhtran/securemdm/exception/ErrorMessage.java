package com.anhtran.securemdm.exception;

import org.springframework.http.HttpStatus;

public record ErrorMessage(String code, String message, String status) {
  public ErrorMessage(String code, String message, HttpStatus status) {
    this(code, message, status.name());
  }

  public static ErrorMessage of(ErrorCode error) {
    return new ErrorMessage(error.getCode(), error.getDefaultMessage(), error.getStatus());
  }
}
