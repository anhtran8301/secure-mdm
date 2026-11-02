package com.anhtran.securemdm.src.command.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandClaimResponse {
  private String commandId;
  private String attemptId;
  private Long leaseUntil;
  private String signedCommand;
}
