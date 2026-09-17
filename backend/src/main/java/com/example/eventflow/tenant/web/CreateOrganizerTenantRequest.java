package com.example.eventflow.tenant.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateOrganizerTenantRequest {

  @NotBlank
  @Size(max = 64)
  @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,62}[a-z0-9]$", message = "租户编码须为小写字母、数字和中划线")
  private String tenantCode;

  @NotBlank
  @Size(max = 128)
  private String name;

  @NotBlank
  @Size(min = 3, max = 64)
  @Pattern(regexp = "^[a-z][a-z0-9_]{2,63}$", message = "管理员用户名须为小写字母开头，仅含字母数字下划线")
  private String adminUsername;

  @NotBlank
  @Size(min = 8, max = 72)
  private String adminPassword;

  @NotBlank
  @Size(max = 128)
  private String adminDisplayName;

  @Size(max = 32)
  private String adminMobile;

  public String getTenantCode() {
    return tenantCode;
  }

  public void setTenantCode(String tenantCode) {
    this.tenantCode = tenantCode;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAdminUsername() {
    return adminUsername;
  }

  public void setAdminUsername(String adminUsername) {
    this.adminUsername = adminUsername;
  }

  public String getAdminPassword() {
    return adminPassword;
  }

  public void setAdminPassword(String adminPassword) {
    this.adminPassword = adminPassword;
  }

  public String getAdminDisplayName() {
    return adminDisplayName;
  }

  public void setAdminDisplayName(String adminDisplayName) {
    this.adminDisplayName = adminDisplayName;
  }

  public String getAdminMobile() {
    return adminMobile;
  }

  public void setAdminMobile(String adminMobile) {
    this.adminMobile = adminMobile;
  }
}
