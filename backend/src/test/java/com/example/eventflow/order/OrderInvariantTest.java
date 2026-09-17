package com.example.eventflow.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.eventflow.activity.ActivityService;
import com.example.eventflow.activity.web.ActivitySummaryResponse.ActivityDetailResponse;
import com.example.eventflow.activity.web.ReviewDecideRequest;
import com.example.eventflow.activity.web.UpsertActivityRequest;
import com.example.eventflow.activity.web.UpsertShowRequest;
import com.example.eventflow.activity.web.UpsertTierRequest;
import com.example.eventflow.boot.EventflowApplication;
import com.example.eventflow.inventory.Inventory;
import com.example.eventflow.inventory.InventoryMapper;
import com.example.eventflow.order.web.CreateOrderRequest;
import com.example.eventflow.order.web.OrderResponse;
import com.example.eventflow.shared.error.BizException;
import com.example.eventflow.shared.error.ErrorCode;
import com.example.eventflow.shared.security.AuthPrincipal;
import com.example.eventflow.tenant.TenantService;
import com.example.eventflow.tenant.web.CreateOrganizerTenantRequest;
import com.example.eventflow.ticket.Ticket;
import com.example.eventflow.ticket.TicketMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 覆盖 PROJECT-CONTRACT 第 5 节要求的自动化测试项：超卖、重复支付、重复核销、跨租户读写、关单与已支付竞态。
 */
@SpringBootTest(classes = EventflowApplication.class)
@ActiveProfiles("test")
@Testcontainers
class OrderInvariantTest {

  private static final int POOL_SIZE = 24;

  @Container
  static final MySQLContainer<?> mysql =
      new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
          .withDatabaseName("eventflow")
          .withUsername("eventflow")
          .withPassword("eventflow")
          .withUrlParam("connectionTimeZone", "UTC")
          .withUrlParam("forceConnectionTimeZoneToSession", "true");

  @Container
  static final GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @Container
  static final RabbitMQContainer rabbit =
      new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("spring.datasource.hikari.maximum-pool-size", () -> POOL_SIZE);
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    registry.add("spring.rabbitmq.host", rabbit::getHost);
    registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
    registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
    registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
  }

  static final AdjustableClock CLOCK = new AdjustableClock();

  @TestConfiguration
  static class ClockOverride {
    @Bean
    @Primary
    Clock adjustableClock() {
      return CLOCK;
    }
  }

  private static final AuthPrincipal PLATFORM =
      new AuthPrincipal(2001L, 1001L, TenantService.TYPE_PLATFORM, "platform", "PLATFORM", List.of());
  private static final AuthPrincipal ORGANIZER =
      new AuthPrincipal(
          2002L, 1003L, TenantService.TYPE_ORGANIZER, "organizer", "ORGANIZER_ADMIN", List.of());
  private static final AuthPrincipal BUYER =
      new AuthPrincipal(2003L, 1002L, TenantService.TYPE_BUYER, "buyer", "BUYER", List.of());

  private static final AtomicInteger SEQ = new AtomicInteger();

  @Autowired private OrderService orderService;
  @Autowired private ActivityService activityService;
  @Autowired private TenantService tenantService;
  @Autowired private InventoryMapper inventoryMapper;
  @Autowired private TicketMapper ticketMapper;
  @Autowired private TicketOrderMapper orderMapper;

  @BeforeEach
  void resetClock() {
    CLOCK.reset();
  }

  @Test
  @DisplayName("并发下单不超卖：成功数恰好等于库存，且 available+reserved+sold==total")
  void concurrentOrdersNeverOversell() throws Exception {
    int stock = 10;
    int threads = 30;
    OnSaleTier tier = publishOnSaleTier(stock, 1000);

    List<String> outcomes =
        runConcurrently(
            threads,
            () -> {
              try {
                orderService.place(BUYER, orderRequest(tier, 1));
                return "OK";
              } catch (BizException ex) {
                return ex.getCode();
              }
            });

    assertThat(outcomes.stream().filter("OK"::equals).count()).isEqualTo(stock);
    assertThat(outcomes.stream().filter(ErrorCode.SOLD_OUT::equals).count())
        .isEqualTo(threads - stock);

    Inventory inventory = inventoryMapper.findByTicketTierId(tier.tierId());
    assertThat(inventory.getAvailableQty()).isZero();
    assertThat(inventory.getReservedQty()).isEqualTo(stock);
    assertThat(inventory.getSoldQty()).isZero();
    assertInvariant(inventory);
  }

  @Test
  @DisplayName("并发重复支付幂等：只出一份票，sold 只加一次")
  void concurrentDuplicatePaymentIssuesTicketsOnce() throws Exception {
    OnSaleTier tier = publishOnSaleTier(10, 5);
    OrderResponse order = orderService.place(BUYER, orderRequest(tier, 2));

    List<String> outcomes =
        runConcurrently(
            8,
            () -> {
              try {
                orderService.pay(BUYER, order.getId(), true);
                return "OK";
              } catch (BizException ex) {
                return ex.getCode();
              }
            });

    assertThat(outcomes).contains("OK");
    assertThat(ticketMapper.findByOrderId(order.getId())).hasSize(2);
    assertThat(orderMapper.findById(order.getId()).getStatus()).isEqualTo(OrderService.PAID);

    Inventory inventory = inventoryMapper.findByTicketTierId(tier.tierId());
    assertThat(inventory.getSoldQty()).isEqualTo(2);
    assertThat(inventory.getReservedQty()).isZero();
    assertThat(inventory.getAvailableQty()).isEqualTo(8);
    assertInvariant(inventory);
  }

  @Test
  @DisplayName("并发重复核销：同一票码只有一次成功，其余报 TICKET_USED")
  void concurrentVerifyMarksTicketUsedOnce() throws Exception {
    OnSaleTier tier = publishOnSaleTier(5, 5);
    OrderResponse order = orderService.place(BUYER, orderRequest(tier, 1));
    orderService.pay(BUYER, order.getId(), true);
    String verifyCode = ticketMapper.findByOrderId(order.getId()).get(0).getVerifyCode();

    List<String> outcomes =
        runConcurrently(
            8,
            () -> {
              try {
                orderService.verify(ORGANIZER, verifyCode);
                return "OK";
              } catch (BizException ex) {
                return ex.getCode();
              }
            });

    assertThat(outcomes.stream().filter("OK"::equals).count()).isEqualTo(1);
    assertThat(outcomes.stream().filter(ErrorCode.TICKET_USED::equals).count()).isEqualTo(7);
    assertThat(ticketMapper.countUsed(order.getId())).isEqualTo(1);
    assertThat(orderMapper.findById(order.getId()).getStatus()).isEqualTo(OrderService.FULFILLED);
  }

  @Test
  @DisplayName("已核销订单不可整单退款，库存不会被错误回补")
  void usedTicketBlocksRefund() {
    OnSaleTier tier = publishOnSaleTier(5, 5);
    OrderResponse order = orderService.place(BUYER, orderRequest(tier, 1));
    orderService.pay(BUYER, order.getId(), true);
    String verifyCode = ticketMapper.findByOrderId(order.getId()).get(0).getVerifyCode();
    orderService.verify(ORGANIZER, verifyCode);

    assertThatThrownBy(() -> orderService.refund(ORGANIZER, order.getId()))
        .isInstanceOf(BizException.class)
        .extracting(ex -> ((BizException) ex).getCode())
        .isEqualTo(ErrorCode.REFUND_NOT_ALLOWED);

    Inventory inventory = inventoryMapper.findByTicketTierId(tier.tierId());
    assertThat(inventory.getSoldQty()).isEqualTo(1);
    assertThat(inventory.getAvailableQty()).isEqualTo(4);
    assertInvariant(inventory);
  }

  @Test
  @DisplayName("跨租户：另一个主办方读不到、核销不了、退不了本单")
  void otherOrganizerCannotTouchForeignOrder() {
    OnSaleTier tier = publishOnSaleTier(5, 5);
    OrderResponse order = orderService.place(BUYER, orderRequest(tier, 1));
    orderService.pay(BUYER, order.getId(), true);
    Ticket ticket = ticketMapper.findByOrderId(order.getId()).get(0);
    AuthPrincipal rival = createRivalOrganizerAdmin();

    assertThat(codeOf(() -> orderService.getOrganizer(rival, order.getId())))
        .isEqualTo(ErrorCode.NOT_FOUND);
    assertThat(codeOf(() -> orderService.verify(rival, ticket.getVerifyCode())))
        .isEqualTo(ErrorCode.NOT_FOUND);
    assertThat(codeOf(() -> orderService.refund(rival, order.getId())))
        .isEqualTo(ErrorCode.NOT_FOUND);

    assertThat(orderService.listTenant(rival)).isEmpty();
    assertThat(ticketMapper.findByOrderId(order.getId()).get(0).getStatus()).isEqualTo("UNUSED");
  }

  @Test
  @DisplayName("购票用户只能看自己的订单，主办方不能替买家下单")
  void roleBoundariesAreEnforced() {
    OnSaleTier tier = publishOnSaleTier(5, 5);

    assertThat(codeOf(() -> orderService.place(ORGANIZER, orderRequest(tier, 1))))
        .isEqualTo(ErrorCode.FORBIDDEN);
    assertThat(codeOf(() -> orderService.listTenant(BUYER))).isEqualTo(ErrorCode.FORBIDDEN);
    assertThat(codeOf(() -> orderService.listPlatform(ORGANIZER, Instant.now(), Instant.now())))
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("关单与已支付竞态：已支付订单忽略关单，未支付超时订单回补库存")
  void closeIgnoresPaidOrderAndReleasesExpiredOne() {
    OnSaleTier tier = publishOnSaleTier(10, 10);

    OrderResponse paid = orderService.place(BUYER, orderRequest(tier, 2));
    orderService.pay(BUYER, paid.getId(), true);
    OrderResponse pending = orderService.place(BUYER, orderRequest(tier, 3));

    CLOCK.advance(Duration.ofMinutes(30));
    orderService.closeIfCreated(paid.getId());
    orderService.closeIfCreated(pending.getId());
    orderService.closeIfCreated(pending.getId());

    assertThat(orderMapper.findById(paid.getId()).getStatus()).isEqualTo(OrderService.PAID);
    assertThat(orderMapper.findById(pending.getId()).getStatus())
        .isEqualTo(OrderService.CANCELLED);
    assertThat(ticketMapper.findByOrderId(paid.getId())).hasSize(2);
    assertThat(ticketMapper.findByOrderId(pending.getId())).isEmpty();

    Inventory inventory = inventoryMapper.findByTicketTierId(tier.tierId());
    assertThat(inventory.getSoldQty()).isEqualTo(2);
    assertThat(inventory.getReservedQty()).isZero();
    assertThat(inventory.getAvailableQty()).isEqualTo(8);
    assertInvariant(inventory);
  }

  @Test
  @DisplayName("退款成功后库存回到可售，票券作废，不可重复退")
  void refundRestoresStockAndIsNotRepeatable() {
    OnSaleTier tier = publishOnSaleTier(5, 5);
    OrderResponse order = orderService.place(BUYER, orderRequest(tier, 2));
    orderService.pay(BUYER, order.getId(), true);

    orderService.refund(ORGANIZER, order.getId());

    assertThat(orderMapper.findById(order.getId()).getStatus()).isEqualTo(OrderService.REFUNDED);
    assertThat(ticketMapper.findByOrderId(order.getId()))
        .allSatisfy(ticket -> assertThat(ticket.getStatus()).isEqualTo("VOID"));
    assertThat(codeOf(() -> orderService.refund(ORGANIZER, order.getId())))
        .isEqualTo(ErrorCode.REFUND_NOT_ALLOWED);

    Inventory inventory = inventoryMapper.findByTicketTierId(tier.tierId());
    assertThat(inventory.getSoldQty()).isZero();
    assertThat(inventory.getAvailableQty()).isEqualTo(5);
    assertInvariant(inventory);
  }

  @Test
  @DisplayName("超过每人限购时拒绝下单，且不占用库存")
  void perUserLimitIsEnforced() {
    OnSaleTier tier = publishOnSaleTier(10, 2);
    orderService.place(BUYER, orderRequest(tier, 2));

    assertThat(codeOf(() -> orderService.place(BUYER, orderRequest(tier, 1))))
        .isEqualTo(ErrorCode.LIMIT_EXCEEDED);

    Inventory inventory = inventoryMapper.findByTicketTierId(tier.tierId());
    assertThat(inventory.getReservedQty()).isEqualTo(2);
    assertThat(inventory.getAvailableQty()).isEqualTo(8);
    assertInvariant(inventory);
  }

  private static void assertInvariant(Inventory inventory) {
    assertThat(inventory.getAvailableQty() + inventory.getReservedQty() + inventory.getSoldQty())
        .isEqualTo(inventory.getTotalQty());
  }

  private static String codeOf(Runnable call) {
    try {
      call.run();
      return "OK";
    } catch (BizException ex) {
      return ex.getCode();
    }
  }

  private List<String> runConcurrently(int threads, Callable<String> task) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      CountDownLatch startGun = new CountDownLatch(1);
      List<Future<String>> futures = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        futures.add(
            pool.submit(
                () -> {
                  startGun.await();
                  return task.call();
                }));
      }
      startGun.countDown();
      List<String> outcomes = new ArrayList<>();
      for (Future<String> future : futures) {
        outcomes.add(future.get(90, TimeUnit.SECONDS));
      }
      return outcomes;
    } finally {
      pool.shutdownNow();
    }
  }

  private CreateOrderRequest orderRequest(OnSaleTier tier, int qty) {
    CreateOrderRequest request = new CreateOrderRequest();
    request.setShowId(tier.showId());
    request.setTicketTierId(tier.tierId());
    request.setQty(qty);
    return request;
  }

  /** 建活动、场次、票档，提交并由平台通过，返回可下单的场次与票档。 */
  private OnSaleTier publishOnSaleTier(int totalQty, int perUserLimit) {
    UpsertActivityRequest activityRequest = new UpsertActivityRequest();
    activityRequest.setTitle("不变量测试活动 " + SEQ.incrementAndGet());
    ActivityDetailResponse detail = activityService.create(ORGANIZER, activityRequest);
    Long activityId = detail.getId();

    Instant now = CLOCK.instant();
    UpsertShowRequest showRequest = new UpsertShowRequest();
    showRequest.setName("场次");
    showRequest.setStartAt(now.plus(2, ChronoUnit.HOURS));
    showRequest.setEndAt(now.plus(4, ChronoUnit.HOURS));
    showRequest.setSaleStartAt(now.minus(1, ChronoUnit.HOURS));
    showRequest.setSaleEndAt(now.plus(3, ChronoUnit.HOURS));
    detail = activityService.addShow(ORGANIZER, activityId, showRequest);
    Long showId = detail.getShows().get(0).getId();

    UpsertTierRequest tierRequest = new UpsertTierRequest();
    tierRequest.setName("普通票");
    tierRequest.setUnitPriceFen(1000L);
    tierRequest.setPerUserLimit(perUserLimit);
    tierRequest.setTotalQty(totalQty);
    detail = activityService.addTier(ORGANIZER, showId, tierRequest);
    Long tierId = detail.getShows().get(0).getTiers().get(0).getId();

    activityService.submit(ORGANIZER, activityId);
    ReviewDecideRequest decision = new ReviewDecideRequest();
    decision.setDecision(ActivityService.APPROVED);
    activityService.decide(PLATFORM, activityId, decision);
    return new OnSaleTier(showId, tierId);
  }

  private AuthPrincipal createRivalOrganizerAdmin() {
    int n = SEQ.incrementAndGet();
    CreateOrganizerTenantRequest request = new CreateOrganizerTenantRequest();
    request.setTenantCode("org-rival-" + n);
    request.setName("竞品主办方 " + n);
    request.setAdminUsername("rival_admin_" + n);
    request.setAdminPassword("Passw0rd!");
    request.setAdminDisplayName("竞品管理员");
    var created = tenantService.createOrganizer(request, PLATFORM.getUserId());
    return new AuthPrincipal(
        created.getUsers().get(0).getId(),
        created.getId(),
        TenantService.TYPE_ORGANIZER,
        request.getAdminUsername(),
        TenantService.ORGANIZER_ADMIN,
        List.of());
  }

  private record OnSaleTier(Long showId, Long tierId) {}

  /** 可推进的 UTC 时钟，用来确定性地触发支付超时关单，而不是让测试 sleep。 */
  static final class AdjustableClock extends Clock {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.now());

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now.get();
    }

    void reset() {
      now.set(Instant.now());
    }

    void advance(Duration amount) {
      now.updateAndGet(current -> current.plus(amount));
    }
  }
}
