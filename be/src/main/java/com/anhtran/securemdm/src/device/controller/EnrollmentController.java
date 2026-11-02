package com.anhtran.securemdm.src.device.controller;

import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.device.dto.request.EnrollDeviceRequest;
import com.anhtran.securemdm.src.device.dto.response.DeviceEnrollmentResponse;
import com.anhtran.securemdm.src.device.dto.response.EnrollmentTokenResponse;
import com.anhtran.securemdm.src.device.service.EnrollmentService;
import com.anhtran.securemdm.utils.AppConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstants.API_PREFIX)
public class EnrollmentController {
  private final EnrollmentService enrollmentService;

  @PostMapping("/enrollment-tokens")
  public Mono<EnrollmentTokenResponse> issue(@AuthenticationPrincipal Actor actor) {
    return enrollmentService.issue(actor);
  }

  @PostMapping("/enrollment-tokens/{id}/revoke")
  public Mono<Void> revoke(@AuthenticationPrincipal Actor actor, @PathVariable String id) {
    return enrollmentService.revoke(actor, id);
  }

  /** Public: authenticated by the one-time enrollment token in the body. */
  @PostMapping("/device-enrollments")
  public Mono<DeviceEnrollmentResponse> enroll(@Valid @RequestBody EnrollDeviceRequest request) {
    return enrollmentService.enroll(request);
  }
}
