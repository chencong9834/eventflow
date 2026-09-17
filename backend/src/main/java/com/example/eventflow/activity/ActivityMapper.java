package com.example.eventflow.activity;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityMapper {

  @Select(
      """
      SELECT a.*,
             (SELECT COUNT(*) FROM activity_show s WHERE s.activity_id = a.id) AS show_count
      FROM activity a
      WHERE a.tenant_id = #{tenantId}
      ORDER BY a.updated_at DESC
      """)
  List<Activity> findByTenant(@Param("tenantId") Long tenantId);

  @Select("SELECT * FROM activity WHERE id = #{id} AND tenant_id = #{tenantId}")
  Activity findByIdAndTenant(@Param("id") Long id, @Param("tenantId") Long tenantId);

  @Select(
      """
      SELECT a.*, t.name AS organizer_name, t.tenant_code AS organizer_code
      FROM activity a
      INNER JOIN sys_tenant t ON t.id = a.tenant_id
      WHERE a.id = #{id}
      """)
  Activity findById(@Param("id") Long id);

  @Select(
      """
      SELECT a.*, t.name AS organizer_name, t.tenant_code AS organizer_code,
             (SELECT COUNT(*) FROM activity_show s WHERE s.activity_id = a.id) AS show_count
      FROM activity a
      INNER JOIN sys_tenant t ON t.id = a.tenant_id
      WHERE a.review_status = #{reviewStatus}
      ORDER BY a.updated_at
      """)
  List<Activity> findByReviewStatus(@Param("reviewStatus") String reviewStatus);

  @Select("SELECT COUNT(*) FROM activity WHERE review_status = #{reviewStatus}")
  int countByReviewStatus(@Param("reviewStatus") String reviewStatus);

  @Select(
      """
      SELECT a.*, t.name AS organizer_name, t.tenant_code AS organizer_code,
             (SELECT COUNT(*) FROM activity_show s2 WHERE s2.activity_id = a.id) AS show_count
      FROM activity a
      INNER JOIN sys_tenant t ON t.id = a.tenant_id
      WHERE a.review_status = 'APPROVED'
        AND a.sale_status = 'ON_SALE'
        AND t.status = 'ACTIVE'
        AND EXISTS (
          SELECT 1 FROM activity_show s
          WHERE s.activity_id = a.id
            AND s.sale_start_at <= #{now}
            AND s.sale_end_at >= #{now}
        )
      ORDER BY a.updated_at DESC
      """)
  List<Activity> findOnSaleCatalog(@Param("now") java.time.LocalDateTime now);

  @Insert(
      """
      INSERT INTO activity
        (id, tenant_id, title, description, cover_url, review_status, sale_status, created_by)
      VALUES
        (#{id}, #{tenantId}, #{title}, #{description}, #{coverUrl}, #{reviewStatus}, #{saleStatus}, #{createdBy})
      """)
  int insert(Activity activity);

  @Update(
      """
      UPDATE activity
      SET title = #{title}, description = #{description}, cover_url = #{coverUrl}
      WHERE id = #{id} AND tenant_id = #{tenantId}
      """)
  int updateMeta(Activity activity);

  @Update(
      """
      UPDATE activity
      SET review_status = #{reviewStatus}, sale_status = #{saleStatus}
      WHERE id = #{id}
      """)
  int updateStatuses(
      @Param("id") Long id,
      @Param("reviewStatus") String reviewStatus,
      @Param("saleStatus") String saleStatus);
}
