package com.anhtran.securemdm.src.device.dto.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Device state reported by the agent (heartbeat, policy report). */
public record Inventory(
    boolean screenLockEnabled,
    @Min(1) @Max(100) int androidApiLevel,
    @NotNull @Size(max = 200) List<@NotBlank @Size(max = 180) String> installedPackages) {}
