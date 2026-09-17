package com.example.eventflow.activity;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityShowMapper {

  @Select("SELECT * FROM activity_show WHERE activity_id = #{activityId} ORDER BY start_at")
  List<ActivityShow> findByActivityId(@Param("activityId") Long activityId);

  @Select("SELECT * FROM activity_show WHERE id = #{id} AND tenant_id = #{tenantId}")
  ActivityShow findByIdAndTenant(@Param("id") Long id, @Param("tenantId") Long tenantId);

  @Select("SELECT * FROM activity_show WHERE id = #{id}")
  ActivityShow findById(@Param("id") Long id);

  @Insert(
      """
      INSERT INTO activity_show
        (id, tenant_id, activity_id, name, start_at, end_at, sale_start_at, sale_end_at, created_by)
      VALUES
        (#{id}, #{tenantId}, #{activityId}, #{name}, #{startAt}, #{endAt}, #{saleStartAt}, #{saleEndAt}, #{createdBy})
      """)
  int insert(ActivityShow show);

  @Update(
      """
      UPDATE activity_show
      SET name = #{name}, start_at = #{startAt}, end_at = #{endAt},
          sale_start_at = #{saleStartAt}, sale_end_at = #{saleEndAt}
      WHERE id = #{id} AND tenant_id = #{tenantId}
      """)
  int update(ActivityShow show);
}
