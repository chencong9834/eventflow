package com.example.eventflow.order;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TicketOrderMapper {

  @Insert(
      """
      INSERT INTO ticket_order
        (id, order_no, tenant_id, buyer_user_id, activity_id, show_id, ticket_tier_id,
         activity_title, show_name, tier_name, qty, unit_price_fen, amount_fen, status,
         pay_deadline_at, created_by)
      VALUES
        (#{id}, #{orderNo}, #{tenantId}, #{buyerUserId}, #{activityId}, #{showId}, #{ticketTierId},
         #{activityTitle}, #{showName}, #{tierName}, #{qty}, #{unitPriceFen}, #{amountFen}, #{status},
         #{payDeadlineAt}, #{createdBy})
      """)
  int insert(TicketOrder order);

  @Select("SELECT * FROM ticket_order WHERE id = #{id}")
  TicketOrder findById(@Param("id") Long id);

  @Select(
      """
      SELECT * FROM ticket_order
      WHERE buyer_user_id = #{buyerUserId}
      ORDER BY created_at DESC
      """)
  List<TicketOrder> findByBuyer(@Param("buyerUserId") Long buyerUserId);

  @Select(
      """
      SELECT * FROM ticket_order
      WHERE tenant_id = #{tenantId}
      ORDER BY created_at DESC
      """)
  List<TicketOrder> findByTenant(@Param("tenantId") Long tenantId);

  @Select(
      """
      SELECT COALESCE(SUM(qty), 0) FROM ticket_order
      WHERE buyer_user_id = #{buyerUserId} AND ticket_tier_id = #{ticketTierId}
        AND status IN ('CREATED', 'PAID', 'FULFILLED')
      """)
  int sumActiveQty(
      @Param("buyerUserId") Long buyerUserId, @Param("ticketTierId") Long ticketTierId);

  @Update("UPDATE ticket_order SET status = #{toStatus} WHERE id = #{id} AND status = #{fromStatus}")
  int updateStatus(
      @Param("id") Long id, @Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus);

  @Select(
      """
      SELECT * FROM ticket_order
      WHERE status = 'CREATED' AND pay_deadline_at < #{now}
      ORDER BY pay_deadline_at
      LIMIT 100
      """)
  List<TicketOrder> findExpiredCreated(@Param("now") LocalDateTime now);

  @Select(
      """
      SELECT * FROM ticket_order
      WHERE created_at >= #{from} AND created_at < #{to}
      ORDER BY created_at DESC
      """)
  List<TicketOrder> findInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
