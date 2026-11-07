import { useEffect, useState } from 'react';
import { App, Button, Card, Dropdown, Input, Select, Space, Tag, type MenuProps } from 'antd';
import { DownOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import {
  useAdminCancelOrderMutation,
  useListAdminOrdersQuery,
  useUpdateOrderStatusMutation,
  type AdminOrder,
  type OrderStatusTransition,
} from '../features/order/adminOrderApi';
import { ApiErrorAlert, DataTable, FilterBar, ListSkeleton, PageHeader, TableEmpty } from '../components/ui';
import { formatVnd } from '../components/marketplace/ProductCardGrid';

// /admin/orders — RequireRole ADMIN. PageHeader → FilterBar → Table (server-side paging).
// Trang thai khop OrderStatus enum (§ADR-0001): PAID -> CONFIRMED -> SHIPPED -> DELIVERED.
const STATUS_COLORS: Record<string, string> = {
  PENDING_PAYMENT: 'orange',
  PAID: 'green',
  CONFIRMED: 'blue',
  SHIPPED: 'geekblue',
  DELIVERED: 'green',
  PAYMENT_FAILED: 'red',
  CANCELLED: 'default',
  EXPIRED: 'default',
};

const ALL_STATUSES = [
  'PENDING_PAYMENT',
  'PAID',
  'CONFIRMED',
  'SHIPPED',
  'DELIVERED',
  'PAYMENT_FAILED',
  'CANCELLED',
  'EXPIRED',
];

// Chuyen trang thai xuoi (admin) — dung state machine BE, don PAID phai confirm truoc khi ship.
const NEXT_STATUS: Record<string, OrderStatusTransition[]> = {
  PENDING_PAYMENT: [],
  PAID: ['CONFIRMED'],
  CONFIRMED: ['SHIPPED'],
  SHIPPED: ['DELIVERED'],
};

function useDebounced(value: string, delay = 300): string {
  const [v, setV] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setV(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return v;
}

export default function AdminOrdersPage() {
  const { message, modal } = App.useApp();
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<string | undefined>(undefined);
  const [page, setPage] = useState(0);

  const debounced = useDebounced(search);
  const appliedSearch = debounced.trim().length >= 2 ? debounced.trim() : undefined;

  // Reset ve trang 0 khi filter doi.
  useEffect(() => {
    setPage(0);
  }, [appliedSearch, status]);

  const query = useListAdminOrdersQuery({ page, size: 20, status, search: appliedSearch });
  const [updateStatus, updateState] = useUpdateOrderStatusMutation();
  const [cancelOrder, cancelState] = useAdminCancelOrderMutation();

  const orders: AdminOrder[] = query.data?.data?.content ?? [];
  const total = query.data?.data?.totalElements ?? 0;

  const runMutation = async (fn: () => Promise<unknown>, ok: string, fail: string) => {
    try {
      await fn();
      message.success(ok);
    } catch {
      message.error(fail);
    }
  };

  const onUpdateStatus = (orderId: string, next: OrderStatusTransition) => {
    void runMutation(
      () => updateStatus({ orderId, status: next }).unwrap(),
      `Đã chuyển đơn sang ${next}`,
      'Cập nhật trạng thái thất bại',
    );
  };

  const onCancel = (orderId: string, orderCode: string) => {
    let reason = '';
    modal.confirm({
      title: `Hủy đơn ${orderCode}?`,
      content: (
        <Input.TextArea
          rows={3}
          placeholder="Lý do hủy (không bắt buộc)"
          onChange={(e) => {
            reason = e.target.value;
          }}
        />
      ),
      okText: 'Hủy đơn',
      okButtonProps: { danger: true },
      cancelText: 'Giữ đơn',
      onOk: () =>
        runMutation(
          () => cancelOrder({ orderId, reason: reason.trim() || undefined }).unwrap(),
          'Đã hủy đơn hàng',
          'Hủy đơn hàng thất bại',
        ),
    });
  };

  const columns: ColumnsType<AdminOrder> = [
    {
      title: 'Mã đơn',
      dataIndex: 'orderCode',
      key: 'orderCode',
      render: (v: string) => <span style={{ fontWeight: 500 }}>{v}</span>,
    },
    { title: 'User', dataIndex: 'userId', key: 'userId', render: (v: string) => v ?? '—' },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 150,
      render: (s: string) => <Tag color={STATUS_COLORS[s] ?? 'default'}>{s}</Tag>,
    },
    {
      title: 'Tổng tiền',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      align: 'right' as const,
      render: (v: number) => (
        <span className="money" style={{ fontWeight: 600 }}>
          {formatVnd(v)}
        </span>
      ),
    },
    { title: 'Thanh toán', dataIndex: 'paymentMethod', key: 'paymentMethod' },
    {
      title: 'Ngày đặt',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (v: string) =>
        v ? new Date(v).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' }) : '—',
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 220,
      fixed: 'right' as const,
      render: (_: unknown, r: AdminOrder) => {
        const nexts = NEXT_STATUS[r.status] ?? [];
        // Don PAID+ phai di hoan tien (BE tu choi cancel) — chi hien nut voi don non-terminal
      const cancellable = ['PENDING_PAYMENT', 'PAYMENT_FAILED', 'EXPIRED'].includes(r.status);
        const menuItems: MenuProps['items'] = nexts.map((n) => ({
          key: n,
          label: `Chuyển sang ${n}`,
          onClick: () => onUpdateStatus(r.orderId, n),
        }));
        return (
          <Space>
            {menuItems.length > 0 && (
              <Dropdown menu={{ items: menuItems }} trigger={['click']}>
                <Button size="small" loading={updateState.isLoading}>
                  Cập nhật trạng thái <DownOutlined />
                </Button>
              </Dropdown>
            )}
            {cancellable && (
              <Button
                size="small"
                danger
                loading={cancelState.isLoading}
                onClick={() => onCancel(r.orderId, r.orderCode)}
              >
                Hủy đơn
              </Button>
            )}
          </Space>
        );
      },
    },
  ];

  const queryError = (query.error as { message?: string } | undefined)?.message;
  const mutationError =
    (updateState.error as { message?: string } | undefined)?.message ??
    (cancelState.error as { message?: string } | undefined)?.message;

  return (
    <div>
      <PageHeader title="Quản lý đơn hàng" subTitle="Tất cả đơn hàng trong hệ thống (ADMIN)" />
      <ApiErrorAlert message={mutationError ?? queryError ?? null} />
      <FilterBar>
        <Input
          placeholder="Tìm theo mã đơn / userId (≥ 2 ký tự)"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          allowClear
          style={{ width: 300 }}
        />
        <Select
          placeholder="Trạng thái"
          allowClear
          value={status}
          onChange={setStatus}
          options={[{ value: '', label: 'Tất cả' }, ...ALL_STATUSES.map((s) => ({ value: s, label: s }))]}
          style={{ width: 180 }}
        />
        <Button icon={<ReloadOutlined />} onClick={() => query.refetch()}>
          Tải lại
        </Button>
        {(search || status) && (
          <Button
            onClick={() => {
              setSearch('');
              setStatus(undefined);
            }}
          >
            Xóa lọc
          </Button>
        )}
      </FilterBar>

      {query.isLoading ? (
        <ListSkeleton />
      ) : query.isError ? (
        <Card>
          <TableEmpty
            description="Không tải được danh sách đơn hàng"
            actionText="Thử lại"
            onAction={() => query.refetch()}
          />
        </Card>
      ) : orders.length === 0 ? (
        <Card>
          <TableEmpty
            description={appliedSearch ? `Không tìm thấy "${appliedSearch}"` : 'Chưa có đơn hàng'}
            actionText="Xóa lọc"
            onAction={() => {
              setSearch('');
              setStatus(undefined);
            }}
          />
        </Card>
      ) : (
        <DataTable<AdminOrder>
          columns={columns}
          data={orders}
          loading={query.isFetching}
          total={total}
          page={page}
          onPageChange={setPage}
          rowKey="orderId"
        />
      )}
    </div>
  );
}
