package com.example.eventflow.activity;

import java.time.LocalDateTime;

public class TicketTier {

  private Long id;
  private Long tenantId;
  private Long showId;
  private String name;
  private Long unitPriceFen;
  private Integer perUserLimit;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Long createdBy;
  private Integer totalQty;
  private Integer availableQty;
  private Integer reservedQty;
  private Integer soldQty;

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

  public Long getShowId() {
    return showId;
  }

  public void setShowId(Long showId) {
    this.showId = showId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getUnitPriceFen() {
    return unitPriceFen;
  }

  public void setUnitPriceFen(Long unitPriceFen) {
    this.unitPriceFen = unitPriceFen;
  }

  public Integer getPerUserLimit() {
    return perUserLimit;
  }

  public void setPerUserLimit(Integer perUserLimit) {
    this.perUserLimit = perUserLimit;
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

  public Integer getTotalQty() {
    return totalQty;
  }

  public void setTotalQty(Integer totalQty) {
    this.totalQty = totalQty;
  }

  public Integer getAvailableQty() {
    return availableQty;
  }

  public void setAvailableQty(Integer availableQty) {
    this.availableQty = availableQty;
  }

  public Integer getReservedQty() {
    return reservedQty;
  }

  public void setReservedQty(Integer reservedQty) {
    this.reservedQty = reservedQty;
  }

  public Integer getSoldQty() {
    return soldQty;
  }

  public void setSoldQty(Integer soldQty) {
    this.soldQty = soldQty;
  }
}
