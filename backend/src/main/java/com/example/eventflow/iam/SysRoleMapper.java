package com.example.eventflow.iam;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysRoleMapper {

  @Select("SELECT * FROM sys_role WHERE id = #{id}")
  SysRole findById(@Param("id") Long id);

  @Select("SELECT * FROM sys_role WHERE tenant_id = #{tenantId} AND code = #{code}")
  SysRole findByTenantAndCode(@Param("tenantId") Long tenantId, @Param("code") String code);

  @Select("SELECT * FROM sys_role WHERE tenant_id = #{tenantId} ORDER BY code")
  List<SysRole> findByTenantId(@Param("tenantId") Long tenantId);

  @Insert(
      """
      INSERT INTO sys_role (id, tenant_id, code, name, created_by)
      VALUES (#{id}, #{tenantId}, #{code}, #{name}, #{createdBy})
      """)
  int insert(SysRole role);
}
