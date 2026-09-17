package com.example.eventflow.order.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreateOrderRequest {

  @NotNull private Long showId;
  @NotNull private Long ticketTierId;

  @NotNull
  @Min(1)
  private Integer qty;

  public Long getShowId() {
    return showId;
  }

  public void setShowId(Long showId) {
    this.showId = showId;
  }

  public Long getTicketTierId() {
    return ticketTierId;
  }

  public void setTicketTierId(Long ticketTierId) {
    this.ticketTierId = ticketTierId;
  }

  public Integer getQty() {
    return qty;
  }

  public void setQty(Integer qty) {
    this.qty = qty;
  }
}
