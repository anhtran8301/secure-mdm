package com.anhtran.securemdm.src.policy.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyReportResponse {
  private String compliance;
  private List<String> reasons;
  private Long revision;
}
