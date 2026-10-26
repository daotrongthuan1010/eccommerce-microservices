import { useEffect, useState } from 'react';
import { App, Button, InputNumber, Space } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { Link } from 'react-router-dom';
import {
  addToCart,
  clearCart,
  getCart,
  removeFromCart,
  type Cart,
  type CartItem,
} from '../mocks/orderMock';
import { mockCatalog } from '../mocks/searchMock';
import { ApiErrorAlert, DataTable, PageHeader } from '../components/ui';

// Wave 4.2 — gio hang hoan toan offline tren orderMock. KHONG goi /api/order/**.
export default function CartPage() {
  const { message } = App.useApp();
  const [cart, setCart] = useState<Cart | null>(null);
  const [error, setError] = useState<string | null>(null);

  const refresh = async () => {
    try {
      setCart(await getCart());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không tải được giỏ hàng');
    }
  };

  useEffect(() => {
    void refresh();
  }, []);

  const add = async (skuId: string) => {
    const p = mockCatalog.find((c) => c.id === skuId);
    if (!p) return;
    const item: CartItem = {
      skuId: p.id,
      sku: p.sku,
      productName: p.name,
      price: p.price,
      quantity: 1,
    };
    setCart(await addToCart(item));
    message.success(`Đã thêm ${p.name}`);
  };

  const columns: ColumnsType<CartItem> = [
    { title: 'Sản phẩm', dataIndex: 'productName' },
    { title: 'SKU', dataIndex: 'sku' },
    {
      title: 'Giá',
      dataIndex: 'price',
      render: (v: number) => v.toLocaleString('vi-VN'),
    },
    {
      title: 'Số lượng',
      dataIndex: 'quantity',
      render: (q: number, r) => (
        <InputNumber
          min={1}
          value={q}
          onChange={async (v) => {
            if (!v) return;
            await removeFromCart(r.skuId);
            setCart(await addToCart({ ...r, quantity: v }));
          }}
        />
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, r) => (
        <Button
          size="small"
          danger
          onClick={async () => setCart(await removeFromCart(r.skuId))}
        >
          Xóa
        </Button>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Giỏ hàng (mock)"
        subTitle="Hoàn toàn offline — không gọi API thật"
        extra={
          <Space>
            <Button onClick={() => void clearCart().then(setCart)}>Xóa hết</Button>
            <Link to="/checkout">
              <Button type="primary" disabled={!cart || cart.items.length === 0}>
                Checkout
              </Button>
            </Link>
          </Space>
        }
      />
      <ApiErrorAlert message={error} />
      <h4>Thêm nhanh từ mock catalog</h4>
      <Space wrap style={{ marginBottom: 16 }}>
        {mockCatalog.map((p) => (
          <Button key={p.id} onClick={() => void add(p.id)}>
            + {p.name}
          </Button>
        ))}
      </Space>
      <DataTable<CartItem> columns={columns} data={cart?.items ?? []} rowKey="skuId" />
      <p>
        Tổng: <strong>{(cart?.totalAmount ?? 0).toLocaleString('vi-VN')} ₫</strong> (
        {cart?.totalQuantity ?? 0} món)
      </p>
    </div>
  );
}
