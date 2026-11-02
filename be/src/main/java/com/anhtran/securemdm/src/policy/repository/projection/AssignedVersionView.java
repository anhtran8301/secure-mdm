package com.anhtran.securemdm.src.policy.repository.projection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Policy version currently assigned to a device, with the assignment revision. */
@Getter
@Setter
@NoArgsConstructor
public class AssignedVersionView {
  private String id;
  private String organizationId;
  private String policyId;
  private Integer version;
  private String configJson;
  private Long createdAt;
  private Long revision;
}
