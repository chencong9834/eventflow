package com.example.eventflow.shared.outbox;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InboxMapper {

  @Insert("INSERT IGNORE INTO inbox_event (event_id, event_type) VALUES (#{eventId}, #{eventType})")
  int insertIgnore(@Param("eventId") String eventId, @Param("eventType") String eventType);
}
