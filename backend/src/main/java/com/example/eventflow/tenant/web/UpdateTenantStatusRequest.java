package com.example.eventflow.tenant.web;

public class UpdateTenantStatusRequest {

  @jakarta.validation.constraints.NotBlank
  @jakarta.validation.constraints.Pattern(regexp = "ACTIVE|DISABLED")
  private String status;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
