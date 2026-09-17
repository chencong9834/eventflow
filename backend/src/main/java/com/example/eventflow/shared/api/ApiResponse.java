package com.example.eventflow.shared.api;

public class ApiResponse<T> {

  private String code;
  private String message;
  private T data;

  public static <T> ApiResponse<T> ok(T data) {
    ApiResponse<T> res = new ApiResponse<>();
    res.code = "OK";
    res.message = "success";
    res.data = data;
    return res;
  }

  public static ApiResponse<Void> ok() {
    return ok(null);
  }

  public static <T> ApiResponse<T> error(String code, String message) {
    ApiResponse<T> res = new ApiResponse<>();
    res.code = code;
    res.message = message;
    return res;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public T getData() {
    return data;
  }
}
