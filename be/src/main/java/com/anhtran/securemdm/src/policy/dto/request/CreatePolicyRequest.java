package com.anhtran.securemdm.src.policy.dto.request;

import com.anhtran.securemdm.src.policy.dto.model.PolicyConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePolicyRequest(
    @NotBlank @Size(max = 120) String name, @Valid @NotNull PolicyConfig config) {}
