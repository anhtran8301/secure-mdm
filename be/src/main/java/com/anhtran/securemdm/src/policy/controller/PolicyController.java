package com.anhtran.securemdm.src.policy.controller;

import com.anhtran.securemdm.common.dto.PageResponse;
import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.policy.dto.model.PolicyConfig;
import com.anhtran.securemdm.src.policy.dto.request.AssignPolicyRequest;
import com.anhtran.securemdm.src.policy.dto.request.CreatePolicyRequest;
import com.anhtran.securemdm.src.policy.dto.response.PolicyAssignmentResponse;
import com.anhtran.securemdm.src.policy.dto.response.PolicyResponse;
import com.anhtran.securemdm.src.policy.dto.response.PolicyVersionResponse;
import com.anhtran.securemdm.src.policy.service.PolicyService;
import com.anhtran.securemdm.utils.AppConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstants.API_PREFIX)
public class PolicyController {
  private final PolicyService policyService;

  @PostMapping("/policies")
  public Mono<PolicyVersionResponse> create(
      @AuthenticationPrincipal Actor actor, @Valid @RequestBody CreatePolicyRequest request) {
    return policyService.create(actor, request);
  }

  @PostMapping("/policies/{id}/versions")
  public Mono<PolicyVersionResponse> createVersion(
      @AuthenticationPrincipal Actor actor,
      @PathVariable String id,
      @Valid @RequestBody PolicyConfig config) {
    return policyService.createVersion(actor, id, config);
  }

  @GetMapping("/policies")
  public Mono<PageResponse<PolicyResponse>> list(
      @AuthenticationPrincipal Actor actor,
      @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
      @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
    return policyService.list(actor, page, size);
  }

  @GetMapping("/policies/{id}/versions")
  public Mono<PageResponse<PolicyVersionResponse>> versions(
      @AuthenticationPrincipal Actor actor,
      @PathVariable String id,
      @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
      @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
    return policyService.versions(actor, id, page, size);
  }

  @PutMapping("/devices/{id}/policy-assignment")
  public Mono<PolicyAssignmentResponse> assign(
      @AuthenticationPrincipal Actor actor,
      @PathVariable String id,
      @Valid @RequestBody AssignPolicyRequest request) {
    return policyService.assign(actor, id, request.versionId());
  }

  @DeleteMapping("/devices/{id}/policy-assignment")
  public Mono<PolicyAssignmentResponse> unassign(
      @AuthenticationPrincipal Actor actor, @PathVariable String id) {
    return policyService.unassign(actor, id);
  }

  @GetMapping("/devices/{id}/policy-assignment")
  public Mono<PolicyAssignmentResponse> assignment(
      @AuthenticationPrincipal Actor actor, @PathVariable String id) {
    return policyService.current(actor.organizationId(), id);
  }
}
