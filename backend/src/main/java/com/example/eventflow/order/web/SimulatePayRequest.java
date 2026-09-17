package com.example.eventflow.order.web;

import jakarta.validation.constraints.NotNull;

public class SimulatePayRequest {

  @NotNull private Boolean success;

  public Boolean getSuccess() {
    return success;
  }

  public void setSuccess(Boolean success) {
    this.success = success;
  }
}
