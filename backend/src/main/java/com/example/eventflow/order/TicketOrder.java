package com.example.eventflow.order;

import java.time.LocalDateTime;

public class TicketOrder {

  private Long id;
  private String orderNo;
  private Long tenantId;
  private Long buyerUserId;
  private Long activityId;
  private Long showId;
  private Long ticketTierId;
  private String activityTitle;
  private String showName;
  private String tierName;
  private Integer qty;
  private Long unitPriceFen;
  private Long amountFen;
  private String status;
  private LocalDateTime payDeadlineAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Long createdBy;

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

  public Long getBuyerUserId() {
    return buyerUserId;
  }

  public void setBuyerUserId(Long buyerUserId) {
    this.buyerUserId = buyerUserId;
  }

  public Long getActivityId() {
    return activityId;
  }

  public void setActivityId(Long activityId) {
    this.activityId = activityId;
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

  public Integer getQty() {
    return qty;
  }

  public void setQty(Integer qty) {
    this.qty = qty;
  }

  public Long getUnitPriceFen() {
    return unitPriceFen;
  }

  public void setUnitPriceFen(Long unitPriceFen) {
    this.unitPriceFen = unitPriceFen;
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

  public LocalDateTime getPayDeadlineAt() {
    return payDeadlineAt;
  }

  public void setPayDeadlineAt(LocalDateTime payDeadlineAt) {
    this.payDeadlineAt = payDeadlineAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(Long createdBy) {
    this.createdBy = createdBy;
  }
}
