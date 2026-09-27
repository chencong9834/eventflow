package com.example.eventflow.ticket;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TicketMapper {

  @Insert(
      """
      INSERT INTO ticket
        (id, tenant_id, order_id, buyer_user_id, show_id, ticket_tier_id, ticket_no, verify_code, status)
      VALUES
        (#{id}, #{tenantId}, #{orderId}, #{buyerUserId}, #{showId}, #{ticketTierId}, #{ticketNo}, #{verifyCode}, #{status})
      """)
  int insert(Ticket ticket);

  @Select("SELECT * FROM ticket WHERE order_id = #{orderId} ORDER BY id")
  List<Ticket> findByOrderId(@Param("orderId") Long orderId);

  @Select(
      """
      SELECT * FROM ticket
      WHERE buyer_user_id = #{buyerUserId}
      ORDER BY id DESC
      """)
  List<Ticket> findByBuyer(@Param("buyerUserId") Long buyerUserId);

  @Select("SELECT * FROM ticket WHERE verify_code = #{verifyCode}")
  Ticket findByVerifyCode(@Param("verifyCode") String verifyCode);

  @Update(
      """
      UPDATE ticket SET status = 'USED', used_at = CURRENT_TIMESTAMP(3)
      WHERE id = #{id} AND status = 'UNUSED'
      """)
  int markUsed(@Param("id") Long id);

  @Update("UPDATE ticket SET status = 'VOID' WHERE order_id = #{orderId} AND status = 'UNUSED'")
  int voidUnusedByOrder(@Param("orderId") Long orderId);

  @Select("SELECT COUNT(*) FROM ticket WHERE order_id = #{orderId} AND status = 'USED'")
  int countUsed(@Param("orderId") Long orderId);

  @Select("SELECT COUNT(*) FROM ticket WHERE order_id = #{orderId} AND status = 'UNUSED'")
  int countUnused(@Param("orderId") Long orderId);
}
