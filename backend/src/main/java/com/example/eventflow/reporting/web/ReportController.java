package com.example.eventflow.reporting.web;

import com.example.eventflow.reporting.ReportMapper;
import com.example.eventflow.reporting.ReportSummary;
import com.example.eventflow.shared.api.ApiResponse;
import com.example.eventflow.shared.error.BizException;
import com.example.eventflow.shared.error.ErrorCode;
import com.example.eventflow.shared.security.AuthPrincipal;
import com.example.eventflow.shared.time.Utc;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizer/reports")
public class ReportController {

  private final ReportMapper reportMapper;

  public ReportController(ReportMapper reportMapper) {
    this.reportMapper = reportMapper;
  }

  @PreAuthorize("hasAuthority('report:read')")
  @GetMapping
  public ApiResponse<ReportResponse> summary(
      @AuthenticationPrincipal AuthPrincipal principal,
      @RequestParam Instant from,
      @RequestParam Instant to) {
    if (!to.isAfter(from)) {
      throw new BizException(ErrorCode.BAD_REQUEST, "结束时间必须晚于开始时间", HttpStatus.BAD_REQUEST);
    }
    if (from.plusSeconds(366L * 24 * 3600).isBefore(to)) {
      throw new BizException(ErrorCode.BAD_REQUEST, "查询窗口不能超过一年", HttpStatus.BAD_REQUEST);
    }
    ReportSummary sales = reportMapper.sales(principal.getTenantId(), Utc.from(from), Utc.from(to));
    if (sales == null) {
      sales = new ReportSummary();
    }
    ReportResponse dto = new ReportResponse();
    dto.setFrom(from);
    dto.setTo(to);
    dto.setPaidOrderCount(sales.getPaidOrderCount());
    dto.setSoldQty(sales.getSoldQty());
    dto.setSalesFen(sales.getSalesFen());
    dto.setRefundFen(reportMapper.refundFen(principal.getTenantId(), Utc.from(from), Utc.from(to)));
    dto.setUsedTicketCount(reportMapper.usedTickets(principal.getTenantId(), Utc.from(from), Utc.from(to)));
    return ApiResponse.ok(dto);
  }
}
