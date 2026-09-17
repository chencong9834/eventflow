package com.example.eventflow.shared.security;

import com.example.eventflow.iam.RolePermissionMapper;
import com.example.eventflow.identity.SysUser;
import com.example.eventflow.identity.SysUserMapper;
import com.example.eventflow.shared.tenant.TenantContext;
import com.example.eventflow.tenant.SysTenant;
import com.example.eventflow.tenant.SysTenantMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final SysUserMapper userMapper;
  private final SysTenantMapper tenantMapper;
  private final RolePermissionMapper rolePermissionMapper;

  public JwtAuthFilter(
      JwtService jwtService,
      SysUserMapper userMapper,
      SysTenantMapper tenantMapper,
      RolePermissionMapper rolePermissionMapper) {
    this.jwtService = jwtService;
    this.userMapper = userMapper;
    this.tenantMapper = tenantMapper;
    this.rolePermissionMapper = rolePermissionMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String header = request.getHeader(HttpHeaders.AUTHORIZATION);
      if (header != null && header.startsWith("Bearer ")) {
        String token = header.substring(7);
        try {
          Claims claims = jwtService.parse(token);
          Long userId = Long.valueOf(claims.getSubject());
          SysUser user = userMapper.findById(userId);
          Integer claimVersion = toInt(claims.get("ver"));
          if (user != null
              && "ACTIVE".equals(user.getStatus())
              && claimVersion != null
              && claimVersion == user.tokenVersionOrDefault()) {
            SysTenant tenant = tenantMapper.findById(user.getTenantId());
            Long tokenTenantId = toLong(claims.get("tenantId"));
            if (tokenTenantId != null && !tokenTenantId.equals(user.getTenantId())) {
              SecurityContextHolder.clearContext();
            } else if (tenant != null && "ACTIVE".equals(tenant.getStatus())) {
              List<String> perms = rolePermissionMapper.findPermissionCodes(user.getRoleId());
              AuthPrincipal principal =
                  new AuthPrincipal(
                      user.getId(),
                      user.getTenantId(),
                      tenant.getType(),
                      user.getUsername(),
                      user.getRoleCode(),
                      perms);
              UsernamePasswordAuthenticationToken auth =
                  new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
              SecurityContextHolder.getContext().setAuthentication(auth);
              TenantContext.set(
                  user.getTenantId(), tenant.getType(), user.getId(), user.getRoleCode());
            }
          }
        } catch (JwtException | IllegalArgumentException ignored) {
          SecurityContextHolder.clearContext();
        }
      }
      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
      SecurityContextHolder.clearContext();
    }
  }

  private static Long toLong(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String text && !text.isBlank()) {
      return Long.valueOf(text);
    }
    return null;
  }

  private static Integer toInt(Object value) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value instanceof String text && !text.isBlank()) {
      return Integer.valueOf(text);
    }
    return null;
  }
}
