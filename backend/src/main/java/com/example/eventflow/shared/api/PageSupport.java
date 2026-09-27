package com.example.eventflow.shared.api;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/** 只分页紧接着执行的那一条查询，映射发生在分页上下文关闭之后。 */
public final class PageSupport {

  private PageSupport() {}

  public static <E, T> PageResult<T> query(
      PageQuery query, Supplier<List<E>> select, Function<E, T> mapper) {
    PageHelper.startPage(query.page(), query.size());
    List<E> queried;
    try {
      queried = select.get();
    } finally {
      PageHelper.clearPage();
    }
    long total = queried.size();
    int pageNum = query.page();
    int pageSize = query.size();
    if (queried instanceof Page<?> page) {
      total = page.getTotal();
      pageNum = page.getPageNum();
      pageSize = page.getPageSize();
    }
    List<T> items = new ArrayList<>(queried.size());
    for (E row : queried) {
      items.add(mapper.apply(row));
    }
    return new PageResult<>(items, pageNum, pageSize, total);
  }
}
