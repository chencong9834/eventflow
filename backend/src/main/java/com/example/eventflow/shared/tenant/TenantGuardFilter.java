package com.example.eventflow.shared.tenant;

import com.example.eventflow.shared.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.eventflow.shared.api.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Isolation tenant_id comes only from the login token. Client headers cannot switch tenant.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class TenantGuardFilter extends OncePerRequestFilter {

  public static final String HEADER_TENANT_ID = "X-Tenant-Id";

  private final ObjectMapper objectMapper;

  public TenantGuardFilter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String claimed = request.getHeader(HEADER_TENANT_ID);
    Long bound = TenantContext.tenantId();
    if (claimed != null && !claimed.isBlank() && bound != null) {
      try {
        long headerTenant = Long.parseLong(claimed.trim());
        if (headerTenant != bound) {
          response.setStatus(HttpServletResponse.SC_FORBIDDEN);
          response.setContentType(MediaType.APPLICATION_JSON_VALUE);
          objectMapper.writeValue(
              response.getWriter(),
              ApiResponse.error(ErrorCode.TENANT_SWITCH_FORBIDDEN, "禁止切换或覆盖租户"));
          return;
        }
      } catch (NumberFormatException ex) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
            response.getWriter(), ApiResponse.error(ErrorCode.BAD_REQUEST, "非法租户头"));
        return;
      }
    }
    filterChain.doFilter(request, response);
  }
}
