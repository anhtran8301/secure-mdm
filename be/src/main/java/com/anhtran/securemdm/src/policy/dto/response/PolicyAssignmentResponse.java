package com.anhtran.securemdm.src.policy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyAssignmentResponse {
  private Long revision;
  private PolicyVersionResponse policy;
}
