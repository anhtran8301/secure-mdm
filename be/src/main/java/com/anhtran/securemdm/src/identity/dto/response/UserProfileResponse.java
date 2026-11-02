package com.anhtran.securemdm.src.identity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
  private String id;
  private String organizationId;
  private String organizationSlug;
  private String role;
}
