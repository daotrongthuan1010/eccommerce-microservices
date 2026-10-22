import { useEffect, useState } from 'react';
import type { ColumnsType } from 'antd/es/table';
import { listOrders, type Order } from '../mocks/orderMock';
import { ApiErrorAlert, DataTable, ListSkeleton, PageHeader, StatusTag } from '../components/ui';

// Wave 4.2 — danh sach don hang tu orderMock. KHONG goi /api/order/**.
export default function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);

  useEffect(() => {
    listOrders(page, 20)
      .then((p) => {
        setOrders(p.content);
        setTotal(p.totalElements);
      })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : 'Không tải được đơn hàng'))
      .finally(() => setLoading(false));
  }, [page]);

  const columns: ColumnsType<Order> = [
    { title: 'Mã đơn', dataIndex: 'code' },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      render: (s: string) => <StatusTag status={s} />,
    },
    {
      title: 'Tổng tiền',
      dataIndex: 'totalAmount',
      render: (v: number) => v.toLocaleString('vi-VN'),
    },
    { title: 'Ngày tạo', dataIndex: 'createdAt' },
  ];

  return (
    <div>
      <PageHeader title="Đơn hàng (mock)" subTitle="Dữ liệu local — không gọi API thật" />
      <ApiErrorAlert message={error} />
      {loading ? (
        <ListSkeleton />
      ) : (
        <DataTable<Order>
          columns={columns}
          data={orders}
          total={total}
          page={page}
          onPageChange={setPage}
          rowKey="id"
        />
      )}
    </div>
  );
}
