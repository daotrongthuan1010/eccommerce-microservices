import { App, Button, Empty, InputNumber, Space, Table, Typography } from 'antd';
import { DeleteOutlined, ShoppingCartOutlined } from '@ant-design/icons';
import { Link, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import {
  clearCart,
  guestCartAmount,
  guestCartCount,
  removeItem,
  updateQuantity,
  type GuestCartItem,
} from '../features/marketplace/guestCartSlice';
import {
  useGetCartQuery,
  useRemoveFromCartMutation,
  useUpdateCartItemMutation,
  type CartItem,
} from '../features/order/orderApi';
import { ApiErrorAlert } from '../components/ui';
import { formatVnd } from '../components/marketplace/ProductCardGrid';

// Marketplace CartPage (2026-11-05 fe-marketplace D5, spec customer/06):
// - Chua login: guest cart localStorage (gia TAM TINH).
// - Da login: cart server (GET /api/order/cart) — nguon su that; guest cart
//   da merge o MarketplaceLayout.
// Tong hien tai la tam tinh; BE tinh lai khi checkout.

type Row = {
  key: string;
  skuId: string;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineAmount: number;
  server: boolean;
};

export default function CartPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { message } = App.useApp();
  const username = useAppSelector((s) => s.auth.username);
  const guestItems = useAppSelector((s) => s.guestCart.items);

  const cartQ = useGetCartQuery(undefined, { skip: !username });
  const [updateServerItem] = useUpdateCartItemMutation();
  const [removeServerItem] = useRemoveFromCartMutation();

  const serverItems: CartItem[] = cartQ.data?.data?.items ?? [];

  const rows: Row[] = username
    ? serverItems.map((i) => ({
        key: i.skuId,
        skuId: i.skuId,
        productName: i.productName,
        unitPrice: i.unitPrice,
        quantity: i.quantity,
        lineAmount: i.lineAmount,
        server: true,
      }))
    : guestItems.map((i: GuestCartItem) => ({
        key: i.skuId,
        skuId: i.skuId,
        productName: i.productName,
        unitPrice: i.unitPrice,
        quantity: i.quantity,
        lineAmount: i.unitPrice * i.quantity,
        server: false,
      }));

  const totalAmount = username
    ? cartQ.data?.data?.totalAmount ?? 0
    : guestCartAmount(guestItems);
  const totalCount = username ? cartQ.data?.data?.totalQuantity ?? 0 : guestCartCount(guestItems);

  const onQty = async (row: Row, qty: number | null) => {
    if (!qty || qty < 1) return;
    if (row.server) {
      try {
        await updateServerItem({ skuId: row.skuId, quantity: qty }).unwrap();
      } catch {
        message.error('Cập nhật giỏ hàng thất bại');
      }
    } else {
      dispatch(updateQuantity({ skuId: row.skuId, quantity: qty }));
    }
  };

  const onRemove = async (row: Row) => {
    if (row.server) {
      try {
        await removeServerItem(row.skuId).unwrap();
        message.success('Đã xóa khỏi giỏ hàng');
      } catch {
        message.error('Xóa khỏi giỏ hàng thất bại');
      }
    } else {
      dispatch(removeItem(row.skuId));
    }
  };

  const err = (cartQ.error as { message?: string } | undefined)?.message;

  return (
    <div style={{ maxWidth: 960, margin: '0 auto' }}>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          Giỏ hàng
        </Typography.Title>
        {!username && (
          <Typography.Text type="secondary">
            Bạn đang xem giỏ hàng khách (lưu trên máy).{' '}
            <Link to="/login">Đăng nhập</Link> để đồng bộ giỏ hàng vào tài khoản.
          </Typography.Text>
        )}
        <ApiErrorAlert message={username ? err ?? null : null} />

        {rows.length === 0 ? (
          <Empty description="Giỏ hàng trống" style={{ padding: 48 }}>
            <Link to="/products">
              <Button type="primary" icon={<ShoppingCartOutlined />}>
                Tiếp tục mua sắm
              </Button>
            </Link>
          </Empty>
        ) : (
          <>
            <Table<Row>
              size="middle"
              rowKey="key"
              pagination={false}
              dataSource={rows}
              columns={[
                {
                  title: 'Sản phẩm',
                  dataIndex: 'productName',
                  render: (v: string, r) => <Link to={`/products/${r.skuId}`}>{v}</Link>,
                },
                {
                  title: 'Đơn giá',
                  dataIndex: 'unitPrice',
                  width: 130,
                  render: (v: number) => <span className="money">{formatVnd(v)}</span>,
                },
                {
                  title: 'Số lượng',
                  dataIndex: 'quantity',
                  width: 140,
                  render: (q: number, r) => (
                    <InputNumber min={1} max={999} value={q} onChange={(v) => void onQty(r, v)} />
                  ),
                },
                {
                  title: 'Thành tiền',
                  dataIndex: 'lineAmount',
                  width: 150,
                  render: (v: number) => (
                    <span className="money" style={{ color: '#e8543f', fontWeight: 600 }}>
                      {formatVnd(v)}
                    </span>
                  ),
                },
                {
                  title: '',
                  key: 'actions',
                  width: 60,
                  render: (_: unknown, r) => (
                    <Button
                      size="small"
                      type="text"
                      danger
                      icon={<DeleteOutlined />}
                      onClick={() => void onRemove(r)}
                    />
                  ),
                },
              ]}
            />
            <Space style={{ justifyContent: 'space-between', width: '100%' }} wrap>
              <Button
                onClick={() => {
                  if (username) message.info('Xóa từng sản phẩm trên cart server');
                  else dispatch(clearCart());
                }}
                disabled={!username && rows.length === 0}
              >
                {username ? '' : 'Xóa hết'}
              </Button>
              <Space size={24} wrap>
                <Typography.Text>
                  Tạm tính ({totalCount} món):{' '}
                  <strong className="money" style={{ color: '#e8543f', fontSize: 18 }}>
                    {formatVnd(totalAmount)}
                  </strong>
                </Typography.Text>
                <Button
                  type="primary"
                  size="large"
                  onClick={() => navigate(username ? '/checkout' : '/login')}
                >
                  Thanh toán
                </Button>
              </Space>
            </Space>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              * Giá tạm tính có thể thay đổi; tổng tiền cuối cùng được hệ thống tính lại khi đặt hàng.
            </Typography.Text>
          </>
        )}
      </Space>
    </div>
  );
}
