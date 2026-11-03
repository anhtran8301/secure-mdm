package com.anhtran.securemdm.src.device.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnrollDeviceRequest(
    @NotBlank @Size(max = 100) String token,
    @NotBlank @Size(max = 100) String deviceUid,
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 4096) String csr) {}
