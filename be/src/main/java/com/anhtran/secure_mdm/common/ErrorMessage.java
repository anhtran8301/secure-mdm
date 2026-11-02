package com.anhtran.secure_mdm.common;

import org.springframework.http.HttpStatus;

public record ErrorMessage(String code, String message, String status) {
    public ErrorMessage(String code, String message, HttpStatus status) {
        this(code, message, status.name());
    }
}
