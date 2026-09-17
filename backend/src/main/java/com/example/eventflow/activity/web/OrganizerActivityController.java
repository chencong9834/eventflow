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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizer")
public class OrganizerActivityController {

  private final ActivityService activityService;

  public OrganizerActivityController(ActivityService activityService) {
    this.activityService = activityService;
  }

  @PreAuthorize(
      "hasAnyAuthority('activity:write','order:read','ticket:verify','report:read','refund:write')")
  @GetMapping("/activities")
  public ApiResponse<List<ActivitySummaryResponse>> list(
      @AuthenticationPrincipal AuthPrincipal principal) {
    return ApiResponse.ok(activityService.listMine(principal));
  }

  @PreAuthorize(
      "hasAnyAuthority('activity:write','order:read','ticket:verify','report:read','refund:write')")
  @GetMapping("/activities/{id}")
  public ApiResponse<ActivityDetailResponse> detail(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
    return ApiResponse.ok(activityService.getMine(principal, id));
  }

  @PreAuthorize("hasAuthority('activity:write')")
  @PostMapping("/activities")
  public ApiResponse<ActivityDetailResponse> create(
      @AuthenticationPrincipal AuthPrincipal principal,
      @Valid @RequestBody UpsertActivityRequest request) {
    return ApiResponse.ok(activityService.create(principal, request));
  }

  @PreAuthorize("hasAuthority('activity:write')")
  @PutMapping("/activities/{id}")
  public ApiResponse<ActivityDetailResponse> update(
      @AuthenticationPrincipal AuthPrincipal principal,
      @PathVariable Long id,
      @Valid @RequestBody UpsertActivityRequest request) {
    return ApiResponse.ok(activityService.update(principal, id, request));
  }

  @PreAuthorize("hasAuthority('activity:write')")
  @PostMapping("/activities/{id}/shows")
  public ApiResponse<ActivityDetailResponse> addShow(
      @AuthenticationPrincipal AuthPrincipal principal,
      @PathVariable Long id,
      @Valid @RequestBody UpsertShowRequest request) {
    return ApiResponse.ok(activityService.addShow(principal, id, request));
  }

  @PreAuthorize("hasAuthority('activity:write')")
  @PutMapping("/shows/{id}")
  public ApiResponse<ActivityDetailResponse> updateShow(
      @AuthenticationPrincipal AuthPrincipal principal,
      @PathVariable Long id,
      @Valid @RequestBody UpsertShowRequest request) {
    return ApiResponse.ok(activityService.updateShow(principal, id, request));
  }

  @PreAuthorize("hasAuthority('activity:write')")
  @PostMapping("/shows/{id}/tiers")
  public ApiResponse<ActivityDetailResponse> addTier(
      @AuthenticationPrincipal AuthPrincipal principal,
      @PathVariable Long id,
      @Valid @RequestBody UpsertTierRequest request) {
    return ApiResponse.ok(activityService.addTier(principal, id, request));
  }

  @PreAuthorize("hasAuthority('activity:write')")
  @PutMapping("/tiers/{id}")
  public ApiResponse<ActivityDetailResponse> updateTier(
      @AuthenticationPrincipal AuthPrincipal principal,
      @PathVariable Long id,
      @Valid @RequestBody UpsertTierRequest request) {
    return ApiResponse.ok(activityService.updateTier(principal, id, request));
  }

  @PreAuthorize("hasAuthority('activity:write')")
  @PostMapping("/activities/{id}/submit")
  public ApiResponse<ActivityDetailResponse> submit(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
    return ApiResponse.ok(activityService.submit(principal, id));
  }

  @PreAuthorize("hasAuthority('activity:write')")
  @PostMapping("/activities/{id}/revision")
  public ApiResponse<ActivityDetailResponse> startRevision(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
    return ApiResponse.ok(activityService.startRevision(principal, id));
  }

  @PreAuthorize("hasAuthority('activity:write')")
  @PostMapping("/activities/{id}/on-sale")
  public ApiResponse<ActivityDetailResponse> onSale(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
    return ApiResponse.ok(activityService.onSale(principal, id));
  }

  @PreAuthorize("hasAuthority('activity:write')")
  @PostMapping("/activities/{id}/off-sale")
  public ApiResponse<ActivityDetailResponse> offSale(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
    return ApiResponse.ok(activityService.offSale(principal, id));
  }
}
