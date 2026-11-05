import { useState } from 'react';
import { App, Button, Card, Descriptions, Drawer, Form, Input, Modal, Rate, Space, Tabs, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  useListPendingReviewsQuery,
  useListMyReviewsQuery,
  useDecideReviewMutation,
  useHideReviewMutation,
  useRestoreReviewMutation,
  useDeleteReviewMutation,
  useCreateReviewMutation,
  type Review,
} from '../features/review/reviewApi';
import { useAppSelector } from '../app/hooks';
import {
  ApiErrorAlert,
  DataTable,
  ListSkeleton,
  PageHeader,
  StatusTag,
  SummaryRow,
} from '../components/ui';

// review-fix 2026-11-05: boi lai tu mock + endpoint moderation sai (/moderate
// khong ton tai tren BE). 2 tab: Hang cho duyet (MODERATOR/ADMIN — data that
// tu GET /admin/reviews/moderation/revisions) + Review cua toi (data that tu
// GET /api/reviews/me). Duyet theo revision: isApproved → PUBLISH, false →
// REJECT kem ly do. FE-STYLE §8: PageHeader → SummaryRow → FilterBar →
// DataTable → Drawer 560.

export function ReviewsPage() {
  const { message } = App.useApp();
  const roles = useAppSelector((s) => s.auth.roles) ?? [];
  const canModerate = roles.includes('MODERATOR') || roles.includes('ADMIN');

  const [selected, setSelected] = useState<Review | null>(null);
  const [rejectOpen, setRejectOpen] = useState<Review | null>(null);
  const [rejectForm] = Form.useForm();
  const [createOpen, setCreateOpen] = useState(false);
  const [createForm] = Form.useForm();
  const [idemKey, setIdemKey] = useState(() => crypto.randomUUID());

  const pendingQ = useListPendingReviewsQuery(undefined, { skip: !canModerate });
  const mineQ = useListMyReviewsQuery();

  const [decide, decideState] = useDecideReviewMutation();
  const [hide, hideState] = useHideReviewMutation();
  const [restore, restoreState] = useRestoreReviewMutation();
  const [remove, removeState] = useDeleteReviewMutation();
  const [createReview, createState] = useCreateReviewMutation();

  const pending: Review[] = pendingQ.data?.data?.content ?? [];
  const mine: Review[] = mineQ.data?.data?.content ?? [];

  const mutationError =
    (decideState.error as { message?: string } | undefined)?.message ??
    (hideState.error as { message?: string } | undefined)?.message ??
    (restoreState.error as { message?: string } | undefined)?.message ??
    (removeState.error as { message?: string } | undefined)?.message ??
    (createState.error as { message?: string } | undefined)?.message;

  const runAction = async (fn: () => Promise<unknown>, ok: string) => {
    try {
      await fn();
      message.success(ok);
      setSelected(null);
    } catch (e) {
      const err = e as { data?: { message?: string } };
      message.error(err?.data?.message ?? 'Thao tác thất bại');
    }
  };

  const pendingColumns: ColumnsType<Review> = [
    {
      title: 'Sản phẩm',
      dataIndex: 'productId',
      key: 'productId',
      render: (v: string) => <Typography.Text copyable={{ text: v }}>{v?.slice(0, 8)}…</Typography.Text>,
    },
    {
      title: 'Người review',
      dataIndex: 'reviewerDisplayName',
      key: 'reviewerDisplayName',
      render: (v: string, r: Review) => (r.anonymous ? 'Ẩn danh' : v ?? '—'),
    },
    { title: 'Sao', dataIndex: 'rating', key: 'rating', width: 90, render: (v: number) => <Rate disabled value={v} style={{ fontSize: 14 }} /> },
    {
      title: 'Nội dung',
      dataIndex: 'content',
      key: 'content',
      ellipsis: true,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'revisionStatus',
      key: 'revisionStatus',
      width: 130,
      render: (v: string) => <StatusTag status={v ?? 'PENDING'} />,
    },
    {
      title: 'Hành động',
      key: 'actions',
      fixed: 'right',
      width: 240,
      render: (_: unknown, r: Review) => (
        <Space>
          <Button
            size="small"
            type="primary"
            loading={decideState.isLoading}
            onClick={() => runAction(() => decide({ revisionId: r.revisionId!, body: { isApproved: true } }).unwrap(), 'Đã duyệt review')}
          >
            Duyệt
          </Button>
          <Button size="small" danger onClick={() => { setRejectOpen(r); rejectForm.resetFields(); }}>
            Từ chối
          </Button>
          <Button size="small" type="text" onClick={() => setSelected(r)}>
            Chi tiết
          </Button>
        </Space>
      ),
    },
  ];

  const mineColumns: ColumnsType<Review> = [
    {
      title: 'Sản phẩm',
      dataIndex: 'productId',
      key: 'productId',
      render: (v: string) => <Typography.Text copyable={{ text: v }}>{v?.slice(0, 8)}…</Typography.Text>,
    },
    { title: 'Sao', dataIndex: 'rating', key: 'rating', width: 90, render: (v: number) => <Rate disabled value={v} style={{ fontSize: 14 }} /> },
    { title: 'Nội dung', dataIndex: 'content', key: 'content', ellipsis: true },
    { title: 'Hữu ích', dataIndex: 'helpfulCount', key: 'helpfulCount', width: 100, align: 'right' },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 130,
      render: (v: string) => <StatusTag status={v ?? 'PENDING'} />,
    },
    {
      title: 'Hành động',
      key: 'actions',
      fixed: 'right',
      width: 140,
      render: (_: unknown, r: Review) => (
        <Space>
          <Button size="small" type="text" onClick={() => setSelected(r)}>
            Chi tiết
          </Button>
          <Button
            size="small"
            danger
            type="text"
            loading={removeState.isLoading}
            onClick={() => runAction(() => remove(r.id).unwrap(), 'Đã xóa review')}
          >
            Xóa
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Đánh giá"
        subTitle="Duyệt review sản phẩm và quản lý đánh giá của bạn"
        extra={
          <Button type="primary" onClick={() => { setCreateOpen(true); setIdemKey(crypto.randomUUID()); }}>
            Viết đánh giá
          </Button>
        }
      />
      <SummaryRow
        items={[
          { label: 'Chờ duyệt', value: canModerate ? (pendingQ.data?.data?.totalElements ?? 0) : '—' },
          { label: 'Review của tôi', value: mineQ.data?.data?.totalElements ?? 0 },
        ]}
      />
      {mutationError && <ApiErrorAlert message={mutationError} />}
      <Tabs
        defaultActiveKey={canModerate ? 'pending' : 'mine'}
        items={[
          ...(canModerate
            ? [
                {
                  key: 'pending',
                  label: 'Hàng chờ duyệt',
                  children: pendingQ.isLoading ? (
                    <ListSkeleton />
                  ) : (
                    <DataTable<Review>
                      columns={pendingColumns}
                      data={pending}
                      rowKey="id"
                      loading={pendingQ.isFetching}
                    />
                  ),
                },
              ]
            : []),
          {
            key: 'mine',
            label: 'Review của tôi',
            children: mineQ.isLoading ? (
              <ListSkeleton />
            ) : (
              <DataTable<Review>
                columns={mineColumns}
                data={mine}
                rowKey="id"
                loading={mineQ.isFetching}
              />
            ),
          },
        ]}
      />

      {/* Drawer 560 — chi tiet review (FE-STYLE §8). */}
      <Drawer title="Chi tiết đánh giá" width={560} open={!!selected} onClose={() => setSelected(null)}>
        {selected && (
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            <Descriptions column={1} size="small">
              <Descriptions.Item label="Sản phẩm">
                <Typography.Text copyable>{selected.productId}</Typography.Text>
              </Descriptions.Item>
              <Descriptions.Item label="Người review">
                {selected.anonymous ? 'Ẩn danh' : selected.reviewerDisplayName ?? '—'}
              </Descriptions.Item>
              <Descriptions.Item label="Số sao"><Rate disabled value={selected.rating ?? 0} /></Descriptions.Item>
              <Descriptions.Item label="Mua hàng xác thực">
                {selected.verifiedPurchase ? 'Có' : 'Không'}
              </Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                <StatusTag status={selected.status ?? 'PENDING'} />
              </Descriptions.Item>
              <Descriptions.Item label="Số lượt thấy hữu ích">{selected.helpfulCount ?? 0}</Descriptions.Item>
            </Descriptions>
            <Card size="small" title="Nội dung">
              <Typography.Paragraph>{selected.content ?? '—'}</Typography.Paragraph>
            </Card>
            {canModerate && selected.id && (
              <Space>
                <Button
                  danger
                  loading={hideState.isLoading}
                  onClick={() => runAction(() => hide({ id: selected.id, body: { reasonCode: 'MODERATOR_HIDDEN' } }).unwrap(), 'Đã ẩn review')}
                >
                  Ẩn review
                </Button>
                <Button
                  loading={restoreState.isLoading}
                  onClick={() => runAction(() => restore({ id: selected.id, body: {} }).unwrap(), 'Đã mở lại review')}
                >
                  Mở lại
                </Button>
              </Space>
            )}
          </Space>
        )}
      </Drawer>

      {/* Modal tu choi revision — bat buoc ly do (BE ModerationRequest.reasonCode). */}
      <Modal
        title="Từ chối review"
        open={!!rejectOpen}
        onCancel={() => setRejectOpen(null)}
        onOk={() =>
          rejectForm.validateFields().then((v) =>
            runAction(
              () =>
                decide({
                  revisionId: rejectOpen!.revisionId!,
                  body: { isApproved: false, reasonCode: v.reasonCode, reasonNote: v.reasonNote },
                }).unwrap(),
              'Đã từ chối review',
            ).then(() => setRejectOpen(null)),
          )
        }
        confirmLoading={decideState.isLoading}
        okText="Từ chối"
        okButtonProps={{ danger: true }}
        cancelText="Đóng"
      >
        <Form form={rejectForm} layout="vertical">
          <Form.Item
            name="reasonCode"
            label="Lý do"
            rules={[{ required: true, message: 'Chọn lý do từ chối' }]}
          >
            <Input placeholder="VD: SPAM, OFFENSIVE, IRRELEVANT" />
          </Form.Item>
          <Form.Item name="reasonNote" label="Ghi chú thêm">
            <Input.TextArea rows={3} placeholder="Ghi chú gửi cho người review (tùy chọn)" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Modal tao review — Idempotency-Key giu nguyen khi retry (BE 201). */}
      <Modal
        title="Viết đánh giá"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={() =>
          createForm.validateFields().then((v) =>
            runAction(
              () => createReview({ body: v as Parameters<typeof createReview>[0]['body'], idempotencyKey: idemKey }).unwrap(),
              'Đã gửi đánh giá — chờ duyệt',
            ).then(() => {
              setCreateOpen(false);
              createForm.resetFields();
              setIdemKey(crypto.randomUUID());
            }),
          )
        }
        confirmLoading={createState.isLoading}
        okText="Gửi"
        cancelText="Hủy"
      >
        <Form form={createForm} layout="vertical">
          <Form.Item
            name="productId"
            label="Mã sản phẩm"
            rules={[{ required: true, message: 'Nhập productId' }]}
          >
            <Input placeholder="UUID sản phẩm" />
          </Form.Item>
          <Form.Item name="rating" label="Số sao" rules={[{ required: true, message: 'Chọn số sao' }]}>
            <Rate />
          </Form.Item>
          <Form.Item name="title" label="Tiêu đề">
            <Input placeholder="Tiêu đề (tùy chọn)" />
          </Form.Item>
          <Form.Item name="content" label="Nội dung">
            <Input.TextArea rows={4} placeholder="Chia sẻ trải nghiệm của bạn" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

export default ReviewsPage;
