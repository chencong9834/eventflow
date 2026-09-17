package com.example.eventflow.order;

import com.example.eventflow.activity.Activity;
import com.example.eventflow.activity.ActivityMapper;
import com.example.eventflow.activity.ActivityService;
import com.example.eventflow.activity.ActivityShow;
import com.example.eventflow.activity.ActivityShowMapper;
import com.example.eventflow.activity.TicketTier;
import com.example.eventflow.activity.TicketTierMapper;
import com.example.eventflow.inventory.Inventory;
import com.example.eventflow.inventory.InventoryMapper;
import com.example.eventflow.inventory.RedisStockService;
import com.example.eventflow.notification.SiteNotice;
import com.example.eventflow.notification.SiteNoticeMapper;
import com.example.eventflow.order.web.CreateOrderRequest;
import com.example.eventflow.order.web.OrderResponse;
import com.example.eventflow.order.web.OrderResponse.TicketResponse;
import com.example.eventflow.payment.Payment;
import com.example.eventflow.payment.PaymentMapper;
import com.example.eventflow.refund.TicketRefund;
import com.example.eventflow.refund.TicketRefundMapper;
import com.example.eventflow.shared.error.BizException;
import com.example.eventflow.shared.error.ErrorCode;
import com.example.eventflow.shared.id.SnowflakeIdGenerator;
import com.example.eventflow.shared.outbox.OutboxEvent;
import com.example.eventflow.shared.outbox.OutboxMapper;
import com.example.eventflow.shared.security.AuthPrincipal;
import com.example.eventflow.shared.time.Utc;
import com.example.eventflow.tenant.TenantService;
import com.example.eventflow.ticket.Ticket;
import com.example.eventflow.ticket.TicketMapper;
import com.example.eventflow.identity.SysUser;
import com.example.eventflow.identity.SysUserMapper;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

  public static final String CREATED = "CREATED";
  public static final String PAID = "PAID";
  public static final String CANCELLED = "CANCELLED";
  public static final String REFUNDED = "REFUNDED";
  public static final String FULFILLED = "FULFILLED";
  public static final String ORDER_CREATED = "OrderCreated";

  private static final SecureRandom RANDOM = new SecureRandom();

  private final TicketOrderMapper orderMapper;
  private final PaymentMapper paymentMapper;
  private final TicketMapper ticketMapper;
  private final TicketRefundMapper refundMapper;
  private final InventoryMapper inventoryMapper;
  private final RedisStockService redisStock;
  private final ActivityMapper activityMapper;
  private final ActivityShowMapper showMapper;
  private final TicketTierMapper tierMapper;
  private final OutboxMapper outboxMapper;
  private final SiteNoticeMapper noticeMapper;
  private final SysUserMapper userMapper;
  private final SnowflakeIdGenerator ids;
  private final OrderProperties orderProperties;
  private final Clock clock;

  public OrderService(
      TicketOrderMapper orderMapper,
      PaymentMapper paymentMapper,
      TicketMapper ticketMapper,
      TicketRefundMapper refundMapper,
      InventoryMapper inventoryMapper,
      RedisStockService redisStock,
      ActivityMapper activityMapper,
      ActivityShowMapper showMapper,
      TicketTierMapper tierMapper,
      OutboxMapper outboxMapper,
      SiteNoticeMapper noticeMapper,
      SysUserMapper userMapper,
      SnowflakeIdGenerator ids,
      OrderProperties orderProperties,
      Clock clock) {
    this.orderMapper = orderMapper;
    this.paymentMapper = paymentMapper;
    this.ticketMapper = ticketMapper;
    this.refundMapper = refundMapper;
    this.inventoryMapper = inventoryMapper;
    this.redisStock = redisStock;
    this.activityMapper = activityMapper;
    this.showMapper = showMapper;
    this.tierMapper = tierMapper;
    this.outboxMapper = outboxMapper;
    this.noticeMapper = noticeMapper;
    this.userMapper = userMapper;
    this.ids = ids;
    this.orderProperties = orderProperties;
    this.clock = clock;
  }

  public List<OrderResponse> listMine(AuthPrincipal principal) {
    requireBuyer(principal);
    return orderMapper.findByBuyer(principal.getUserId()).stream().map(this::toResponse).toList();
  }

  public List<OrderResponse> listTenant(AuthPrincipal principal) {
    requireOrganizer(principal);
    return orderMapper.findByTenant(principal.getTenantId()).stream().map(this::toResponse).toList();
  }

  public OrderResponse getBuyer(AuthPrincipal principal, Long orderId) {
    requireBuyer(principal);
    TicketOrder order = requireOrder(orderId);
    if (!principal.getUserId().equals(order.getBuyerUserId())) {
      throw new BizException(ErrorCode.NOT_FOUND, "订单不存在", HttpStatus.NOT_FOUND);
    }
    return toResponse(order);
  }

  public OrderResponse getOrganizer(AuthPrincipal principal, Long orderId) {
    requireOrganizer(principal);
    TicketOrder order = requireOrder(orderId);
    if (!principal.getTenantId().equals(order.getTenantId())) {
      throw new BizException(ErrorCode.NOT_FOUND, "订单不存在", HttpStatus.NOT_FOUND);
    }
    return toResponse(order);
  }

  public List<TicketResponse> listBuyerTickets(AuthPrincipal principal) {
    requireBuyer(principal);
    return ticketMapper.findByBuyer(principal.getUserId()).stream().map(this::toTicket).toList();
  }

  @Transactional
  public OrderResponse place(AuthPrincipal principal, CreateOrderRequest request) {
    requireBuyer(principal);
    TicketTier tier = tierMapper.findById(request.getTicketTierId());
    ActivityShow show = showMapper.findById(request.getShowId());
    if (tier == null || show == null || !show.getId().equals(tier.getShowId())) {
      throw new BizException(ErrorCode.BAD_REQUEST, "场次与票档不匹配", HttpStatus.BAD_REQUEST);
    }
    Activity activity = activityMapper.findById(show.getActivityId());
    LocalDateTime now = Utc.now(clock);
    if (activity == null
        || !ActivityService.APPROVED.equals(activity.getReviewStatus())
        || !ActivityService.ON_SALE.equals(activity.getSaleStatus())
        || now.isBefore(show.getSaleStartAt())
        || now.isAfter(show.getSaleEndAt())) {
      throw new BizException(ErrorCode.NOT_ON_SALE, "不在售卖窗口或未上架", HttpStatus.CONFLICT);
    }
    int qty = request.getQty();
    if (qty > tier.getPerUserLimit()) {
      throw new BizException(ErrorCode.LIMIT_EXCEEDED, "超过单次限购", HttpStatus.CONFLICT);
    }
    int already = orderMapper.sumActiveQty(principal.getUserId(), tier.getId());
    if (already + qty > tier.getPerUserLimit()) {
      throw new BizException(ErrorCode.LIMIT_EXCEEDED, "超过每人限购", HttpStatus.CONFLICT);
    }
    boolean held =
        redisStock.tryReserve(
            tier.getId(),
            qty,
            () -> {
              Inventory inventory = inventoryMapper.findByTicketTierId(tier.getId());
              return inventory == null ? 0 : inventory.getAvailableQty();
            });
    if (!held) {
      throw new BizException(ErrorCode.SOLD_OUT, "库存不足", HttpStatus.CONFLICT);
    }
    TicketOrder order = new TicketOrder();
    order.setId(ids.nextId());
    order.setOrderNo("E" + order.getId());
    order.setTenantId(activity.getTenantId());
    order.setBuyerUserId(principal.getUserId());
    order.setActivityId(activity.getId());
    order.setShowId(show.getId());
    order.setTicketTierId(tier.getId());
    order.setActivityTitle(activity.getTitle());
    order.setShowName(show.getName());
    order.setTierName(tier.getName());
    order.setQty(qty);
    order.setUnitPriceFen(tier.getUnitPriceFen());
    order.setAmountFen(tier.getUnitPriceFen() * qty);
    order.setStatus(CREATED);
    order.setPayDeadlineAt(now.plusSeconds(orderProperties.getPayTimeoutSeconds()));
    order.setCreatedBy(principal.getUserId());
    orderMapper.insert(order);
    writeOutbox(ORDER_CREATED, order.getId().toString());
    notice(principal.getTenantId(), principal.getUserId(), "下单成功", "请在支付截止前完成模拟支付：" + order.getOrderNo());
    // 同一票档只有一行 inventory，扣减会拿到该行的排他锁并持有到提交，所有并发下单在这里串行。
    // 因此把它放在事务的最后一条语句，把临界区压到"一次条件更新 + 提交"，前面的插入不占锁。
    // 影响行数为 0 说明可售不足，抛异常整笔回滚，上面的插入一并撤销。
    if (inventoryMapper.reserve(tier.getId(), qty) == 0) {
      throw new BizException(ErrorCode.SOLD_OUT, "库存不足", HttpStatus.CONFLICT);
    }
    return toResponse(order);
  }

  @Transactional
  public OrderResponse pay(AuthPrincipal principal, Long orderId, boolean success) {
    requireBuyer(principal);
    TicketOrder order = requireBuyerOrder(principal, orderId);
    if (paymentMapper.findByOrderId(orderId) != null || !CREATED.equals(order.getStatus())) {
      if (PAID.equals(order.getStatus()) || FULFILLED.equals(order.getStatus())) {
        return toResponse(order);
      }
      throw new BizException(ErrorCode.ORDER_NOT_PAYABLE, "订单不可支付", HttpStatus.CONFLICT);
    }
    Payment payment = new Payment();
    payment.setId(ids.nextId());
    payment.setOrderId(order.getId());
    payment.setTenantId(order.getTenantId());
    payment.setAmountFen(order.getAmountFen());
    payment.setChannel("SIMULATED");
    if (success) {
      int paid = orderMapper.updateStatus(order.getId(), CREATED, PAID);
      if (paid == 0) {
        throw new BizException(ErrorCode.ORDER_NOT_PAYABLE, "订单不可支付", HttpStatus.CONFLICT);
      }
      issueTickets(order);
      payment.setStatus("SUCCEEDED");
      payment.setSimulatedResult("SUCCESS");
      paymentMapper.insert(payment);
      notice(principal.getTenantId(), principal.getUserId(), "支付成功", "已出票：" + order.getOrderNo());
      // 同 place：把争抢同一行 inventory 的更新放到事务末尾，缩短行锁持有时间。
      if (inventoryMapper.confirmSold(order.getTicketTierId(), order.getQty()) == 0) {
        throw new BizException(ErrorCode.SOLD_OUT, "预留库存异常", HttpStatus.CONFLICT);
      }
    } else {
      payment.setStatus("FAILED");
      payment.setSimulatedResult("FAILED");
      paymentMapper.insert(payment);
      cancelCreated(order, principal.getTenantId(), principal.getUserId(), "模拟支付失败，订单已关闭");
    }
    return toResponse(requireOrder(orderId));
  }

  @Transactional
  public OrderResponse cancel(AuthPrincipal principal, Long orderId) {
    requireBuyer(principal);
    TicketOrder order = requireBuyerOrder(principal, orderId);
    cancelCreated(order, principal.getTenantId(), principal.getUserId(), "已取消待支付订单");
    return toResponse(requireOrder(orderId));
  }

  @Transactional
  public void closeIfCreated(Long orderId) {
    TicketOrder order = orderMapper.findById(orderId);
    if (order == null || !CREATED.equals(order.getStatus())) {
      return;
    }
    if (Utc.now(clock).isBefore(order.getPayDeadlineAt())) {
      return;
    }
    int updated = orderMapper.updateStatus(order.getId(), CREATED, CANCELLED);
    if (updated == 0) {
      return;
    }
    if (inventoryMapper.releaseReserved(order.getTicketTierId(), order.getQty()) > 0) {
      redisStock.restoreAfterCommit(order.getTicketTierId(), order.getQty());
    }
    SysUser buyer = userMapper.findById(order.getBuyerUserId());
    if (buyer != null) {
      notice(buyer.getTenantId(), buyer.getId(), "订单关闭", "超时未支付，订单已关闭");
    }
  }

  @Transactional
  public TicketResponse verify(AuthPrincipal principal, String verifyCode) {
    requireOrganizer(principal);
    Ticket ticket = ticketMapper.findByVerifyCode(verifyCode.trim());
    if (ticket == null || !principal.getTenantId().equals(ticket.getTenantId())) {
      throw new BizException(ErrorCode.NOT_FOUND, "票券不存在", HttpStatus.NOT_FOUND);
    }
    if (!"UNUSED".equals(ticket.getStatus())) {
      throw new BizException(ErrorCode.TICKET_USED, "票券已核销或已作废", HttpStatus.CONFLICT);
    }
    ActivityShow show = showMapper.findById(ticket.getShowId());
    LocalDateTime now = Utc.now(clock);
    if (show == null || now.isAfter(show.getEndAt())) {
      throw new BizException(ErrorCode.BAD_REQUEST, "场次已结束，不能核销", HttpStatus.BAD_REQUEST);
    }
    if (ticketMapper.markUsed(ticket.getId()) == 0) {
      throw new BizException(ErrorCode.TICKET_USED, "票券已核销或已作废", HttpStatus.CONFLICT);
    }
    TicketOrder order = requireOrder(ticket.getOrderId());
    if (ticketMapper.countUnused(order.getId()) == 0) {
      orderMapper.updateStatus(order.getId(), PAID, FULFILLED);
    }
    return toTicket(ticketMapper.findByVerifyCode(verifyCode.trim()));
  }

  @Transactional
  public OrderResponse refund(AuthPrincipal principal, Long orderId) {
    requireOrganizer(principal);
    TicketOrder order = requireOrder(orderId);
    if (!principal.getTenantId().equals(order.getTenantId())) {
      throw new BizException(ErrorCode.NOT_FOUND, "订单不存在", HttpStatus.NOT_FOUND);
    }
    applyRefund(order, principal.getUserId(), "主办方整单退款");
    return toResponse(requireOrder(orderId));
  }

  public List<OrderResponse> listPlatform(AuthPrincipal principal, Instant from, Instant to) {
    requirePlatform(principal);
    if (from == null || to == null || !to.isAfter(from)) {
      throw new BizException(ErrorCode.BAD_REQUEST, "必须提供有效时间范围", HttpStatus.BAD_REQUEST);
    }
    return orderMapper.findInRange(Utc.from(from), Utc.from(to)).stream().map(this::toResponse).toList();
  }

  public OrderResponse getPlatform(AuthPrincipal principal, Long orderId) {
    requirePlatform(principal);
    return toResponse(requireOrder(orderId));
  }

  @Transactional
  public OrderResponse refundByPlatform(AuthPrincipal principal, Long orderId) {
    if (!TenantService.TYPE_PLATFORM.equals(principal.getTenantType())) {
      throw new BizException(ErrorCode.FORBIDDEN, "仅平台可做客服退款", HttpStatus.FORBIDDEN);
    }
    TicketOrder order = requireOrder(orderId);
    applyRefund(order, principal.getUserId(), "平台客服整单退款");
    return toResponse(requireOrder(orderId));
  }

  private void applyRefund(TicketOrder order, Long actorUserId, String noticePrefix) {
    if (!PAID.equals(order.getStatus())) {
      throw new BizException(ErrorCode.REFUND_NOT_ALLOWED, "仅已支付且未核销的订单可整单退款", HttpStatus.CONFLICT);
    }
    if (ticketMapper.countUsed(order.getId()) > 0) {
      throw new BizException(ErrorCode.REFUND_NOT_ALLOWED, "已有入场票，不能整单退款", HttpStatus.CONFLICT);
    }
    if (refundMapper.findByOrderId(order.getId()) != null) {
      throw new BizException(ErrorCode.REFUND_NOT_ALLOWED, "已发起过退款", HttpStatus.CONFLICT);
    }
    int updated = orderMapper.updateStatus(order.getId(), PAID, REFUNDED);
    if (updated == 0) {
      throw new BizException(ErrorCode.REFUND_NOT_ALLOWED, "订单状态已变化", HttpStatus.CONFLICT);
    }
    ticketMapper.voidUnusedByOrder(order.getId());
    if (inventoryMapper.restoreSold(order.getTicketTierId(), order.getQty()) == 0) {
      throw new BizException(ErrorCode.SOLD_OUT, "回补库存失败", HttpStatus.CONFLICT);
    }
    redisStock.restoreAfterCommit(order.getTicketTierId(), order.getQty());
    TicketRefund refund = new TicketRefund();
    refund.setId(ids.nextId());
    refund.setOrderId(order.getId());
    refund.setTenantId(order.getTenantId());
    refund.setAmountFen(order.getAmountFen());
    refund.setStatus("SUCCEEDED");
    refund.setCreatedBy(actorUserId);
    refundMapper.insert(refund);
    SysUser buyer = userMapper.findById(order.getBuyerUserId());
    if (buyer != null) {
      notice(buyer.getTenantId(), buyer.getId(), "退款成功", noticePrefix + "：" + order.getOrderNo());
    }
  }

  private TicketOrder requireBuyerOrder(AuthPrincipal principal, Long orderId) {
    TicketOrder order = requireOrder(orderId);
    if (!principal.getUserId().equals(order.getBuyerUserId())) {
      throw new BizException(ErrorCode.NOT_FOUND, "订单不存在", HttpStatus.NOT_FOUND);
    }
    return order;
  }

  private void cancelCreated(TicketOrder order, Long noticeTenantId, Long noticeUserId, String message) {
    int updated = orderMapper.updateStatus(order.getId(), CREATED, CANCELLED);
    if (updated == 0) {
      throw new BizException(ErrorCode.ORDER_NOT_PAYABLE, "订单已支付或已关闭", HttpStatus.CONFLICT);
    }
    if (inventoryMapper.releaseReserved(order.getTicketTierId(), order.getQty()) == 0) {
      throw new BizException(ErrorCode.SOLD_OUT, "释放预留库存失败", HttpStatus.CONFLICT);
    }
    redisStock.restoreAfterCommit(order.getTicketTierId(), order.getQty());
    notice(noticeTenantId, noticeUserId, "订单关闭", message);
  }

  private void issueTickets(TicketOrder order) {
    for (int i = 0; i < order.getQty(); i++) {
      Ticket ticket = new Ticket();
      ticket.setId(ids.nextId());
      ticket.setTenantId(order.getTenantId());
      ticket.setOrderId(order.getId());
      ticket.setBuyerUserId(order.getBuyerUserId());
      ticket.setShowId(order.getShowId());
      ticket.setTicketTierId(order.getTicketTierId());
      ticket.setTicketNo(randomCode());
      ticket.setVerifyCode(randomCode());
      ticket.setStatus("UNUSED");
      ticketMapper.insert(ticket);
    }
  }

  private void writeOutbox(String type, String payload) {
    OutboxEvent event = new OutboxEvent();
    event.setId(ids.nextId());
    event.setEventId(UUID.randomUUID().toString());
    event.setEventType(type);
    event.setPayload(payload);
    outboxMapper.insert(event);
  }

  private void notice(Long tenantId, Long userId, String title, String body) {
    SiteNotice notice = new SiteNotice();
    notice.setId(ids.nextId());
    notice.setTenantId(tenantId);
    notice.setUserId(userId);
    notice.setTitle(title);
    notice.setBody(body);
    noticeMapper.insert(notice);
  }

  private TicketOrder requireOrder(Long orderId) {
    TicketOrder order = orderMapper.findById(orderId);
    if (order == null) {
      throw new BizException(ErrorCode.NOT_FOUND, "订单不存在", HttpStatus.NOT_FOUND);
    }
    return order;
  }

  private OrderResponse toResponse(TicketOrder order) {
    OrderResponse dto = new OrderResponse();
    dto.setId(order.getId());
    dto.setOrderNo(order.getOrderNo());
    dto.setTenantId(order.getTenantId());
    dto.setActivityTitle(order.getActivityTitle());
    dto.setShowName(order.getShowName());
    dto.setTierName(order.getTierName());
    dto.setQty(order.getQty());
    dto.setUnitPriceFen(order.getUnitPriceFen());
    dto.setAmountFen(order.getAmountFen());
    dto.setStatus(order.getStatus());
    dto.setPayDeadlineAt(Utc.toInstant(order.getPayDeadlineAt()));
    dto.setCreatedAt(Utc.toInstant(order.getCreatedAt()));
    dto.setTickets(ticketMapper.findByOrderId(order.getId()).stream().map(this::toTicket).toList());
    return dto;
  }

  private TicketResponse toTicket(Ticket ticket) {
    TicketResponse dto = new TicketResponse();
    dto.setId(ticket.getId());
    dto.setTicketNo(ticket.getTicketNo());
    dto.setVerifyCode(ticket.getVerifyCode());
    dto.setStatus(ticket.getStatus());
    dto.setUsedAt(Utc.toInstant(ticket.getUsedAt()));
    return dto;
  }

  private static String randomCode() {
    byte[] bytes = new byte[16];
    RANDOM.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  private static void requireBuyer(AuthPrincipal principal) {
    if (!TenantService.TYPE_BUYER.equals(principal.getTenantType())) {
      throw new BizException(ErrorCode.FORBIDDEN, "仅购票用户可下单", HttpStatus.FORBIDDEN);
    }
  }

  private static void requireOrganizer(AuthPrincipal principal) {
    if (!TenantService.TYPE_ORGANIZER.equals(principal.getTenantType())) {
      throw new BizException(ErrorCode.FORBIDDEN, "仅主办方可操作", HttpStatus.FORBIDDEN);
    }
  }

  private static void requirePlatform(AuthPrincipal principal) {
    if (!TenantService.TYPE_PLATFORM.equals(principal.getTenantType())) {
      throw new BizException(ErrorCode.FORBIDDEN, "仅平台可查看跨租户订单", HttpStatus.FORBIDDEN);
    }
  }
}
