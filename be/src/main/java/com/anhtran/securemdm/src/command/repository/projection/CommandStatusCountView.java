package com.anhtran.securemdm.src.command.repository.projection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Dashboard command count per status. */
@Getter
@Setter
@NoArgsConstructor
public class CommandStatusCountView {
  private String status;
  private Long total;
}
