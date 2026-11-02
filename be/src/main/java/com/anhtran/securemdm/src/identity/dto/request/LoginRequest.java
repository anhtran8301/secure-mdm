package com.anhtran.securemdm.src.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Size(max = 80) String organizationSlug,
    @Email @NotBlank @Size(max = 180) String email,
    @NotBlank @Size(max = 72) String password) {}
