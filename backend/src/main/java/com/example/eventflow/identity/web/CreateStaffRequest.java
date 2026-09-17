package com.example.eventflow.identity.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateStaffRequest {

  @NotBlank
  @Size(min = 3, max = 64)
  @Pattern(regexp = "^[a-z][a-z0-9_]{2,63}$", message = "用户名须为小写字母开头，仅含字母数字下划线")
  private String username;

  @NotBlank
  @Size(min = 8, max = 72)
  private String password;

  @NotBlank
  @Size(max = 128)
  private String displayName;

  @Size(max = 32)
  private String mobile;

  @NotBlank
  @Size(max = 32)
  private String roleCode;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getMobile() {
    return mobile;
  }

  public void setMobile(String mobile) {
    this.mobile = mobile;
  }

  public String getRoleCode() {
    return roleCode;
  }

  public void setRoleCode(String roleCode) {
    this.roleCode = roleCode;
  }
}
