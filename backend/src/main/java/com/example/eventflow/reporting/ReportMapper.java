package com.example.eventflow.reporting;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportMapper {

  @Select(
      """
      SELECT
        COUNT(*) AS paid_order_count,
        COALESCE(SUM(qty), 0) AS sold_qty,
        COALESCE(SUM(amount_fen), 0) AS sales_fen
      FROM ticket_order
      WHERE tenant_id = #{tenantId}
        AND status IN ('PAID', 'FULFILLED', 'REFUNDED')
        AND created_at >= #{from}
        AND created_at < #{to}
      """)
  ReportSummary sales(
      @Param("tenantId") Long tenantId,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to);

  @Select(
      """
      SELECT COALESCE(SUM(amount_fen), 0)
      FROM ticket_refund
      WHERE tenant_id = #{tenantId} AND status = 'SUCCEEDED'
        AND created_at >= #{from} AND created_at < #{to}
      """)
  long refundFen(
      @Param("tenantId") Long tenantId,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to);

  @Select(
      """
      SELECT COUNT(*)
      FROM ticket t
      INNER JOIN ticket_order o ON o.id = t.order_id
      WHERE o.tenant_id = #{tenantId} AND t.status = 'USED'
        AND t.used_at >= #{from} AND t.used_at < #{to}
      """)
  int usedTickets(
      @Param("tenantId") Long tenantId,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to);
}
