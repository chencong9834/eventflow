package com.example.eventflow.activity.web;

import com.example.eventflow.activity.ActivityService;
import com.example.eventflow.activity.web.ActivitySummaryResponse.ActivityDetailResponse;
import com.example.eventflow.shared.api.ApiResponse;
import com.example.eventflow.shared.security.AuthPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/reviews")
public class PlatformReviewController {

  private final ActivityService activityService;

  public PlatformReviewController(ActivityService activityService) {
    this.activityService = activityService;
  }

  @PreAuthorize("hasAnyAuthority('review:write','tenant:read')")
  @GetMapping
  public ApiResponse<List<ActivitySummaryResponse>> list(
      @RequestParam(required = false, defaultValue = "PENDING") String status) {
    return ApiResponse.ok(activityService.listForReview(status));
  }

  @PreAuthorize("hasAnyAuthority('review:write','tenant:read')")
  @GetMapping("/{id}")
  public ApiResponse<ActivityDetailResponse> detail(@PathVariable Long id) {
    return ApiResponse.ok(activityService.getForReview(id));
  }

  @PreAuthorize("hasAuthority('review:write')")
  @PostMapping("/{id}/decide")
  public ApiResponse<ActivityDetailResponse> decide(
      @AuthenticationPrincipal AuthPrincipal principal,
      @PathVariable Long id,
      @Valid @RequestBody ReviewDecideRequest request) {
    return ApiResponse.ok(activityService.decide(principal, id, request));
  }
}
