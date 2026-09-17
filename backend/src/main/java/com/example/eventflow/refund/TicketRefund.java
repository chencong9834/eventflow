package com.example.eventflow.refund;

public class TicketRefund {

  private Long id;
  private Long orderId;
  private Long tenantId;
  private Long amountFen;
  private String status;
  private Long createdBy;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public Long getAmountFen() {
    return amountFen;
  }

  public void setAmountFen(Long amountFen) {
    this.amountFen = amountFen;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(Long createdBy) {
    this.createdBy = createdBy;
  }
}
