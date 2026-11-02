package com.anhtran.securemdm.src.policy.controller;

import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.policy.dto.request.PolicyReportRequest;
import com.anhtran.securemdm.src.policy.dto.response.PolicyAssignmentResponse;
import com.anhtran.securemdm.src.policy.dto.response.PolicyReportResponse;
import com.anhtran.securemdm.src.policy.service.PolicyService;
import com.anhtran.securemdm.utils.AppConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Endpoints called by an enrolled device; the principal is the device itself (role DEVICE). */
@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstants.API_PREFIX + "/device-agent")
public class PolicyAgentController {
  private final PolicyService policyService;

  @GetMapping("/policy")
  public Mono<PolicyAssignmentResponse> policy(@AuthenticationPrincipal Actor device) {
    return policyService.current(device.organizationId(), device.id());
  }

  @PostMapping("/policy-report")
  public Mono<PolicyReportResponse> report(
      @AuthenticationPrincipal Actor device, @Valid @RequestBody PolicyReportRequest request) {
    return policyService.report(device, request);
  }
}
