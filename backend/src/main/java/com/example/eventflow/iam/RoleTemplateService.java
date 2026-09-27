package com.example.eventflow.iam;

import cn.hutool.core.lang.Snowflake;
import com.example.eventflow.shared.error.BizException;
import com.example.eventflow.shared.error.ErrorCode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleTemplateService {

  private final SysRoleTemplateMapper templateMapper;
  private final SysRoleMapper roleMapper;
  private final RolePermissionMapper rolePermissionMapper;
  private final Snowflake ids;

  public RoleTemplateService(
      SysRoleTemplateMapper templateMapper,
      SysRoleMapper roleMapper,
      RolePermissionMapper rolePermissionMapper,
      Snowflake ids) {
    this.templateMapper = templateMapper;
    this.roleMapper = roleMapper;
    this.rolePermissionMapper = rolePermissionMapper;
    this.ids = ids;
  }

  @Transactional
  public void copyToTenant(Long tenantId, String tenantType, Long createdBy) {
    List<SysRoleTemplate> templates = templateMapper.findByTenantType(tenantType);
    if (templates.isEmpty()) {
      throw new BizException(ErrorCode.BAD_REQUEST, "没有该租户类型的角色模板", HttpStatus.BAD_REQUEST);
    }
    for (SysRoleTemplate template : templates) {
      SysRole role = new SysRole();
      role.setId(ids.nextId());
      role.setTenantId(tenantId);
      role.setCode(template.getCode());
      role.setName(template.getName());
      role.setCreatedBy(createdBy);
      roleMapper.insert(role);
      for (Long permissionId : templateMapper.findPermissionIds(template.getId())) {
        rolePermissionMapper.insert(role.getId(), permissionId);
      }
    }
  }
}
