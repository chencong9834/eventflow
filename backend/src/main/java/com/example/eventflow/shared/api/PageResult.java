package com.example.eventflow.shared.api;

import java.util.List;

public class PageResult<T> {

  private final List<T> items;
  private final int page;
  private final int size;
  private final long total;

  public PageResult(List<T> items, int page, int size, long total) {
    this.items = items;
    this.page = page;
    this.size = size;
    this.total = total;
  }

  public static <T> PageResult<T> of(List<T> items, PageQuery query, long total) {
    return new PageResult<>(items, query.page(), query.size(), total);
  }

  public List<T> getItems() {
    return items;
  }

  public int getPage() {
    return page;
  }

  public int getSize() {
    return size;
  }

  public long getTotal() {
    return total;
  }
}
