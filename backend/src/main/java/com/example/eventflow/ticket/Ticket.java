package com.example.eventflow.ticket;

import java.time.LocalDateTime;

public class Ticket {

  private Long id;
  private Long tenantId;
  private Long orderId;
  private Long buyerUserId;
  private Long showId;
  private Long ticketTierId;
  private String ticketNo;
  private String verifyCode;
  private String status;
  private LocalDateTime usedAt;
  private LocalDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public Long getBuyerUserId() {
    return buyerUserId;
  }

  public void setBuyerUserId(Long buyerUserId) {
    this.buyerUserId = buyerUserId;
  }

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

  public LocalDateTime getUsedAt() {
    return usedAt;
  }

  public void setUsedAt(LocalDateTime usedAt) {
    this.usedAt = usedAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
