package com.example.eventflow.iam;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RolePermissionMapper {

  @Select(
      """
      SELECT p.code
      FROM sys_permission p
      INNER JOIN sys_role_permission rp ON rp.permission_id = p.id
      WHERE rp.role_id = #{roleId}
      """)
  List<String> findPermissionCodes(@Param("roleId") Long roleId);

  @Insert(
      """
      INSERT INTO sys_role_permission (role_id, permission_id)
      VALUES (#{roleId}, #{permissionId})
      """)
  int insert(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);
}
