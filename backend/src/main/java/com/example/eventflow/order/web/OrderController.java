package com.example.eventflow.order.web;

import com.example.eventflow.order.OrderService;
import com.example.eventflow.shared.api.ApiResponse;
import com.example.eventflow.shared.api.PageQuery;
import com.example.eventflow.shared.api.PageResult;
import com.example.eventflow.shared.security.AuthPrincipal;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OrderController {

  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @PreAuthorize("hasAuthority('order:write')")
  @PostMapping("/buyer/orders")
  public ApiResponse<OrderResponse> place(
      @AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody CreateOrderRequest request) {
    return ApiResponse.ok(orderService.place(principal, request));
  }

  @PreAuthorize("hasAuthority('order:write')")
  @GetMapping("/buyer/orders")
  public ApiResponse<PageResult<OrderResponse>> myOrders(
      @AuthenticationPrincipal AuthPrincipal principal,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    return ApiResponse.ok(orderService.listMine(principal, PageQuery.of(page, size)));
  }

  @PreAuthorize("hasAuthority('order:write')")
  @GetMapping("/buyer/orders/{id}")
  public ApiResponse<OrderResponse> myOrder(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
    return ApiResponse.ok(orderService.getBuyer(principal, id));
  }

  @PreAuthorize("hasAuthority('order:write')")
  @PostMapping("/buyer/orders/{id}/pay")
  public ApiResponse<OrderResponse> pay(
      @AuthenticationPrincipal AuthPrincipal principal,
      @PathVariable Long id,
      @Valid @RequestBody SimulatePayRequest request) {
    return ApiResponse.ok(orderService.pay(principal, id, Boolean.TRUE.equals(request.getSuccess())));
  }

  @PreAuthorize("hasAuthority('order:write')")
  @PostMapping("/buyer/orders/{id}/cancel")
  public ApiResponse<OrderResponse> cancel(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
    return ApiResponse.ok(orderService.cancel(principal, id));
  }

  @PreAuthorize("hasAuthority('ticket:read')")
  @GetMapping("/buyer/tickets")
  public ApiResponse<PageResult<OrderResponse.TicketResponse>> myTickets(
      @AuthenticationPrincipal AuthPrincipal principal,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    return ApiResponse.ok(orderService.listBuyerTickets(principal, PageQuery.of(page, size)));
  }

  @PreAuthorize("hasAuthority('order:read')")
  @GetMapping("/organizer/orders")
  public ApiResponse<PageResult<OrderResponse>> tenantOrders(
      @AuthenticationPrincipal AuthPrincipal principal,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    return ApiResponse.ok(orderService.listTenant(principal, PageQuery.of(page, size)));
  }

  @PreAuthorize("hasAuthority('order:read')")
  @GetMapping("/organizer/orders/{id}")
  public ApiResponse<OrderResponse> tenantOrder(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
    return ApiResponse.ok(orderService.getOrganizer(principal, id));
  }

  @PreAuthorize("hasAuthority('refund:write')")
  @PostMapping("/organizer/orders/{id}/refund")
  public ApiResponse<OrderResponse> refund(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
    return ApiResponse.ok(orderService.refund(principal, id));
  }

  @PreAuthorize("hasAuthority('ticket:verify')")
  @PostMapping("/organizer/tickets/verify")
  public ApiResponse<OrderResponse.TicketResponse> verify(
      @AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody VerifyTicketRequest request) {
    return ApiResponse.ok(orderService.verify(principal, request.getVerifyCode()));
  }

  @PreAuthorize("hasAuthority('order:read')")
  @GetMapping("/platform/orders")
  public ApiResponse<PageResult<OrderResponse>> platformOrders(
      @AuthenticationPrincipal AuthPrincipal principal,
      @RequestParam Instant from,
      @RequestParam Instant to,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    return ApiResponse.ok(orderService.listPlatform(principal, from, to, PageQuery.of(page, size)));
  }

  @PreAuthorize("hasAuthority('order:read')")
  @GetMapping("/platform/orders/{id}")
  public ApiResponse<OrderResponse> platformOrder(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
    return ApiResponse.ok(orderService.getPlatform(principal, id));
  }

  @PreAuthorize("hasAuthority('refund:write')")
  @PostMapping("/platform/orders/{id}/refund")
  public ApiResponse<OrderResponse> platformRefund(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
    return ApiResponse.ok(orderService.refundByPlatform(principal, id));
  }
}
