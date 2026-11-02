package com.anhtran.securemdm.src.policy.service;

import com.anhtran.securemdm.common.dto.PageResponse;
import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.policy.dto.model.PolicyConfig;
import com.anhtran.securemdm.src.policy.dto.request.CreatePolicyRequest;
import com.anhtran.securemdm.src.policy.dto.request.PolicyReportRequest;
import com.anhtran.securemdm.src.policy.dto.response.PolicyAssignmentResponse;
import com.anhtran.securemdm.src.policy.dto.response.PolicyReportResponse;
import com.anhtran.securemdm.src.policy.dto.response.PolicyResponse;
import com.anhtran.securemdm.src.policy.dto.response.PolicyVersionResponse;
import reactor.core.publisher.Mono;

public interface PolicyService {
  Mono<PolicyVersionResponse> create(Actor actor, CreatePolicyRequest request);

  /** Versions are immutable; a change always creates the next version. */
  Mono<PolicyVersionResponse> createVersion(Actor actor, String policyId, PolicyConfig config);

  Mono<PageResponse<PolicyResponse>> list(Actor actor, int page, int size);

  Mono<PageResponse<PolicyVersionResponse>> versions(
      Actor actor, String policyId, int page, int size);

  Mono<PolicyAssignmentResponse> assign(Actor actor, String deviceId, String versionId);

  Mono<PolicyAssignmentResponse> unassign(Actor actor, String deviceId);

  Mono<PolicyAssignmentResponse> current(String organizationId, String deviceId);

  /** Rejects reports for a stale assignment revision or version. */
  Mono<PolicyReportResponse> report(Actor device, PolicyReportRequest request);
}
