package com.example.eventflow.identity;

import com.example.eventflow.iam.RolePermissionMapper;
import com.example.eventflow.iam.SysRole;
import com.example.eventflow.iam.SysRoleMapper;
import com.example.eventflow.identity.web.CreateStaffRequest;
import com.example.eventflow.identity.web.StaffRoleResponse;
import com.example.eventflow.identity.web.StaffUserResponse;
import com.example.eventflow.identity.web.UpdateStaffRequest;
import com.example.eventflow.shared.api.PageQuery;
import com.example.eventflow.shared.api.PageResult;
import com.example.eventflow.shared.api.PageSupport;
import com.example.eventflow.shared.error.BizException;
import com.example.eventflow.shared.error.ErrorCode;
import com.example.eventflow.shared.id.SnowflakeIdGenerator;
import com.example.eventflow.shared.security.AuthPrincipal;
import com.example.eventflow.shared.time.Utc;
import com.example.eventflow.tenant.TenantService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffService {

  private final SysUserMapper userMapper;
  private final SysRoleMapper roleMapper;
  private final RolePermissionMapper rolePermissionMapper;
  private final SnowflakeIdGenerator ids;
  private final PasswordEncoder passwordEncoder;

  public StaffService(
      SysUserMapper userMapper,
      SysRoleMapper roleMapper,
      RolePermissionMapper rolePermissionMapper,
      SnowflakeIdGenerator ids,
      PasswordEncoder passwordEncoder) {
    this.userMapper = userMapper;
    this.roleMapper = roleMapper;
    this.rolePermissionMapper = rolePermissionMapper;
    this.ids = ids;
    this.passwordEncoder = passwordEncoder;
  }

  public PageResult<StaffUserResponse> list(AuthPrincipal principal, PageQuery page) {
    requireOrganizer(principal);
    return PageSupport.query(
        page, () -> userMapper.findSummariesByTenantId(principal.getTenantId()), StaffService::toResponse);
  }

  public List<StaffRoleResponse> roles(AuthPrincipal principal) {
    requireOrganizer(principal);
    List<StaffRoleResponse> rows = new ArrayList<>();
    for (SysRole role : roleMapper.findByTenantId(principal.getTenantId())) {
      StaffRoleResponse item = new StaffRoleResponse();
      item.setId(role.getId());
      item.setCode(role.getCode());
      item.setName(role.getName());
      item.setPermissions(rolePermissionMapper.findPermissionCodes(role.getId()));
      rows.add(item);
    }
    return rows;
  }

  @Transactional
  public StaffUserResponse create(AuthPrincipal principal, CreateStaffRequest request) {
    requireOrganizer(principal);
    String username = request.getUsername().trim().toLowerCase();
    if (userMapper.countByUsername(username) > 0) {
      throw new BizException(ErrorCode.USERNAME_TAKEN, "用户名已存在", HttpStatus.CONFLICT);
    }
    SysRole role = requireTenantRole(principal.getTenantId(), request.getRoleCode());
    SysUser user = new SysUser();
    user.setId(ids.nextId());
    user.setTenantId(principal.getTenantId());
    user.setUsername(username);
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setDisplayName(request.getDisplayName().trim());
    user.setMobile(blankToNull(request.getMobile()));
    user.setRoleId(role.getId());
    user.setStatus(TenantService.STATUS_ACTIVE);
    user.setCreatedBy(principal.getUserId());
    userMapper.insert(user);
    return requireUser(principal.getTenantId(), user.getId());
  }

  @Transactional
  public StaffUserResponse update(AuthPrincipal principal, Long userId, UpdateStaffRequest request) {
    requireOrganizer(principal);
    SysUser target = requireUserEntity(principal.getTenantId(), userId);
    boolean bumpToken = false;
    if (request.getStatus() != null) {
      if (userId.equals(principal.getUserId()) && TenantService.STATUS_DISABLED.equals(request.getStatus())) {
        throw new BizException(ErrorCode.BAD_REQUEST, "不能停用当前登录账号", HttpStatus.BAD_REQUEST);
      }
      if (TenantService.STATUS_DISABLED.equals(request.getStatus())) {
        ensureNotLastAdmin(principal.getTenantId(), target, null);
      }
      userMapper.updateStatus(userId, principal.getTenantId(), request.getStatus());
      bumpToken = true;
    }
    if (request.getRoleCode() != null && !request.getRoleCode().isBlank()) {
      SysRole role = requireTenantRole(principal.getTenantId(), request.getRoleCode());
      if (!role.getId().equals(target.getRoleId())) {
        ensureNotLastAdmin(principal.getTenantId(), target, role.getCode());
        userMapper.updateRoleId(userId, principal.getTenantId(), role.getId());
        bumpToken = true;
      }
    }
    if (request.getPassword() != null && !request.getPassword().isBlank()) {
      userMapper.updatePasswordHash(userId, passwordEncoder.encode(request.getPassword()));
      bumpToken = true;
    }
    if (bumpToken) {
      userMapper.incrementTokenVersion(userId);
    }
    return requireUser(principal.getTenantId(), userId);
  }

  private void ensureNotLastAdmin(Long tenantId, SysUser target, String newRoleCode) {
    if (!TenantService.ORGANIZER_ADMIN.equals(target.getRoleCode())) {
      return;
    }
    boolean leavingAdmin =
        newRoleCode != null && !TenantService.ORGANIZER_ADMIN.equals(newRoleCode);
    boolean disabling = newRoleCode == null;
    if (!leavingAdmin && !disabling) {
      return;
    }
    int admins = userMapper.countActiveByTenantAndRoleCode(tenantId, TenantService.ORGANIZER_ADMIN);
    if (admins <= 1 && TenantService.STATUS_ACTIVE.equals(target.getStatus())) {
      throw new BizException(ErrorCode.LAST_ADMIN_REQUIRED, "至少保留一名启用中的主办方管理员", HttpStatus.CONFLICT);
    }
  }

  private SysRole requireTenantRole(Long tenantId, String roleCode) {
    SysRole role = roleMapper.findByTenantAndCode(tenantId, roleCode.trim());
    if (role == null) {
      throw new BizException(ErrorCode.BAD_REQUEST, "角色不存在或不属于本租户", HttpStatus.BAD_REQUEST);
    }
    return role;
  }

  private StaffUserResponse requireUser(Long tenantId, Long userId) {
    return toResponse(requireUserEntity(tenantId, userId));
  }

  private SysUser requireUserEntity(Long tenantId, Long userId) {
    SysUser user = userMapper.findById(userId);
    if (user == null || !tenantId.equals(user.getTenantId())) {
      throw new BizException(ErrorCode.NOT_FOUND, "账号不存在", HttpStatus.NOT_FOUND);
    }
    return user;
  }

  private static void requireOrganizer(AuthPrincipal principal) {
    if (!TenantService.TYPE_ORGANIZER.equals(principal.getTenantType())) {
      throw new BizException(ErrorCode.FORBIDDEN, "仅主办方可管理本租户账号", HttpStatus.FORBIDDEN);
    }
  }

  private static StaffUserResponse toResponse(SysUser user) {
    StaffUserResponse dto = new StaffUserResponse();
    dto.setId(user.getId());
    dto.setUsername(user.getUsername());
    dto.setDisplayName(user.getDisplayName());
    dto.setMobile(user.getMobile());
    dto.setRoleCode(user.getRoleCode());
    dto.setRoleName(user.getRoleName());
    dto.setStatus(user.getStatus());
    dto.setCreatedAt(Utc.toInstant(user.getCreatedAt()));
    return dto;
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
