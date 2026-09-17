package com.example.eventflow.activity;

import java.time.LocalDateTime;

public class Activity {

  private Long id;
  private Long tenantId;
  private String title;
  private String description;
  private String coverUrl;
  private String reviewStatus;
  private String saleStatus;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Long createdBy;
  private String organizerName;
  private String organizerCode;
  private Integer showCount;

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

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCoverUrl() {
    return coverUrl;
  }

  public void setCoverUrl(String coverUrl) {
    this.coverUrl = coverUrl;
  }

  public String getReviewStatus() {
    return reviewStatus;
  }

  public void setReviewStatus(String reviewStatus) {
    this.reviewStatus = reviewStatus;
  }

  public String getSaleStatus() {
    return saleStatus;
  }

  public void setSaleStatus(String saleStatus) {
    this.saleStatus = saleStatus;
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

  public String getOrganizerName() {
    return organizerName;
  }

  public void setOrganizerName(String organizerName) {
    this.organizerName = organizerName;
  }

  public String getOrganizerCode() {
    return organizerCode;
  }

  public void setOrganizerCode(String organizerCode) {
    this.organizerCode = organizerCode;
  }

  public Integer getShowCount() {
    return showCount;
  }

  public void setShowCount(Integer showCount) {
    this.showCount = showCount;
  }
}
