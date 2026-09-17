package com.example.eventflow.audit;

import java.time.LocalDateTime;

public class AuditOperationLog {

  private Long id;
  private Long actorTenantId;
  private Long actorUserId;
  private String objectType;
  private Long objectId;
  private Long objectTenantId;
  private String fromStatus;
  private String toStatus;
  private String comment;
  private LocalDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getActorTenantId() {
    return actorTenantId;
  }

  public void setActorTenantId(Long actorTenantId) {
    this.actorTenantId = actorTenantId;
  }

  public Long getActorUserId() {
    return actorUserId;
  }

  public void setActorUserId(Long actorUserId) {
    this.actorUserId = actorUserId;
  }

  public String getObjectType() {
    return objectType;
  }

  public void setObjectType(String objectType) {
    this.objectType = objectType;
  }

  public Long getObjectId() {
    return objectId;
  }

  public void setObjectId(Long objectId) {
    this.objectId = objectId;
  }

  public Long getObjectTenantId() {
    return objectTenantId;
  }

  public void setObjectTenantId(Long objectTenantId) {
    this.objectTenantId = objectTenantId;
  }

  public String getFromStatus() {
    return fromStatus;
  }

  public void setFromStatus(String fromStatus) {
    this.fromStatus = fromStatus;
  }

  public String getToStatus() {
    return toStatus;
  }

  public void setToStatus(String toStatus) {
    this.toStatus = toStatus;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
