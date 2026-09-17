package com.example.eventflow.activity;

import java.time.LocalDateTime;

public class ActivityShow {

  private Long id;
  private Long tenantId;
  private Long activityId;
  private String name;
  private LocalDateTime startAt;
  private LocalDateTime endAt;
  private LocalDateTime saleStartAt;
  private LocalDateTime saleEndAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
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

  public Long getActivityId() {
    return activityId;
  }

  public void setActivityId(Long activityId) {
    this.activityId = activityId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public LocalDateTime getStartAt() {
    return startAt;
  }

  public void setStartAt(LocalDateTime startAt) {
    this.startAt = startAt;
  }

  public LocalDateTime getEndAt() {
    return endAt;
  }

  public void setEndAt(LocalDateTime endAt) {
    this.endAt = endAt;
  }

  public LocalDateTime getSaleStartAt() {
    return saleStartAt;
  }

  public void setSaleStartAt(LocalDateTime saleStartAt) {
    this.saleStartAt = saleStartAt;
  }

  public LocalDateTime getSaleEndAt() {
    return saleEndAt;
  }

  public void setSaleEndAt(LocalDateTime saleEndAt) {
    this.saleEndAt = saleEndAt;
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
