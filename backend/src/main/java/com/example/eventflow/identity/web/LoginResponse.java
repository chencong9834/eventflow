package com.example.eventflow.identity.web;

import java.util.List;

public class LoginResponse {

  private String token;
  private CurrentUserResponse user;

  public LoginResponse(String token, CurrentUserResponse user) {
    this.token = token;
    this.user = user;
  }

  public String getToken() {
    return token;
  }

  public CurrentUserResponse getUser() {
    return user;
  }

  public static class CurrentUserResponse {
    private Long userId;
    private String username;
    private String displayName;
    private Long tenantId;
    private String tenantCode;
    private String tenantName;
    private String tenantType;
    private String roleCode;
    private String roleName;
    private List<String> permissions;

    public Long getUserId() {
      return userId;
    }

    public void setUserId(Long userId) {
      this.userId = userId;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getDisplayName() {
      return displayName;
    }

    public void setDisplayName(String displayName) {
      this.displayName = displayName;
    }

    public Long getTenantId() {
      return tenantId;
    }

    public void setTenantId(Long tenantId) {
      this.tenantId = tenantId;
    }

    public String getTenantCode() {
      return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
      this.tenantCode = tenantCode;
    }

    public String getTenantName() {
      return tenantName;
    }

    public void setTenantName(String tenantName) {
      this.tenantName = tenantName;
    }

    public String getTenantType() {
      return tenantType;
    }

    public void setTenantType(String tenantType) {
      this.tenantType = tenantType;
    }

    public String getRoleCode() {
      return roleCode;
    }

    public void setRoleCode(String roleCode) {
      this.roleCode = roleCode;
    }

    public String getRoleName() {
      return roleName;
    }

    public void setRoleName(String roleName) {
      this.roleName = roleName;
    }

    public List<String> getPermissions() {
      return permissions;
    }

    public void setPermissions(List<String> permissions) {
      this.permissions = permissions;
    }
  }
}
