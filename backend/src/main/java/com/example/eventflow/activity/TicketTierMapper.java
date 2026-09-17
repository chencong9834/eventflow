package com.example.eventflow.activity;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TicketTierMapper {

  @Select(
      """
      SELECT t.*, i.total_qty, i.available_qty, i.reserved_qty, i.sold_qty
      FROM ticket_tier t
      INNER JOIN inventory i ON i.ticket_tier_id = t.id
      WHERE t.show_id = #{showId}
      ORDER BY t.unit_price_fen
      """)
  List<TicketTier> findByShowId(@Param("showId") Long showId);

  @Select(
      """
      SELECT t.*, i.total_qty, i.available_qty, i.reserved_qty, i.sold_qty
      FROM ticket_tier t
      INNER JOIN inventory i ON i.ticket_tier_id = t.id
      WHERE t.id = #{id} AND t.tenant_id = #{tenantId}
      """)
  TicketTier findByIdAndTenant(@Param("id") Long id, @Param("tenantId") Long tenantId);

  @Select(
      """
      SELECT t.*, i.total_qty, i.available_qty, i.reserved_qty, i.sold_qty
      FROM ticket_tier t
      INNER JOIN inventory i ON i.ticket_tier_id = t.id
      WHERE t.id = #{id}
      """)
  TicketTier findById(@Param("id") Long id);

  @Insert(
      """
      INSERT INTO ticket_tier
        (id, tenant_id, show_id, name, unit_price_fen, per_user_limit, created_by)
      VALUES
        (#{id}, #{tenantId}, #{showId}, #{name}, #{unitPriceFen}, #{perUserLimit}, #{createdBy})
      """)
  int insert(TicketTier tier);

  @Update(
      """
      UPDATE ticket_tier
      SET name = #{name}, unit_price_fen = #{unitPriceFen}, per_user_limit = #{perUserLimit}
      WHERE id = #{id} AND tenant_id = #{tenantId}
      """)
  int update(TicketTier tier);
}
