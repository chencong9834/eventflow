package com.example.eventflow.shared.outbox;

import com.example.eventflow.order.OrderProperties;
import com.example.eventflow.order.OrderService;
import com.example.eventflow.order.TicketOrder;
import com.example.eventflow.order.TicketOrderMapper;
import com.example.eventflow.shared.messaging.MessagingConfig;
import com.example.eventflow.shared.time.Utc;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {

  private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

  private final OutboxMapper outboxMapper;
  private final InboxMapper inboxMapper;
  private final RabbitTemplate rabbitTemplate;
  private final OrderService orderService;
  private final TicketOrderMapper orderMapper;
  private final OrderProperties orderProperties;
  private final Clock clock;

  public OutboxPublisher(
      OutboxMapper outboxMapper,
      InboxMapper inboxMapper,
      RabbitTemplate rabbitTemplate,
      OrderService orderService,
      TicketOrderMapper orderMapper,
      OrderProperties orderProperties,
      Clock clock) {
    this.outboxMapper = outboxMapper;
    this.inboxMapper = inboxMapper;
    this.rabbitTemplate = rabbitTemplate;
    this.orderService = orderService;
    this.orderMapper = orderMapper;
    this.orderProperties = orderProperties;
    this.clock = clock;
  }

  @Scheduled(fixedDelay = 1000)
  public void publish() {
    long delayMs = Math.max(1000L, orderProperties.getPayTimeoutSeconds() * 1000L);
    for (OutboxEvent event : outboxMapper.findUnpublished()) {
      try {
        if (OrderService.ORDER_CREATED.equals(event.getEventType())) {
          rabbitTemplate.convertAndSend(
              "",
              MessagingConfig.CLOSE_DELAY_QUEUE,
              event.getPayload(),
              message -> {
                message.getMessageProperties().setExpiration(String.valueOf(delayMs));
                message.getMessageProperties().setHeader("eventId", event.getEventId());
                return message;
              });
        }
        outboxMapper.markPublished(event.getId());
      } catch (RuntimeException ex) {
        log.warn("outbox publish failed id={}", event.getId(), ex);
      }
    }
  }

  @Scheduled(fixedDelay = 15000)
  public void scanExpired() {
    for (TicketOrder order : orderMapper.findExpiredCreated(Utc.now(clock))) {
      orderService.closeIfCreated(order.getId());
    }
  }

  @RabbitListener(queues = MessagingConfig.CLOSE_WORK_QUEUE)
  public void onClose(String orderId, org.springframework.amqp.core.Message message) {
    orderService.closeIfCreated(Long.valueOf(orderId));
    Object eventId = message.getMessageProperties().getHeaders().get("eventId");
    String inboxId = eventId == null ? "close-" + orderId : eventId.toString();
    inboxMapper.insertIgnore(inboxId, "OrderClose");
  }
}
