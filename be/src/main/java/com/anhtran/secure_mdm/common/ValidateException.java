package com.anhtran.secure_mdm.common;

import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ValidateException extends RuntimeException {
    private final String code;
    private final Map<String, String> messageMap;
    private final HttpStatus status;

    public ValidateException(String code, Map<String, String> messageMap, HttpStatus status) {
        super("Validation failed: " + code);
        this.code = code;
        this.messageMap = Map.copyOf(messageMap);
        this.status = status;
    }
}
