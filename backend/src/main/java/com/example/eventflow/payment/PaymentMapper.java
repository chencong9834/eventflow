package com.example.eventflow.payment;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentMapper {

  @Insert(
      """
      INSERT INTO payment (id, order_id, tenant_id, amount_fen, channel, status, simulated_result)
      VALUES (#{id}, #{orderId}, #{tenantId}, #{amountFen}, #{channel}, #{status}, #{simulatedResult})
      """)
  int insert(Payment payment);

  @Select("SELECT * FROM payment WHERE order_id = #{orderId}")
  Payment findByOrderId(@Param("orderId") Long orderId);
}
