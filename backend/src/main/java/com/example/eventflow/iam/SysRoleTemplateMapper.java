package com.example.eventflow.iam;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysRoleTemplateMapper {

  @Select("SELECT * FROM sys_role_template WHERE tenant_type = #{tenantType}")
  List<SysRoleTemplate> findByTenantType(@Param("tenantType") String tenantType);

  @Select("SELECT permission_id FROM sys_role_template_permission WHERE template_id = #{templateId}")
  List<Long> findPermissionIds(@Param("templateId") Long templateId);
}
