package com.example.eventflow.order.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VerifyTicketRequest {

  @NotBlank
  @Size(min = 16, max = 64)
  private String verifyCode;

  public String getVerifyCode() {
    return verifyCode;
  }

  public void setVerifyCode(String verifyCode) {
    this.verifyCode = verifyCode;
  }
}
