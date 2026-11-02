package com.anhtran.securemdm.src.command.service.impl;

import com.anhtran.securemdm.src.command.repository.DeviceCommandRepository;
import com.anhtran.securemdm.src.device.service.DeviceRevocationListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Undelivered commands are cancelled; delivered-but-unfinished ones become UNKNOWN. */
@Component
@RequiredArgsConstructor
public class CommandRevocationListener implements DeviceRevocationListener {
  private final DeviceCommandRepository commandRepository;

  @Override
  public Mono<Void> onDeviceRevoked(String organizationId, String deviceId) {
    return commandRepository.cancelForDevice(organizationId, deviceId).then();
  }
}
