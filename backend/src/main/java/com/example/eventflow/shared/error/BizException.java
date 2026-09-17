package com.example.eventflow.shared.error;

import org.springframework.http.HttpStatus;

public class BizException extends RuntimeException {

  private final String code;
  private final HttpStatus status;

  public BizException(String code, String message, HttpStatus status) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public String getCode() {
    return code;
  }

  public HttpStatus getStatus() {
    return status;
  }
}
