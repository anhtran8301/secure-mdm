package com.anhtran.securemdm.src.policy.service.impl;

import com.anhtran.securemdm.common.dto.PageResponse;
import com.anhtran.securemdm.exception.CommonException;
import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.audit.service.AuditService;
import com.anhtran.securemdm.src.device.entity.enums.DeviceStatus;
import com.anhtran.securemdm.src.device.repository.DeviceRepository;
import com.anhtran.securemdm.src.policy.dto.model.PolicyConfig;
import com.anhtran.securemdm.src.policy.dto.request.CreatePolicyRequest;
import com.anhtran.securemdm.src.policy.dto.request.PolicyReportRequest;
import com.anhtran.securemdm.src.policy.dto.response.PolicyAssignmentResponse;
import com.anhtran.securemdm.src.policy.dto.response.PolicyReportResponse;
import com.anhtran.securemdm.src.policy.dto.response.PolicyResponse;
import com.anhtran.securemdm.src.policy.dto.response.PolicyVersionResponse;
import com.anhtran.securemdm.src.policy.entity.DevicePolicyAssignment;
import com.anhtran.securemdm.src.policy.entity.DevicePolicyReport;
import com.anhtran.securemdm.src.policy.entity.Policy;
import com.anhtran.securemdm.src.policy.entity.PolicyVersion;
import com.anhtran.securemdm.src.policy.entity.enums.ComplianceStatus;
import com.anhtran.securemdm.src.policy.mapper.PolicyMapper;
import com.anhtran.securemdm.src.policy.repository.DevicePolicyAssignmentRepository;
import com.anhtran.securemdm.src.policy.repository.DevicePolicyReportRepository;
import com.anhtran.securemdm.src.policy.repository.PolicyRepository;
import com.anhtran.securemdm.src.policy.repository.PolicyVersionRepository;
import com.anhtran.securemdm.src.policy.repository.projection.AssignedVersionView;
import com.anhtran.securemdm.src.policy.service.PolicyService;
import com.anhtran.securemdm.src.policy.service.rule.ComplianceEvaluator;
import com.anhtran.securemdm.utils.IdUtils;
import com.anhtran.securemdm.utils.JsonUtils;
import com.anhtran.securemdm.utils.PageUtils;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {
  private final PolicyRepository policyRepository;
  private final PolicyVersionRepository versionRepository;
  private final DeviceRepository deviceRepository;
  private final DevicePolicyAssignmentRepository assignmentRepository;
  private final DevicePolicyReportRepository reportRepository;
  private final AuditService auditService;
  private final PolicyMapper policyMapper;
  private final TransactionalOperator transactions;
  private final Clock clock;

  @Override
  public Mono<PolicyVersionResponse> create(Actor actor, CreatePolicyRequest request) {
    return Mono.defer(
        () -> {
          actor.requireOperator();
          String id = IdUtils.newId();
          return policyRepository
              .save(
                  Policy.builder()
                      .id(id)
                      .organizationId(actor.organizationId())
                      .name(request.name())
                      .createdAt(clock.millis())
                      .newEntity(true)
                      .build())
              .then(insertVersion(actor, id, 1, request.config()))
              .as(transactions::transactional);
        });
  }

  @Override
  public Mono<PolicyVersionResponse> createVersion(
      Actor actor, String policyId, PolicyConfig config) {
    return Mono.defer(
        () -> {
          actor.requireOperator();
          String org = actor.organizationId();
          // The policy row lock serializes version numbering.
          return policyRepository
              .lock(org, policyId)
              .switchIfEmpty(Mono.error(CommonException.notFound()))
              .then(versionRepository.maxVersion(org, policyId))
              .flatMap(max -> insertVersion(actor, policyId, max + 1, config))
              .as(transactions::transactional);
        });
  }

  private Mono<PolicyVersionResponse> insertVersion(
      Actor actor, String policyId, int version, PolicyConfig config) {
    return Mono.defer(
        () ->
            versionRepository
                .save(
                    PolicyVersion.builder()
                        .id(IdUtils.newId())
                        .organizationId(actor.organizationId())
                        .policyId(policyId)
                        .version(version)
                        .configJson(JsonUtils.write(config))
                        .createdAt(clock.millis())
                        .newEntity(true)
                        .build())
                .flatMap(
                    saved ->
                        auditService
                            .record(actor, "POLICY_VERSION_CREATED", "policy", policyId)
                            .thenReturn(policyMapper.toVersionResponse(saved))));
  }

  @Override
  public Mono<PageResponse<PolicyResponse>> list(Actor actor, int page, int size) {
    return Mono.defer(
        () -> {
          long offset = PageUtils.offset(page, size);
          String org = actor.organizationId();
          return Mono.zip(
                  policyRepository
                      .findLatestVersions(org, size, offset)
                      .map(policyMapper::toResponse)
                      .collectList(),
                  policyRepository.countByOrganizationId(org))
              .map(p -> PageResponse.of(p.getT1(), p.getT2(), page, size));
        });
  }

  @Override
  public Mono<PageResponse<PolicyVersionResponse>> versions(
      Actor actor, String policyId, int page, int size) {
    return Mono.defer(
        () -> {
          long offset = PageUtils.offset(page, size);
          String org = actor.organizationId();
          return policyRepository
              .existsByOrganizationIdAndId(org, policyId)
              .filter(Boolean::booleanValue)
              .switchIfEmpty(Mono.error(CommonException.notFound()))
              .then(
                  Mono.zip(
                      versionRepository
                          .findPage(org, policyId, size, offset)
                          .map(policyMapper::toVersionResponse)
                          .collectList(),
                      versionRepository.countByOrganizationIdAndPolicyId(org, policyId)))
              .map(p -> PageResponse.of(p.getT1(), p.getT2(), page, size));
        });
  }

  @Override
  public Mono<PolicyAssignmentResponse> assign(Actor actor, String deviceId, String versionId) {
    return Mono.defer(
        () -> {
          actor.requireOperator();
          String org = actor.organizationId();
          return deviceRepository
              .lockExisting(org, deviceId)
              .flatMap(
                  device -> {
                    if (!DeviceStatus.ACTIVE.name().equals(device.getStatus())) {
                      return Mono.error(CommonException.conflict("Device revoked"));
                    }
                    long revision = device.getAssignmentRevision() + 1;
                    return versionRepository
                        .findByOrganizationIdAndId(org, versionId)
                        .switchIfEmpty(Mono.error(CommonException.notFound()))
                        .then(deviceRepository.incrementAssignmentRevision(org, deviceId))
                        .then(
                            assignmentRepository.upsert(
                                DevicePolicyAssignment.builder()
                                    .deviceId(deviceId)
                                    .organizationId(org)
                                    .versionId(versionId)
                                    .revision(revision)
                                    .assignedAt(clock.millis())
                                    .build()))
                        .then(auditService.record(actor, "POLICY_ASSIGNED", "device", deviceId))
                        .then(current(org, deviceId));
                  })
              .as(transactions::transactional);
        });
  }

  @Override
  public Mono<PolicyAssignmentResponse> unassign(Actor actor, String deviceId) {
    return Mono.defer(
        () -> {
          actor.requireOperator();
          String org = actor.organizationId();
          return deviceRepository
              .lockExisting(org, deviceId)
              .then(deviceRepository.incrementAssignmentRevision(org, deviceId))
              .then(assignmentRepository.deleteForDevice(org, deviceId))
              .then(auditService.record(actor, "POLICY_UNASSIGNED", "device", deviceId))
              .then(current(org, deviceId))
              .as(transactions::transactional);
        });
  }

  @Override
  public Mono<PolicyAssignmentResponse> current(String organizationId, String deviceId) {
    return deviceRepository
        .findExisting(organizationId, deviceId)
        .flatMap(
            device ->
                assignmentRepository
                    .findAssignedVersion(organizationId, deviceId)
                    .map(
                        version ->
                            new PolicyAssignmentResponse(
                                device.getAssignmentRevision(),
                                policyMapper.toVersionResponse(version)))
                    .defaultIfEmpty(
                        new PolicyAssignmentResponse(device.getAssignmentRevision(), null)));
  }

  @Override
  public Mono<PolicyReportResponse> report(Actor device, PolicyReportRequest request) {
    String org = device.organizationId();
    return deviceRepository
        .lockExisting(org, device.id())
        .flatMap(
            d -> {
              if (!DeviceStatus.ACTIVE.name().equals(d.getStatus())) {
                return Mono.error(CommonException.unauthorized());
              }
              return assignmentRepository
                  .findAssignedVersion(org, device.id())
                  .switchIfEmpty(Mono.error(CommonException.conflict("No current assignment")))
                  .flatMap(assigned -> evaluate(device, assigned, request));
            })
        .as(transactions::transactional);
  }

  private Mono<PolicyReportResponse> evaluate(
      Actor device, AssignedVersionView assigned, PolicyReportRequest request) {
    long revision = request.revision();
    if (assigned.getRevision() != revision || !request.versionId().equals(assigned.getId())) {
      return Mono.error(CommonException.conflict("Stale policy report"));
    }
    String org = device.organizationId();
    List<String> reasons =
        ComplianceEvaluator.reasons(
            policyMapper.config(assigned.getConfigJson()), request.inventory());
    String compliance =
        (reasons.isEmpty() ? ComplianceStatus.COMPLIANT : ComplianceStatus.NON_COMPLIANT).name();
    return reportRepository
        .findByOrganizationIdAndDeviceId(org, device.id())
        .map(old -> old.getRevision() != revision || !compliance.equals(old.getCompliance()))
        .defaultIfEmpty(true)
        // Repeated identical reports are not audited again.
        .flatMap(
            changed ->
                changed
                    ? auditService.record(device, "POLICY_REPORTED", "device", device.id())
                    : Mono.<Void>empty())
        .then(
            deviceRepository.updateInventory(
                org, device.id(), JsonUtils.write(request.inventory()), clock.millis()))
        .then(
            reportRepository.upsert(
                DevicePolicyReport.builder()
                    .deviceId(device.id())
                    .organizationId(org)
                    .revision(revision)
                    .versionId(request.versionId())
                    .compliance(compliance)
                    .reasonsJson(JsonUtils.write(reasons))
                    .reportedAt(clock.millis())
                    .build()))
        .thenReturn(new PolicyReportResponse(compliance, reasons, revision));
  }
}
