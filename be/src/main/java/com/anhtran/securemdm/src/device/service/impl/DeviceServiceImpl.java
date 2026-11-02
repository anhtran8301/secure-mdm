package com.anhtran.securemdm.src.device.service.impl;

import com.anhtran.securemdm.common.dto.PageResponse;
import com.anhtran.securemdm.config.MdmProperties;
import com.anhtran.securemdm.exception.CommonException;
import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.audit.service.AuditService;
import com.anhtran.securemdm.src.device.dto.model.Inventory;
import com.anhtran.securemdm.src.device.dto.response.DeviceResponse;
import com.anhtran.securemdm.src.device.entity.enums.DeviceStatus;
import com.anhtran.securemdm.src.device.mapper.DeviceMapper;
import com.anhtran.securemdm.src.device.repository.DeviceCredentialRepository;
import com.anhtran.securemdm.src.device.repository.DeviceRepository;
import com.anhtran.securemdm.src.device.service.DeviceRevocationListener;
import com.anhtran.securemdm.src.device.service.DeviceService;
import com.anhtran.securemdm.utils.JsonUtils;
import com.anhtran.securemdm.utils.PageUtils;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {
  private static final int MAX_SEARCH_LENGTH = 120;

  private final DeviceRepository deviceRepository;
  private final DeviceCredentialRepository credentialRepository;
  private final List<DeviceRevocationListener> revocationListeners;
  private final AuditService auditService;
  private final DeviceMapper deviceMapper;
  private final MdmProperties properties;
  private final TransactionalOperator transactions;
  private final Clock clock;

  @Override
  public Mono<Void> heartbeat(Actor device, Inventory inventory) {
    String org = device.organizationId();
    return deviceRepository
        .lockExisting(org, device.id())
        .flatMap(
            d ->
                DeviceStatus.ACTIVE.name().equals(d.getStatus())
                    ? deviceRepository
                        .updateInventory(
                            org, device.id(), JsonUtils.write(inventory), clock.millis())
                        .then()
                    : Mono.<Void>error(CommonException.unauthorized()))
        .as(transactions::transactional);
  }

  @Override
  public Mono<Void> revoke(Actor actor, String deviceId) {
    return Mono.defer(
        () -> {
          actor.requireAdmin();
          String org = actor.organizationId();
          return deviceRepository
              .lockExisting(org, deviceId)
              .then(deviceRepository.markRevoked(org, deviceId))
              .then(credentialRepository.revokeForDevice(org, deviceId, clock.millis()))
              .then(notifyRevoked(org, deviceId))
              .then(auditService.record(actor, "DEVICE_REVOKED", "device", deviceId))
              .as(transactions::transactional);
        });
  }

  @Override
  public Mono<DeviceResponse> get(Actor actor, String deviceId) {
    return deviceRepository
        .findView(actor.organizationId(), deviceId, offlineCutoff())
        .switchIfEmpty(Mono.error(CommonException.notFound()))
        .map(deviceMapper::toResponse);
  }

  @Override
  public Mono<PageResponse<DeviceResponse>> list(Actor actor, String search, int page, int size) {
    return Mono.defer(
        () -> {
          long offset = PageUtils.offset(page, size);
          if (search.length() > MAX_SEARCH_LENGTH) {
            throw CommonException.badRequest("Search too long");
          }
          String org = actor.organizationId();
          String pattern = PageUtils.containsPattern(search);
          return Mono.zip(
                  deviceRepository
                      .findViews(org, pattern, offlineCutoff(), size, offset)
                      .map(deviceMapper::toResponse)
                      .collectList(),
                  deviceRepository.countByNamePattern(org, pattern))
              .map(p -> PageResponse.of(p.getT1(), p.getT2(), page, size));
        });
  }

  private Mono<Void> notifyRevoked(String organizationId, String deviceId) {
    return Flux.fromIterable(revocationListeners)
        .concatMap(listener -> listener.onDeviceRevoked(organizationId, deviceId))
        .then();
  }

  private long offlineCutoff() {
    return clock.millis() - properties.offlineMs();
  }
}
