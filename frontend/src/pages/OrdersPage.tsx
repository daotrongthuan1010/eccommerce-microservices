import { Button, Empty, Space, Table, Tag, Typography } from 'antd';
import { Link } from 'react-router-dom';
import { useMyOrdersQuery, type Order } from '../features/order/orderApi';
import { ApiErrorAlert, ListSkeleton } from '../components/ui';
import { formatVnd } from '../components/marketplace/ProductCardGrid';

// Marketplace OrdersPage (2026-11-05 fe-marketplace D6, spec customer/09):
// danh sach don hang cua toi tu GET /api/order/me/orders (that, da login).
const STATUS_COLORS: Record<string, string> = {
  PENDING_PAYMENT: 'orange',
  PAID: 'green',
  PROCESSING: 'blue',
  SHIPPED: 'geekblue',
  DELIVERED: 'green',
  CANCELLED: 'default',
  COMPLETED: 'green',
};

export default function OrdersPage() {
  const ordersQ = useMyOrdersQuery({ size: 20 });
  const orders = ordersQ.data?.data?.content ?? [];
  const total = ordersQ.data?.data?.totalElements ?? 0;

  if (ordersQ.isLoading) return <ListSkeleton />;

  return (
    <div style={{ maxWidth: 960, margin: '0 auto' }}>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          Đơn hàng của tôi
        </Typography.Title>
        <ApiErrorAlert message={(ordersQ.error as { message?: string } | undefined)?.message ?? null} />
        {orders.length === 0 && !ordersQ.error ? (
          <Empty description="Bạn chưa có đơn hàng nào">
            <Link to="/products">
              <Button type="primary">Mua sắm ngay</Button>
            </Link>
          </Empty>
        ) : (
          <Table<Order>
            size="middle"
            rowKey="id"
            dataSource={orders}
            pagination={total > 20 ? { pageSize: 20, total } : false}
            columns={[
              {
                title: 'Mã đơn',
                dataIndex: 'orderCode',
                render: (v: string, r) => <Link to={`/orders/${r.id}`}>{v}</Link>,
              },
              {
                title: 'Trạng thái',
                dataIndex: 'status',
                render: (s: string) => <Tag color={STATUS_COLORS[s] ?? 'default'}>{s}</Tag>,
              },
              {
                title: 'Tổng tiền',
                dataIndex: 'totalAmount',
                render: (v: number) => (
                  <span className="money" style={{ fontWeight: 600 }}>
                    {formatVnd(v)}
                  </span>
                ),
              },
              {
                title: 'Ngày đặt',
                dataIndex: 'createdAt',
                render: (v: string) =>
                  v ? new Date(v).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' }) : '—',
              },
              {
                title: '',
                key: 'actions',
                render: (_: unknown, r) => (
                  <Link to={`/orders/${r.id}`}>
                    <Button size="small">Chi tiết</Button>
                  </Link>
                ),
              },
            ]}
          />
        )}
      </Space>
    </div>
  );
}
