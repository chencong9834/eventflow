package com.example.eventflow.platform.web;

import com.example.eventflow.shared.api.ApiResponse;
import com.example.eventflow.tenant.TenantService;
import com.example.eventflow.tenant.web.PlatformOverviewResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform")
public class PlatformHomeController {

  private final TenantService tenantService;

  public PlatformHomeController(TenantService tenantService) {
    this.tenantService = tenantService;
  }

  @PreAuthorize("hasAuthority('tenant:read')")
  @GetMapping("/overview")
  public ApiResponse<PlatformOverviewResponse> overview() {
    return ApiResponse.ok(tenantService.overview());
  }
}
