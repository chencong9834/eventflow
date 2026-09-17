package com.example.eventflow.tenant.web;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TenantDetailResponse extends TenantSummaryResponse {

  private List<RoleItem> roles = new ArrayList<>();
  private List<UserItem> users = new ArrayList<>();

  public List<RoleItem> getRoles() {
    return roles;
  }

  public void setRoles(List<RoleItem> roles) {
    this.roles = roles;
  }

  public List<UserItem> getUsers() {
    return users;
  }

  public void setUsers(List<UserItem> users) {
    this.users = users;
  }

  public static class RoleItem {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String code;
    private String name;
    private List<String> permissions = new ArrayList<>();

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public List<String> getPermissions() {
      return permissions;
    }

    public void setPermissions(List<String> permissions) {
      this.permissions = permissions;
    }
  }

  public static class UserItem {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String username;
    private String displayName;
    private String mobile;
    private String roleCode;
    private String roleName;
    private String status;
    private LocalDateTime createdAt;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
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

    public String getRoleName() {
      return roleName;
    }

    public void setRoleName(String roleName) {
      this.roleName = roleName;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }
  }
}
