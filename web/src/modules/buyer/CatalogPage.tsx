import { Button, Card, Col, Empty, Pagination, Row, Tag, Typography } from "antd";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { http } from "../../shared/http";
import type { ActivitySummary, ApiResponse, PageResult } from "../../shared/types";
import { CoverImage } from "../../shared/CoverImage";

export function CatalogPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const query = useQuery({
    queryKey: ["catalog", "activities", page, size],
    queryFn: async () => {
      const res = (await http.get("/catalog/activities", { params: { page, size } })) as ApiResponse<
        PageResult<ActivitySummary>
      >;
      return res.data;
    }
  });
  const items = query.data?.items ?? [];

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0, marginBottom: 8 }}>
        在售活动
      </Typography.Title>
      <Typography.Paragraph type="secondary">选择一场活动，按票档下单后模拟支付即可出票。</Typography.Paragraph>
      {query.isLoading ? (
        <Row gutter={[16, 16]}>
          {[1, 2, 3].map((key) => (
            <Col xs={24} sm={12} lg={8} key={key}>
              <Card loading />
            </Col>
          ))}
        </Row>
      ) : items.length === 0 ? (
        <Card>
          <Empty description="当前没有在售活动。请用平台账号审核通过一场活动后再来。" />
        </Card>
      ) : (
        <Row gutter={[16, 16]}>
          {items.map((item) => (
            <Col xs={24} sm={12} lg={8} key={item.id}>
              <Card
                className="catalog-card"
                hoverable
                cover={<CoverImage src={item.coverUrl} title={item.title} />}
                onClick={() => navigate(`/buyer/activities/${item.id}`)}
              >
                <Tag color="green" style={{ marginBottom: 8 }}>
                  售卖中
                </Tag>
                <Typography.Title level={5} style={{ margin: "0 0 8px" }} ellipsis>
                  {item.title}
                </Typography.Title>
                <Typography.Paragraph type="secondary" ellipsis={{ rows: 2 }} style={{ minHeight: 44 }}>
                  {item.description || item.organizerName || "主办方活动"}
                </Typography.Paragraph>
                <Button type="primary" block>
                  立即购票
                </Button>
              </Card>
            </Col>
          ))}
        </Row>
      )}
      {(query.data?.total ?? 0) > 0 ? (
        <Pagination
          style={{ marginTop: 16, textAlign: "right" }}
          current={page}
          pageSize={size}
          total={query.data?.total ?? 0}
          showSizeChanger
          pageSizeOptions={["10", "20", "50", "100"]}
          onChange={(nextPage, nextSize) => {
            setPage(nextSize === size ? nextPage : 1);
            setSize(nextSize);
          }}
        />
      ) : null}
    </div>
  );
}
