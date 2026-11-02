package com.anhtran.securemdm.src.policy.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AssignPolicyRequest(@NotBlank String versionId) {}
