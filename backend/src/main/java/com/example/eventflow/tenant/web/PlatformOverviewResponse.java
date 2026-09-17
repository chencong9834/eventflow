package com.example.eventflow.tenant.web;

public class PlatformOverviewResponse {

  private int tenantTotal;
  private int organizerActive;
  private int organizerDisabled;
  private int userTotal;
  private int pendingReviewHint;

  public int getTenantTotal() {
    return tenantTotal;
  }

  public void setTenantTotal(int tenantTotal) {
    this.tenantTotal = tenantTotal;
  }

  public int getOrganizerActive() {
    return organizerActive;
  }

  public void setOrganizerActive(int organizerActive) {
    this.organizerActive = organizerActive;
  }

  public int getOrganizerDisabled() {
    return organizerDisabled;
  }

  public void setOrganizerDisabled(int organizerDisabled) {
    this.organizerDisabled = organizerDisabled;
  }

  public int getUserTotal() {
    return userTotal;
  }

  public void setUserTotal(int userTotal) {
    this.userTotal = userTotal;
  }

  public int getPendingReviewHint() {
    return pendingReviewHint;
  }

  public void setPendingReviewHint(int pendingReviewHint) {
    this.pendingReviewHint = pendingReviewHint;
  }
}
