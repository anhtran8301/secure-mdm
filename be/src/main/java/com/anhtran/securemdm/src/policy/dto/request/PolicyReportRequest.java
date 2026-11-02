package com.anhtran.securemdm.src.policy.dto.request;

import com.anhtran.securemdm.src.device.dto.model.Inventory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PolicyReportRequest(
    @Min(1) long revision, @NotBlank String versionId, @Valid @NotNull Inventory inventory) {}
