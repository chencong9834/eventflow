package com.example.eventflow.inventory;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryMapper {

  @Select("SELECT * FROM inventory WHERE ticket_tier_id = #{ticketTierId}")
  Inventory findByTicketTierId(@Param("ticketTierId") Long ticketTierId);

  @Insert(
      """
      INSERT INTO inventory
        (id, tenant_id, ticket_tier_id, total_qty, available_qty, reserved_qty, sold_qty, created_by)
      VALUES
        (#{id}, #{tenantId}, #{ticketTierId}, #{totalQty}, #{availableQty}, #{reservedQty}, #{soldQty}, #{createdBy})
      """)
  int insert(Inventory inventory);

  @Update(
      """
      UPDATE inventory
      SET total_qty = #{totalQty}, available_qty = #{availableQty}
      WHERE ticket_tier_id = #{ticketTierId} AND reserved_qty = 0 AND sold_qty = 0
      """)
  int resetTotalIfUnsold(
      @Param("ticketTierId") Long ticketTierId,
      @Param("totalQty") int totalQty,
      @Param("availableQty") int availableQty);

  @Update(
      """
      UPDATE inventory
      SET available_qty = available_qty - #{qty}, reserved_qty = reserved_qty + #{qty}
      WHERE ticket_tier_id = #{ticketTierId} AND available_qty >= #{qty}
      """)
  int reserve(@Param("ticketTierId") Long ticketTierId, @Param("qty") int qty);

  @Update(
      """
      UPDATE inventory
      SET reserved_qty = reserved_qty - #{qty}, sold_qty = sold_qty + #{qty}
      WHERE ticket_tier_id = #{ticketTierId} AND reserved_qty >= #{qty}
      """)
  int confirmSold(@Param("ticketTierId") Long ticketTierId, @Param("qty") int qty);

  @Update(
      """
      UPDATE inventory
      SET reserved_qty = reserved_qty - #{qty}, available_qty = available_qty + #{qty}
      WHERE ticket_tier_id = #{ticketTierId} AND reserved_qty >= #{qty}
      """)
  int releaseReserved(@Param("ticketTierId") Long ticketTierId, @Param("qty") int qty);

  @Update(
      """
      UPDATE inventory
      SET sold_qty = sold_qty - #{qty}, available_qty = available_qty + #{qty}
      WHERE ticket_tier_id = #{ticketTierId} AND sold_qty >= #{qty}
      """)
  int restoreSold(@Param("ticketTierId") Long ticketTierId, @Param("qty") int qty);
}
