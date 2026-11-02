package com.anhtran.securemdm.src.policy.dto.response;

import com.anhtran.securemdm.src.policy.dto.model.PolicyConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponse {
  private String id;
  private String organizationId;
  private String name;
  private Long createdAt;
  private String versionId;
  private Integer version;
  private PolicyConfig config;
}
