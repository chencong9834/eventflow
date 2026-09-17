package com.example.eventflow.payment;

public class Payment {

  private Long id;
  private Long orderId;
  private Long tenantId;
  private Long amountFen;
  private String channel;
  private String status;
  private String simulatedResult;

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

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getSimulatedResult() {
    return simulatedResult;
  }

  public void setSimulatedResult(String simulatedResult) {
    this.simulatedResult = simulatedResult;
  }
}
