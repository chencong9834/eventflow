package com.example.eventflow.shared.api;

import com.example.eventflow.shared.error.BizException;
import com.example.eventflow.shared.error.ErrorCode;
import org.springframework.http.HttpStatus;

public final class PageQuery {

  public static final int DEFAULT_PAGE = 1;
  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  private final int page;
  private final int size;

  private PageQuery(int page, int size) {
    this.page = page;
    this.size = size;
  }

  public static PageQuery of(Integer page, Integer size) {
    int resolvedPage = page == null ? DEFAULT_PAGE : page;
    int resolvedSize = size == null ? DEFAULT_SIZE : size;
    if (resolvedPage < 1 || resolvedSize < 1 || resolvedSize > MAX_SIZE) {
      throw new BizException(ErrorCode.BAD_REQUEST, "分页参数不合法", HttpStatus.BAD_REQUEST);
    }
    long offset = (long) (resolvedPage - 1) * resolvedSize;
    if (offset > Integer.MAX_VALUE) {
      throw new BizException(ErrorCode.BAD_REQUEST, "分页参数不合法", HttpStatus.BAD_REQUEST);
    }
    return new PageQuery(resolvedPage, resolvedSize);
  }

  public int page() {
    return page;
  }

  public int size() {
    return size;
  }

  public int offset() {
    return (page - 1) * size;
  }
}
