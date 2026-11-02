package com.anhtran.securemdm.src.policy.repository.projection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Policy with its latest version. */
@Getter
@Setter
@NoArgsConstructor
public class PolicyView {
  private String id;
  private String organizationId;
  private String name;
  private Long createdAt;
  private String versionId;
  private Integer version;
  private String configJson;
}
