package com.example.eventflow.identity.web;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateStaffRequest {

  @Size(max = 32)
  private String roleCode;

  @Pattern(regexp = "ACTIVE|DISABLED", message = "非法状态")
  private String status;

  @Size(min = 8, max = 72)
  private String password;

  public String getRoleCode() {
    return roleCode;
  }

  public void setRoleCode(String roleCode) {
    this.roleCode = roleCode;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
