package com.anhtran.securemdm.src.command.config;

import com.anhtran.securemdm.src.command.service.CommandService;
import com.anhtran.securemdm.src.device.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Mono;

/**
 * Reconciles expired commands and leases and purges old proof ids; disabled in tests via
 * mdm.scheduling-enabled=false.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mdm.scheduling-enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
  private final CommandService commandService;
  private final DeviceService deviceService;

  @Scheduled(fixedDelayString = "${mdm.sweep-ms:5000}")
  public Mono<Void> sweepCommands() {
    return Mono.defer(commandService::sweep);
  }

  @Scheduled(fixedDelayString = "${mdm.sweep-ms:5000}")
  public Mono<Void> purgeProofReplays() {
    return Mono.defer(deviceService::purgeProofReplays);
  }
}
