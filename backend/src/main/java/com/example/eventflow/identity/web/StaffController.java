package com.example.eventflow.identity.web;

import com.example.eventflow.identity.StaffService;
import com.example.eventflow.shared.api.ApiResponse;
import com.example.eventflow.shared.security.AuthPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizer")
public class StaffController {

  private final StaffService staffService;

  public StaffController(StaffService staffService) {
    this.staffService = staffService;
  }

  @PreAuthorize("hasAuthority('user:write')")
  @GetMapping("/staff")
  public ApiResponse<List<StaffUserResponse>> list(@AuthenticationPrincipal AuthPrincipal principal) {
    return ApiResponse.ok(staffService.list(principal));
  }

  @PreAuthorize("hasAuthority('user:write')")
  @GetMapping("/roles")
  public ApiResponse<List<StaffRoleResponse>> roles(@AuthenticationPrincipal AuthPrincipal principal) {
    return ApiResponse.ok(staffService.roles(principal));
  }

  @PreAuthorize("hasAuthority('user:write')")
  @PostMapping("/staff")
  public ApiResponse<StaffUserResponse> create(
      @AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody CreateStaffRequest request) {
    return ApiResponse.ok(staffService.create(principal, request));
  }

  @PreAuthorize("hasAuthority('user:write')")
  @PatchMapping("/staff/{id}")
  public ApiResponse<StaffUserResponse> update(
      @AuthenticationPrincipal AuthPrincipal principal,
      @PathVariable Long id,
      @Valid @RequestBody UpdateStaffRequest request) {
    return ApiResponse.ok(staffService.update(principal, id, request));
  }
}
