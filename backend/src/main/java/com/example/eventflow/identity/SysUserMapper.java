package com.example.eventflow.identity;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysUserMapper {

  @Select(
      """
      SELECT u.id, u.tenant_id, u.username, u.password_hash, u.display_name, u.mobile,
             u.role_id, r.code AS role_code, r.name AS role_name, u.status, u.token_version,
             u.created_at, u.updated_at, u.created_by
      FROM sys_user u
      INNER JOIN sys_role r ON r.id = u.role_id
      WHERE u.username = #{username}
      """)
  SysUser findByUsername(@Param("username") String username);

  @Select(
      """
      SELECT u.id, u.tenant_id, u.username, u.password_hash, u.display_name, u.mobile,
             u.role_id, r.code AS role_code, r.name AS role_name, u.status, u.token_version,
             u.created_at, u.updated_at, u.created_by
      FROM sys_user u
      INNER JOIN sys_role r ON r.id = u.role_id
      WHERE u.id = #{id}
      """)
  SysUser findById(@Param("id") Long id);

  @Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username}")
  int countByUsername(@Param("username") String username);

  @Select(
      """
      SELECT u.id, u.tenant_id, u.username, u.display_name, u.mobile,
             u.role_id, r.code AS role_code, r.name AS role_name, u.status, u.created_at
      FROM sys_user u
      INNER JOIN sys_role r ON r.id = u.role_id
      WHERE u.tenant_id = #{tenantId}
      ORDER BY u.id
      """)
  List<SysUser> findSummariesByTenantId(@Param("tenantId") Long tenantId);

  @Select("SELECT COUNT(*) FROM sys_user WHERE tenant_id = #{tenantId}")
  int countByTenantId(@Param("tenantId") Long tenantId);

  @Select("SELECT COUNT(*) FROM sys_user")
  int countAll();

  @Insert(
      """
      INSERT INTO sys_user
        (id, tenant_id, username, password_hash, display_name, mobile, role_id, status, token_version, created_by)
      VALUES
        (#{id}, #{tenantId}, #{username}, #{passwordHash}, #{displayName}, #{mobile}, #{roleId}, #{status}, 1, #{createdBy})
      """)
  int insert(SysUser user);

  @Update("UPDATE sys_user SET token_version = token_version + 1 WHERE tenant_id = #{tenantId}")
  int bumpTokenVersionByTenant(@Param("tenantId") Long tenantId);

  @Select("SELECT * FROM sys_user WHERE password_hash = #{passwordHash}")
  List<SysUser> findByPasswordHash(@Param("passwordHash") String passwordHash);

  @Update("UPDATE sys_user SET password_hash = #{passwordHash} WHERE id = #{id}")
  int updatePasswordHash(@Param("id") Long id, @Param("passwordHash") String passwordHash);

  @Update("UPDATE sys_user SET token_version = token_version + 1 WHERE id = #{id}")
  int incrementTokenVersion(@Param("id") Long id);

  @Update("UPDATE sys_user SET status = #{status} WHERE id = #{id} AND tenant_id = #{tenantId}")
  int updateStatus(
      @Param("id") Long id, @Param("tenantId") Long tenantId, @Param("status") String status);

  @Update("UPDATE sys_user SET role_id = #{roleId} WHERE id = #{id} AND tenant_id = #{tenantId}")
  int updateRoleId(
      @Param("id") Long id, @Param("tenantId") Long tenantId, @Param("roleId") Long roleId);

  @Select(
      """
      SELECT COUNT(*)
      FROM sys_user u
      INNER JOIN sys_role r ON r.id = u.role_id
      WHERE u.tenant_id = #{tenantId} AND u.status = 'ACTIVE' AND r.code = #{roleCode}
      """)
  int countActiveByTenantAndRoleCode(
      @Param("tenantId") Long tenantId, @Param("roleCode") String roleCode);
}
