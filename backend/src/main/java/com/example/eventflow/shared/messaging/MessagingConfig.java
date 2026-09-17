package com.example.eventflow.shared.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class MessagingConfig {

  public static final String CLOSE_EXCHANGE = "eventflow.close";
  public static final String CLOSE_DELAY_QUEUE = "eventflow.order.close.delay";
  public static final String CLOSE_WORK_QUEUE = "eventflow.order.close";
  public static final String CLOSE_ROUTING_KEY = "order.close";

  @Bean
  public DirectExchange closeExchange() {
    return new DirectExchange(CLOSE_EXCHANGE, true, false);
  }

  /** TTL 到期后进入死信队列，由关单消费者处理。 */
  @Bean
  public Queue closeDelayQueue() {
    return QueueBuilder.durable(CLOSE_DELAY_QUEUE)
        .deadLetterExchange(CLOSE_EXCHANGE)
        .deadLetterRoutingKey(CLOSE_ROUTING_KEY)
        .build();
  }

  @Bean
  public Queue closeWorkQueue() {
    return QueueBuilder.durable(CLOSE_WORK_QUEUE).build();
  }

  @Bean
  public Binding closeWorkBinding(Queue closeWorkQueue, DirectExchange closeExchange) {
    return BindingBuilder.bind(closeWorkQueue).to(closeExchange).with(CLOSE_ROUTING_KEY);
  }
}
