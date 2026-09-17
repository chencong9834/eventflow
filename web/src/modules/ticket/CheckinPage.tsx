import { Button, Card, Form, Input, Result, Typography, message } from "antd";
import { useMutation } from "@tanstack/react-query";
import { Controller, useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { http, ApiError } from "../../shared/http";
import { ticketLabel } from "../../shared/format";
import type { ApiResponse, IssuedTicket } from "../../shared/types";

const schema = z.object({
  verifyCode: z.string().trim().min(16, "请输入核销码")
});

export function CheckinPage() {
  const form = useForm<{ verifyCode: string }>({
    resolver: zodResolver(schema),
    defaultValues: { verifyCode: "" }
  });
  const verify = useMutation({
    mutationFn: async (verifyCode: string) => {
      const res = (await http.post("/organizer/tickets/verify", { verifyCode })) as ApiResponse<IssuedTicket>;
      return res.data;
    },
    onSuccess: () => {
      message.success("核销成功");
      form.reset();
    },
    onError: (e) => message.error(e instanceof ApiError ? e.message : "核销失败")
  });

  return (
    <Card title="现场核销">
      <Typography.Paragraph type="secondary">输入购票用户出示的核销码。重复核销会被拒绝。</Typography.Paragraph>
      <Form layout="inline" onFinish={form.handleSubmit((v) => verify.mutate(v.verifyCode))}>
        <Form.Item
          validateStatus={form.formState.errors.verifyCode ? "error" : ""}
          help={form.formState.errors.verifyCode?.message}
        >
          <Controller
            name="verifyCode"
            control={form.control}
            render={({ field }) => <Input {...field} style={{ width: 360 }} placeholder="核销码" />}
          />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" loading={verify.isPending}>
            核销
          </Button>
        </Form.Item>
      </Form>
      {verify.data ? (
        <Result status="success" title="核销成功" subTitle={`票号 ${verify.data.ticketNo} · ${ticketLabel(verify.data.status)}`} />
      ) : null}
    </Card>
  );
}
