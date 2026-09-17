package com.example.eventflow.refund;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TicketRefundMapper {

  @Insert(
      """
      INSERT INTO ticket_refund (id, order_id, tenant_id, amount_fen, status, created_by)
      VALUES (#{id}, #{orderId}, #{tenantId}, #{amountFen}, #{status}, #{createdBy})
      """)
  int insert(TicketRefund refund);

  @Select("SELECT * FROM ticket_refund WHERE order_id = #{orderId}")
  TicketRefund findByOrderId(@Param("orderId") Long orderId);
}
