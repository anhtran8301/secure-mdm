package com.anhtran.securemdm.src.command.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CreateCommandRequest(@NotBlank String type, @NotNull Map<String, Object> payload) {}
