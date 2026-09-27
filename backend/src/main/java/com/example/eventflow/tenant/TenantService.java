package com.example.eventflow.tenant;

import cn.hutool.core.lang.Snowflake;
import com.example.eventflow.iam.RolePermissionMapper;
import com.example.eventflow.iam.RoleTemplateService;
import com.example.eventflow.iam.SysRole;
import com.example.eventflow.iam.SysRoleMapper;
import com.example.eventflow.activity.ActivityService;
import com.example.eventflow.identity.SysUser;
import com.example.eventflow.identity.SysUserMapper;
import com.example.eventflow.shared.api.PageQuery;
import com.example.eventflow.shared.api.PageResult;
import com.example.eventflow.shared.api.PageSupport;
import com.example.eventflow.shared.error.BizException;
import com.example.eventflow.shared.error.ErrorCode;
import com.example.eventflow.tenant.web.CreateOrganizerTenantRequest;
import com.example.eventflow.tenant.web.PlatformOverviewResponse;
import com.example.eventflow.tenant.web.TenantDetailResponse;
import com.example.eventflow.tenant.web.TenantDetailResponse.RoleItem;
import com.example.eventflow.tenant.web.TenantDetailResponse.UserItem;
import com.example.eventflow.tenant.web.TenantSummaryResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

  public static final String TYPE_ORGANIZER = "ORGANIZER";
  public static final String TYPE_PLATFORM = "PLATFORM";
  public static final String TYPE_BUYER = "BUYER";
  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String STATUS_DISABLED = "DISABLED";
  public static final String ORGANIZER_ADMIN = "ORGANIZER_ADMIN";

  private final SysTenantMapper tenantMapper;
  private final SysRoleMapper roleMapper;
  private final RolePermissionMapper rolePermissionMapper;
  private final SysUserMapper userMapper;
  private final RoleTemplateService roleTemplateService;
  private final Snowflake ids;
  private final PasswordEncoder passwordEncoder;
  private final ActivityService activityService;

  public TenantService(
      SysTenantMapper tenantMapper,
      SysRoleMapper roleMapper,
      RolePermissionMapper rolePermissionMapper,
      SysUserMapper userMapper,
      RoleTemplateService roleTemplateService,
      Snowflake ids,
      PasswordEncoder passwordEncoder,
      ActivityService activityService) {
    this.tenantMapper = tenantMapper;
    this.roleMapper = roleMapper;
    this.rolePermissionMapper = rolePermissionMapper;
    this.userMapper = userMapper;
    this.roleTemplateService = roleTemplateService;
    this.ids = ids;
    this.passwordEncoder = passwordEncoder;
    this.activityService = activityService;
  }

  public PageResult<TenantSummaryResponse> listAll(PageQuery page) {
    return PageSupport.query(page, tenantMapper::findAll, this::toSummary);
  }

  public SysTenant findById(Long id) {
    return tenantMapper.findById(id);
  }

  public TenantDetailResponse getDetail(Long id) {
    SysTenant tenant = requireTenant(id);
    TenantDetailResponse detail = new TenantDetailResponse();
    copySummary(tenant, detail);
    for (SysRole role : roleMapper.findByTenantId(id)) {
      RoleItem item = new RoleItem();
      item.setId(role.getId());
      item.setCode(role.getCode());
      item.setName(role.getName());
      item.setPermissions(rolePermissionMapper.findPermissionCodes(role.getId()));
      detail.getRoles().add(item);
    }
    for (SysUser user : userMapper.findSummariesByTenantId(id)) {
      UserItem item = new UserItem();
      item.setId(user.getId());
      item.setUsername(user.getUsername());
      item.setDisplayName(user.getDisplayName());
      item.setMobile(user.getMobile());
      item.setRoleCode(user.getRoleCode());
      item.setRoleName(user.getRoleName());
      item.setStatus(user.getStatus());
      item.setCreatedAt(user.getCreatedAt());
      detail.getUsers().add(item);
    }
    return detail;
  }

  public PlatformOverviewResponse overview() {
    PlatformOverviewResponse dto = new PlatformOverviewResponse();
    dto.setTenantTotal(tenantMapper.countAll());
    dto.setOrganizerActive(tenantMapper.countByTypeAndStatus(TYPE_ORGANIZER, STATUS_ACTIVE));
    dto.setOrganizerDisabled(tenantMapper.countByTypeAndStatus(TYPE_ORGANIZER, STATUS_DISABLED));
    dto.setUserTotal(userMapper.countAll());
    dto.setPendingReviewHint(activityService.countPending());
    return dto;
  }

  @Transactional
  public TenantDetailResponse createOrganizer(CreateOrganizerTenantRequest request, Long actorId) {
    String code = request.getTenantCode().trim().toLowerCase();
    String username = request.getAdminUsername().trim().toLowerCase();
    if (tenantMapper.countByCode(code) > 0) {
      throw new BizException(ErrorCode.TENANT_CODE_TAKEN, "租户编码已存在", HttpStatus.CONFLICT);
    }
    if (userMapper.countByUsername(username) > 0) {
      throw new BizException(ErrorCode.USERNAME_TAKEN, "管理员用户名已存在", HttpStatus.CONFLICT);
    }
    SysTenant tenant = new SysTenant();
    tenant.setId(ids.nextId());
    tenant.setTenantCode(code);
    tenant.setName(request.getName().trim());
    tenant.setType(TYPE_ORGANIZER);
    tenant.setStatus(STATUS_ACTIVE);
    tenant.setCreatedBy(actorId);
    tenantMapper.insert(tenant);
    roleTemplateService.copyToTenant(tenant.getId(), TYPE_ORGANIZER, actorId);
    SysRole adminRole = roleMapper.findByTenantAndCode(tenant.getId(), ORGANIZER_ADMIN);
    if (adminRole == null) {
      throw new BizException(ErrorCode.BAD_REQUEST, "未找到主办方管理员角色模板", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    SysUser admin = new SysUser();
    admin.setId(ids.nextId());
    admin.setTenantId(tenant.getId());
    admin.setUsername(username);
    admin.setPasswordHash(passwordEncoder.encode(request.getAdminPassword()));
    admin.setDisplayName(request.getAdminDisplayName().trim());
    admin.setMobile(blankToNull(request.getAdminMobile()));
    admin.setRoleId(adminRole.getId());
    admin.setStatus(STATUS_ACTIVE);
    admin.setCreatedBy(actorId);
    userMapper.insert(admin);
    return getDetail(tenant.getId());
  }

  @Transactional
  public TenantDetailResponse updateStatus(Long tenantId, String status) {
    SysTenant tenant = requireTenant(tenantId);
    if (!TYPE_ORGANIZER.equals(tenant.getType())) {
      throw new BizException(ErrorCode.BUILTIN_TENANT_LOCKED, "内置平台/购票租户不可停用", HttpStatus.FORBIDDEN);
    }
    if (!STATUS_ACTIVE.equals(status) && !STATUS_DISABLED.equals(status)) {
      throw new BizException(ErrorCode.BAD_REQUEST, "非法状态", HttpStatus.BAD_REQUEST);
    }
    tenantMapper.updateStatus(tenantId, status);
    if (STATUS_DISABLED.equals(status)) {
      userMapper.bumpTokenVersionByTenant(tenantId);
    }
    return getDetail(tenantId);
  }

  private SysTenant requireTenant(Long id) {
    SysTenant tenant = tenantMapper.findById(id);
    if (tenant == null) {
      throw new BizException(ErrorCode.NOT_FOUND, "租户不存在", HttpStatus.NOT_FOUND);
    }
    return tenant;
  }

  private TenantSummaryResponse toSummary(SysTenant tenant) {
    TenantSummaryResponse dto = new TenantSummaryResponse();
    copySummary(tenant, dto);
    return dto;
  }

  private void copySummary(SysTenant tenant, TenantSummaryResponse dto) {
    dto.setId(tenant.getId());
    dto.setTenantCode(tenant.getTenantCode());
    dto.setName(tenant.getName());
    dto.setType(tenant.getType());
    dto.setStatus(tenant.getStatus());
    dto.setCreatedAt(tenant.getCreatedAt());
    dto.setUserCount(userMapper.countByTenantId(tenant.getId()));
    dto.setRoleCount(roleMapper.findByTenantId(tenant.getId()).size());
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
