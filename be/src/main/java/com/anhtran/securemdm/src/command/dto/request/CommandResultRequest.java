package com.anhtran.securemdm.src.command.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CommandResultRequest(
    @NotBlank String attemptId, @NotBlank String status, @NotNull Map<String, Object> result) {}
