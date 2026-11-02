package com.anhtran.securemdm.src.device.service.impl;

import com.anhtran.securemdm.exception.CommonException;
import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.security.ActorKind;
import com.anhtran.securemdm.security.Role;
import com.anhtran.securemdm.security.SecureTokens;
import com.anhtran.securemdm.src.audit.service.AuditService;
import com.anhtran.securemdm.src.device.dto.request.EnrollDeviceRequest;
import com.anhtran.securemdm.src.device.dto.response.DeviceEnrollmentResponse;
import com.anhtran.securemdm.src.device.dto.response.EnrollmentTokenResponse;
import com.anhtran.securemdm.src.device.entity.Device;
import com.anhtran.securemdm.src.device.entity.DeviceCredential;
import com.anhtran.securemdm.src.device.entity.EnrollmentToken;
import com.anhtran.securemdm.src.device.entity.enums.DeviceStatus;
import com.anhtran.securemdm.src.device.repository.DeviceCredentialRepository;
import com.anhtran.securemdm.src.device.repository.DeviceRepository;
import com.anhtran.securemdm.src.device.repository.EnrollmentTokenRepository;
import com.anhtran.securemdm.src.device.service.EnrollmentService;
import com.anhtran.securemdm.utils.AppConstants;
import com.anhtran.securemdm.utils.IdUtils;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {
  private static final String DEFAULT_PLATFORM = "ANDROID_SIMULATED";

  private final EnrollmentTokenRepository tokenRepository;
  private final DeviceRepository deviceRepository;
  private final DeviceCredentialRepository credentialRepository;
  private final AuditService auditService;
  private final TransactionalOperator transactions;
  private final Clock clock;

  @Override
  public Mono<EnrollmentTokenResponse> issue(Actor actor) {
    return Mono.defer(
        () -> {
          actor.requireAdmin();
          String id = IdUtils.newId();
          String secret = SecureTokens.generate(AppConstants.ENROLLMENT_TOKEN_PREFIX);
          long now = clock.millis();
          long expiresAt = now + AppConstants.ENROLLMENT_TOKEN_TTL_MS;
          return tokenRepository
              .save(
                  EnrollmentToken.builder()
                      .id(id)
                      .organizationId(actor.organizationId())
                      .tokenHash(SecureTokens.hash(secret))
                      .expiresAt(expiresAt)
                      .createdAt(now)
                      .newEntity(true)
                      .build())
              .then(auditService.record(actor, "ENROLLMENT_ISSUED", "enrollment", id))
              .thenReturn(new EnrollmentTokenResponse(id, secret, expiresAt))
              .as(transactions::transactional);
        });
  }

  @Override
  public Mono<Void> revoke(Actor actor, String tokenId) {
    return Mono.defer(
        () -> {
          actor.requireAdmin();
          return tokenRepository
              .revoke(actor.organizationId(), tokenId, clock.millis())
              .flatMap(
                  updated ->
                      updated == 0
                          ? Mono.<Void>error(CommonException.notFound())
                          : auditService.record(actor, "ENROLLMENT_REVOKED", "enrollment", tokenId))
              .as(transactions::transactional);
        });
  }

  @Override
  public Mono<DeviceEnrollmentResponse> enroll(EnrollDeviceRequest request) {
    return tokenRepository
        .lockByTokenHash(SecureTokens.hash(request.token()))
        .switchIfEmpty(Mono.error(CommonException.unauthorized()))
        .flatMap(token -> consume(token, request))
        .as(transactions::transactional)
        .onErrorMap(
            DataIntegrityViolationException.class,
            ex -> CommonException.conflict("Device UID already enrolled in this organization"));
  }

  private Mono<DeviceEnrollmentResponse> consume(
      EnrollmentToken token, EnrollDeviceRequest request) {
    long now = clock.millis();
    if (token.getUsedAt() != null || token.getRevokedAt() != null || token.getExpiresAt() <= now) {
      return Mono.error(CommonException.unauthorized());
    }
    String org = token.getOrganizationId();
    String deviceId = IdUtils.newId();
    String credential = SecureTokens.generate(AppConstants.DEVICE_CREDENTIAL_PREFIX);
    long expiresAt = now + AppConstants.DEVICE_CREDENTIAL_TTL_MS;
    return tokenRepository
        .markUsed(token.getId(), now)
        .then(
            deviceRepository.save(
                Device.builder()
                    .id(deviceId)
                    .organizationId(org)
                    .deviceUid(request.deviceUid())
                    .name(request.name())
                    .platform(DEFAULT_PLATFORM)
                    .status(DeviceStatus.ACTIVE.name())
                    .createdAt(now)
                    .inventoryJson("{}")
                    .assignmentRevision(0L)
                    .newEntity(true)
                    .build()))
        .then(
            credentialRepository.save(
                DeviceCredential.builder()
                    .id(IdUtils.newId())
                    .organizationId(org)
                    .deviceId(deviceId)
                    .secretHash(SecureTokens.hash(credential))
                    .expiresAt(expiresAt)
                    .newEntity(true)
                    .build()))
        .then(
            auditService.record(
                new Actor(org, deviceId, Role.DEVICE.name(), ActorKind.DEVICE.name(), null),
                "DEVICE_ENROLLED",
                "device",
                deviceId))
        .thenReturn(new DeviceEnrollmentResponse(deviceId, org, credential, expiresAt));
  }
}
