package com.example.eventflow.audit;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuditOperationLogMapper {

  @Insert(
      """
      INSERT INTO audit_operation_log
        (id, actor_tenant_id, actor_user_id, object_type, object_id, object_tenant_id,
         from_status, to_status, comment)
      VALUES
        (#{id}, #{actorTenantId}, #{actorUserId}, #{objectType}, #{objectId}, #{objectTenantId},
         #{fromStatus}, #{toStatus}, #{comment})
      """)
  int insert(AuditOperationLog log);

  @Select(
      """
      SELECT * FROM audit_operation_log
      WHERE object_type = #{objectType} AND object_id = #{objectId}
      ORDER BY created_at
      """)
  List<AuditOperationLog> findByObject(
      @Param("objectType") String objectType, @Param("objectId") Long objectId);
}
