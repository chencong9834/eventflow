package com.example.eventflow.notification;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SiteNoticeMapper {

  @Insert(
      """
      INSERT INTO site_notice (id, tenant_id, user_id, title, body)
      VALUES (#{id}, #{tenantId}, #{userId}, #{title}, #{body})
      """)
  int insert(SiteNotice notice);

  @Select(
      """
      SELECT * FROM site_notice
      WHERE user_id = #{userId}
      ORDER BY created_at DESC
      """)
  List<SiteNotice> findByUser(@Param("userId") Long userId);
}
