package com.anhtran.securemdm.src.policy.dto.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Immutable rule set stored as JSON in policy_versions.config_json. */
public record PolicyConfig(
    boolean requireScreenLock,
    boolean requireDiskEncryption,
    @Min(1) @Max(100) int minAndroidApiLevel,
    @NotNull @Size(max = 200) List<@NotBlank @Size(max = 180) String> blockedPackages) {}
