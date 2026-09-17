package com.example.eventflow.shared.error;

public final class ErrorCode {

  public static final String BAD_REQUEST = "BAD_REQUEST";
  public static final String UNAUTHORIZED = "UNAUTHORIZED";
  public static final String FORBIDDEN = "FORBIDDEN";
  public static final String NOT_FOUND = "NOT_FOUND";
  public static final String INVALID_CREDENTIAL = "INVALID_CREDENTIAL";
  public static final String LOGIN_RATE_LIMITED = "LOGIN_RATE_LIMITED";
  public static final String ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
  public static final String TENANT_DISABLED = "TENANT_DISABLED";
  public static final String TENANT_SWITCH_FORBIDDEN = "TENANT_SWITCH_FORBIDDEN";
  public static final String TENANT_CODE_TAKEN = "TENANT_CODE_TAKEN";
  public static final String USERNAME_TAKEN = "USERNAME_TAKEN";
  public static final String BUILTIN_TENANT_LOCKED = "BUILTIN_TENANT_LOCKED";
  public static final String LAST_ADMIN_REQUIRED = "LAST_ADMIN_REQUIRED";
  public static final String ACTIVITY_LOCKED = "ACTIVITY_LOCKED";
  public static final String REVIEW_NOT_PENDING = "REVIEW_NOT_PENDING";
  public static final String SOLD_OUT = "SOLD_OUT";
  public static final String NOT_ON_SALE = "NOT_ON_SALE";
  public static final String ORDER_NOT_PAYABLE = "ORDER_NOT_PAYABLE";
  public static final String TICKET_USED = "TICKET_USED";
  public static final String REFUND_NOT_ALLOWED = "REFUND_NOT_ALLOWED";
  public static final String LIMIT_EXCEEDED = "LIMIT_EXCEEDED";

  private ErrorCode() {}
}
