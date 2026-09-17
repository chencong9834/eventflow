package com.example.eventflow.identity;

import com.example.eventflow.iam.RolePermissionMapper;
import com.example.eventflow.iam.SysRole;
import com.example.eventflow.iam.SysRoleMapper;
import com.example.eventflow.identity.web.LoginResponse;
import com.example.eventflow.identity.web.LoginResponse.CurrentUserResponse;
import com.example.eventflow.shared.error.BizException;
import com.example.eventflow.shared.error.ErrorCode;
import com.example.eventflow.shared.security.JwtService;
import com.example.eventflow.tenant.SysTenant;
import com.example.eventflow.tenant.SysTenantMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  public static final String DEMO_PASSWORD = "Passw0rd!";

  /** Burns CPU when the user is missing so existence is not leaked by timing. */
  private static final String DUMMY_BCRYPT =
      "$2a$10$7EqJtq98hPqEX7fNZaFWoOahhGz1/QgxuBtXq0aVmehGq0lZ8qK6e";

  private final SysUserMapper userMapper;
  private final SysTenantMapper tenantMapper;
  private final SysRoleMapper roleMapper;
  private final RolePermissionMapper rolePermissionMapper;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final LoginRateLimiter loginRateLimiter;

  public AuthService(
      SysUserMapper userMapper,
      SysTenantMapper tenantMapper,
      SysRoleMapper roleMapper,
      RolePermissionMapper rolePermissionMapper,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      LoginRateLimiter loginRateLimiter) {
    this.userMapper = userMapper;
    this.tenantMapper = tenantMapper;
    this.roleMapper = roleMapper;
    this.rolePermissionMapper = rolePermissionMapper;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.loginRateLimiter = loginRateLimiter;
  }

  public LoginResponse login(String rawUsername, String password, String clientIp) {
    String username = rawUsername == null ? "" : rawUsername.trim();
    loginRateLimiter.assertAllowed(username, clientIp);
    loginRateLimiter.recordAttempt(clientIp);

    SysUser user = userMapper.findByUsername(username);
    if (!passwordMatches(user, password)) {
      loginRateLimiter.recordFailure(username, clientIp);
      throw new BizException(ErrorCode.INVALID_CREDENTIAL, "用户名或密码错误", HttpStatus.UNAUTHORIZED);
    }
    if (!"ACTIVE".equals(user.getStatus())) {
      throw new BizException(ErrorCode.ACCOUNT_DISABLED, "账号已停用", HttpStatus.FORBIDDEN);
    }
    SysTenant tenant = tenantMapper.findById(user.getTenantId());
    if (tenant == null || !"ACTIVE".equals(tenant.getStatus())) {
      throw new BizException(ErrorCode.TENANT_DISABLED, "租户已停用", HttpStatus.FORBIDDEN);
    }
    loginRateLimiter.clearFailures(username, clientIp);
    CurrentUserResponse profile = toProfile(user, tenant);
    String token =
        jwtService.issue(
            user.getId(),
            user.getUsername(),
            tenant.getId(),
            tenant.getType(),
            user.getRoleCode(),
            user.tokenVersionOrDefault());
    return new LoginResponse(token, profile);
  }

  public CurrentUserResponse currentUser(Long userId) {
    SysUser user = userMapper.findById(userId);
    if (user == null) {
      throw new BizException(ErrorCode.UNAUTHORIZED, "未登录", HttpStatus.UNAUTHORIZED);
    }
    SysTenant tenant = tenantMapper.findById(user.getTenantId());
    return toProfile(user, tenant);
  }

  private boolean passwordMatches(SysUser user, String password) {
    if (user == null || !isUsableHash(user.getPasswordHash())) {
      passwordEncoder.matches(password, DUMMY_BCRYPT);
      return false;
    }
    return passwordEncoder.matches(password, user.getPasswordHash());
  }

  private static boolean isUsableHash(String hash) {
    return hash != null && !hash.isBlank() && !"SEED_PLAIN".equals(hash);
  }

  private CurrentUserResponse toProfile(SysUser user, SysTenant tenant) {
    SysRole role = roleMapper.findById(user.getRoleId());
    CurrentUserResponse dto = new CurrentUserResponse();
    dto.setUserId(user.getId());
    dto.setUsername(user.getUsername());
    dto.setDisplayName(user.getDisplayName());
    dto.setTenantId(tenant.getId());
    dto.setTenantCode(tenant.getTenantCode());
    dto.setTenantName(tenant.getName());
    dto.setTenantType(tenant.getType());
    dto.setRoleCode(role == null ? user.getRoleCode() : role.getCode());
    dto.setRoleName(role == null ? user.getRoleCode() : role.getName());
    dto.setPermissions(rolePermissionMapper.findPermissionCodes(user.getRoleId()));
    return dto;
  }
}
