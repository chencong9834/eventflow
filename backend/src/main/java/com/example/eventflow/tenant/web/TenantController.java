package com.example.eventflow.tenant.web;

import com.example.eventflow.shared.api.ApiResponse;
import com.example.eventflow.shared.api.PageQuery;
import com.example.eventflow.shared.api.PageResult;
import com.example.eventflow.shared.security.AuthPrincipal;
import com.example.eventflow.tenant.SysTenant;
import com.example.eventflow.tenant.TenantService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TenantController {

  private final TenantService tenantService;

  public TenantController(TenantService tenantService) {
    this.tenantService = tenantService;
  }

  @GetMapping("/tenants/me")
  public ApiResponse<SysTenant> me(@AuthenticationPrincipal AuthPrincipal principal) {
    return ApiResponse.ok(tenantService.findById(principal.getTenantId()));
  }

  @PreAuthorize("hasAuthority('tenant:read')")
  @GetMapping("/platform/tenants")
  public ApiResponse<PageResult<TenantSummaryResponse>> listAll(
      @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
    return ApiResponse.ok(tenantService.listAll(PageQuery.of(page, size)));
  }

  @PreAuthorize("hasAuthority('tenant:read')")
  @GetMapping("/platform/tenants/{id}")
  public ApiResponse<TenantDetailResponse> detail(@PathVariable Long id) {
    return ApiResponse.ok(tenantService.getDetail(id));
  }

  @PreAuthorize("hasAuthority('tenant:write')")
  @PostMapping("/platform/tenants")
  public ApiResponse<TenantDetailResponse> createOrganizer(
      @Valid @RequestBody CreateOrganizerTenantRequest request,
      @AuthenticationPrincipal AuthPrincipal principal) {
    return ApiResponse.ok(tenantService.createOrganizer(request, principal.getUserId()));
  }

  @PreAuthorize("hasAuthority('tenant:write')")
  @PatchMapping("/platform/tenants/{id}/status")
  public ApiResponse<TenantDetailResponse> updateStatus(
      @PathVariable Long id, @Valid @RequestBody UpdateTenantStatusRequest request) {
    return ApiResponse.ok(tenantService.updateStatus(id, request.getStatus()));
  }
}
