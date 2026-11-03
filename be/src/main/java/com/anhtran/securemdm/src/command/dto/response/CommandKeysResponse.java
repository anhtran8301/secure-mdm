package com.anhtran.securemdm.src.command.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One signed Trust Bundle per published command key (ADR 0006). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandKeysResponse {
  private List<String> bundles;
}
