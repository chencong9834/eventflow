import type { TablePaginationConfig } from "antd";

export function listPagination(
  page: number,
  size: number,
  total: number,
  onChange: (page: number, size: number) => void
): TablePaginationConfig {
  return {
    current: page,
    pageSize: size,
    total,
    showSizeChanger: true,
    pageSizeOptions: ["10", "20", "50", "100"],
    onChange: (nextPage, nextSize) => onChange(nextSize === size ? nextPage : 1, nextSize)
  };
}
