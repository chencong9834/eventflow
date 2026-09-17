package com.example.eventflow.order.web;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OrderResponse {

  @JsonSerialize(using = ToStringSerializer.class)
  private Long id;

  private String orderNo;

  @JsonSerialize(using = ToStringSerializer.class)
  private Long tenantId;

  private String activityTitle;
  private String showName;
  private String tierName;
  private int qty;
  private long unitPriceFen;
  private long amountFen;
  private String status;
  private Instant payDeadlineAt;
  private Instant createdAt;
  private List<TicketResponse> tickets = new ArrayList<>();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getOrderNo() {
    return orderNo;
  }

  public void setOrderNo(String orderNo) {
    this.orderNo = orderNo;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public String getActivityTitle() {
    return activityTitle;
  }

  public void setActivityTitle(String activityTitle) {
    this.activityTitle = activityTitle;
  }

  public String getShowName() {
    return showName;
  }

  public void setShowName(String showName) {
    this.showName = showName;
  }

  public String getTierName() {
    return tierName;
  }

  public void setTierName(String tierName) {
    this.tierName = tierName;
  }

  public int getQty() {
    return qty;
  }

  public void setQty(int qty) {
    this.qty = qty;
  }

  public long getUnitPriceFen() {
    return unitPriceFen;
  }

  public void setUnitPriceFen(long unitPriceFen) {
    this.unitPriceFen = unitPriceFen;
  }

  public long getAmountFen() {
    return amountFen;
  }

  public void setAmountFen(long amountFen) {
    this.amountFen = amountFen;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getPayDeadlineAt() {
    return payDeadlineAt;
  }

  public void setPayDeadlineAt(Instant payDeadlineAt) {
    this.payDeadlineAt = payDeadlineAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public List<TicketResponse> getTickets() {
    return tickets;
  }

  public void setTickets(List<TicketResponse> tickets) {
    this.tickets = tickets;
  }

  public static class TicketResponse {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String ticketNo;
    private String verifyCode;
    private String status;
    private Instant usedAt;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getTicketNo() {
      return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
      this.ticketNo = ticketNo;
    }

    public String getVerifyCode() {
      return verifyCode;
    }

    public void setVerifyCode(String verifyCode) {
      this.verifyCode = verifyCode;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public Instant getUsedAt() {
      return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
      this.usedAt = usedAt;
    }
  }
}
