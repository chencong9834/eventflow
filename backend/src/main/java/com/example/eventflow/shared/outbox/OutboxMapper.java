package com.example.eventflow.shared.outbox;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OutboxMapper {

  @Insert(
      """
      INSERT INTO outbox_event (id, event_id, event_type, payload, published)
      VALUES (#{id}, #{eventId}, #{eventType}, #{payload}, 0)
      """)
  int insert(OutboxEvent event);

  @Select("SELECT * FROM outbox_event WHERE published = 0 ORDER BY id LIMIT 50")
  List<OutboxEvent> findUnpublished();

  @Update("UPDATE outbox_event SET published = 1, published_at = CURRENT_TIMESTAMP(3) WHERE id = #{id}")
  int markPublished(@Param("id") Long id);
}
