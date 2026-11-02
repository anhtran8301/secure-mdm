package com.anhtran.securemdm.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad request"),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication required"),
  FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied"),
  NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
  CONFLICT(HttpStatus.CONFLICT, "Conflict"),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "9999");

  private final HttpStatus status;
  private final String defaultMessage;
  private final String code;

  ErrorCode(HttpStatus status, String defaultMessage) {
    this(status, defaultMessage, "MDM" + status.value());
  }

  ErrorCode(HttpStatus status, String defaultMessage, String code) {
    this.status = status;
    this.defaultMessage = defaultMessage;
    this.code = code;
  }
}
