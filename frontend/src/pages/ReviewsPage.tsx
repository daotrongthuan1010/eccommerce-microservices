import { useState } from 'react';
import { Button, Card, Form, Input, InputNumber, Space, message } from 'antd';
import {
  useModerateReviewMutation,
  useCreateReviewMutation,
  useDeleteReviewMutation,
} from '../features/review/reviewApi';
import { useAppSelector } from '../app/hooks';
// NOTE: path đúng từ src/pages là ../mocks (task ghi ../../mocks sẽ trỏ
// ra ngoài src và làm build fail).
import { reviewListMock, type ReviewListItem } from '../mocks/reviewListMock';
import {
  PageHeader,
  DataTable,
  ApiErrorAlert,
  StatusTag,
  FormSection,
} from '../components/ui';

// Wave 2 task 2.4 — ReviewsPage: moderation (MODERATOR/ADMIN) + tạo review
// với Idempotency-Key (uuid mỗi submit, giữ nguyên khi retry). List đọc
// mock (backend chưa có GET list — contract §6).
export function ReviewsPage() {
  const roles = useAppSelector((s) => s.auth.roles);
  const canModerate = roles.includes('MODERATOR') || roles.includes('ADMIN');
  const [form] = Form.useForm();
  const [idemKey, setIdemKey] = useState(() => crypto.randomUUID());

  const [moderate, moderateState] = useModerateReviewMutation();
  const [createReview, createState] = useCreateReviewMutation();
  const [remove, removeState] = useDeleteReviewMutation();

  const err =
    (moderateState.error as { message?: string } | undefined)?.message ??
    (createState.error as { message?: string } | undefined)?.message ??
    (removeState.error as { message?: string } | undefined)?.message;

  const onModerate = async (id: string, action: 'APPROVE' | 'REJECT' | 'HIDE') => {
    try {
      await moderate({ id, body: { action } }).unwrap();
      message.success(`Đã ${action} review ${id}`);
    } catch {
      message.error('Moderation thất bại');
    }
  };

  return (
    <div>
      <PageHeader title="Đánh giá" subTitle="Moderation (MODERATOR/ADMIN) — list là mock" />
      <ApiErrorAlert message={err ?? null} />
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Card size="small" title="Danh sách review (MOCK — src/mocks/reviewListMock.ts)">
          <DataTable<ReviewListItem>
            columns={[
              { title: 'ID', dataIndex: 'id', key: 'id', width: 110 },
              { title: 'Sản phẩm', dataIndex: 'productName', key: 'productName' },
              { title: 'Tác giả', dataIndex: 'author', key: 'author', width: 140 },
              { title: 'Sao', dataIndex: 'rating', key: 'rating', width: 70 },
              {
                title: 'Trạng thái',
                dataIndex: 'status',
                key: 'status',
                width: 130,
                render: (v: string) => <StatusTag status={v} />,
              },
              {
                title: 'Thao tác',
                key: 'action',
                width: 260,
                render: (_: unknown, r: ReviewListItem) =>
                  canModerate ? (
                    <Space>
                      <Button size="small" type="primary" loading={moderateState.isLoading} onClick={() => onModerate(r.id, 'APPROVE')}>
                        Approve
                      </Button>
                      <Button size="small" danger loading={moderateState.isLoading} onClick={() => onModerate(r.id, 'REJECT')}>
                        Reject
                      </Button>
                      <Button
                        size="small"
                        danger
                        loading={removeState.isLoading}
                        onClick={async () => {
                          try {
                            await remove(r.id).unwrap();
                            message.success('Đã xóa review');
                          } catch {
                            message.error('Xóa thất bại');
                          }
                        }}
                      >
                        Xoa
                      </Button>
                    </Space>
                  ) : (
                    'Chi MODERATOR/ADMIN'
                  ),
              },
            ]}
            data={reviewListMock}
            rowKey="id"
          />
        </Card>
        <Card size="small" title="Tao review (POST /api/review/api/reviews + Idempotency-Key → 201)">
          <FormSection title="Form">
            <Form
              form={form}
              layout="inline"
              onFinish={async (v: { productId: string; rating: number; title?: string; content?: string }) => {
                try {
                  await createReview({ body: v, idempotencyKey: idemKey }).unwrap();
                  message.success('Đã tạo review — tạo key mới cho submit tiếp theo');
                  setIdemKey(crypto.randomUUID());
                  form.resetFields();
                } catch {
                  message.error('Tạo review thất bại — retry GIỮ NGUYÊN key bên dưới để tránh tạo trùng');
                }
              }}
            >
              <Form.Item name="productId" rules={[{ required: true, message: 'Nhập productId' }]}>
                <Input placeholder="productId" style={{ width: 180 }} />
              </Form.Item>
              <Form.Item name="rating" rules={[{ required: true, message: 'Nhập rating' }]}>
                <InputNumber min={1} max={5} placeholder="1-5" />
              </Form.Item>
              <Form.Item name="title">
                <Input placeholder="Tiêu đề" style={{ width: 200 }} />
              </Form.Item>
              <Form.Item name="content">
                <Input placeholder="Nội dung" style={{ width: 240 }} />
              </Form.Item>
              <Form.Item>
                <Button type="primary" htmlType="submit" loading={createState.isLoading}>
                  Gửi review
                </Button>
              </Form.Item>
            </Form>
            <p style={{ marginTop: 8 }}>
              Idempotency-Key hiện tại: <code>{idemKey}</code> (retry giữ nguyên key này)
            </p>
          </FormSection>
        </Card>
      </Space>
    </div>
  );
}

export default ReviewsPage;
