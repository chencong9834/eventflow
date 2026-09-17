package com.example.eventflow.inventory;

public class Inventory {

  private Long id;
  private Long tenantId;
  private Long ticketTierId;
  private Integer totalQty;
  private Integer availableQty;
  private Integer reservedQty;
  private Integer soldQty;
  private Long createdBy;

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

  public Long getTicketTierId() {
    return ticketTierId;
  }

  public void setTicketTierId(Long ticketTierId) {
    this.ticketTierId = ticketTierId;
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

  public Long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(Long createdBy) {
    this.createdBy = createdBy;
  }
}
