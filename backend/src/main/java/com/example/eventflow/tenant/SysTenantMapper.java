package com.example.eventflow.tenant;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysTenantMapper {

  @Select("SELECT * FROM sys_tenant WHERE id = #{id}")
  SysTenant findById(Long id);

  @Select("SELECT COUNT(*) FROM sys_tenant")
  int countAll();

  @Select("SELECT * FROM sys_tenant ORDER BY id")
  List<SysTenant> findAll();

  @Select("SELECT COUNT(*) FROM sys_tenant WHERE tenant_code = #{tenantCode}")
  int countByCode(@Param("tenantCode") String tenantCode);

  @Select("SELECT COUNT(*) FROM sys_tenant WHERE type = #{type}")
  int countByType(@Param("type") String type);

  @Select("SELECT COUNT(*) FROM sys_tenant WHERE type = #{type} AND status = #{status}")
  int countByTypeAndStatus(@Param("type") String type, @Param("status") String status);

  @Update("UPDATE sys_tenant SET status = #{status} WHERE id = #{id}")
  int updateStatus(@Param("id") Long id, @Param("status") String status);

  @Insert(
      """
      INSERT INTO sys_tenant (id, tenant_code, name, type, status, created_by)
      VALUES (#{id}, #{tenantCode}, #{name}, #{type}, #{status}, #{createdBy})
      """)
  int insert(SysTenant tenant);
}
