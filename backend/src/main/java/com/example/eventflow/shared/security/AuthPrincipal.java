package com.example.eventflow.shared.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class AuthPrincipal {

  private final Long userId;
  private final Long tenantId;
  private final String tenantType;
  private final String username;
  private final String roleCode;
  private final List<String> permissions;

  public AuthPrincipal(
      Long userId,
      Long tenantId,
      String tenantType,
      String username,
      String roleCode,
      List<String> permissions) {
    this.userId = userId;
    this.tenantId = tenantId;
    this.tenantType = tenantType;
    this.username = username;
    this.roleCode = roleCode;
    this.permissions = permissions == null ? List.of() : List.copyOf(permissions);
  }

  public Long getUserId() {
    return userId;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public String getTenantType() {
    return tenantType;
  }

  public String getUsername() {
    return username;
  }

  public String getRoleCode() {
    return roleCode;
  }

  public List<String> getPermissions() {
    return permissions;
  }

  public Collection<? extends GrantedAuthority> getAuthorities() {
    List<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_" + tenantType));
    for (String permission : permissions) {
      authorities.add(new SimpleGrantedAuthority(permission));
    }
    return authorities;
  }
}
