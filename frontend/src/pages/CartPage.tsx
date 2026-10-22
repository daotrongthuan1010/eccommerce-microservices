import { useEffect, useState } from 'react';
import { Button, InputNumber, Space, message } from 'antd';
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
  const [cart, setCart] = useState<Cart | null>(null);
  const [error, setError] = useState<string | null>(null);

  const refresh = async () => {
    try {
      setCart(await getCart());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Khong tai duoc gio hang');
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
    message.success(`Da them ${p.name}`);
  };

  const columns: ColumnsType<CartItem> = [
    { title: 'San pham', dataIndex: 'productName' },
    { title: 'SKU', dataIndex: 'sku' },
    {
      title: 'Gia',
      dataIndex: 'price',
      render: (v: number) => v.toLocaleString('vi-VN'),
    },
    {
      title: 'So luong',
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
      title: 'Thao tac',
      key: 'actions',
      render: (_, r) => (
        <Button
          size="small"
          danger
          onClick={async () => setCart(await removeFromCart(r.skuId))}
        >
          Xoa
        </Button>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Gio hang (mock)"
        subTitle="Hoan toan offline — khong goi API that"
        extra={
          <Space>
            <Button onClick={() => void clearCart().then(setCart)}>Xoa het</Button>
            <Link to="/checkout">
              <Button type="primary" disabled={!cart || cart.items.length === 0}>
                Checkout
              </Button>
            </Link>
          </Space>
        }
      />
      <ApiErrorAlert message={error} />
      <h4>Them nhanh tu mock catalog</h4>
      <Space wrap style={{ marginBottom: 16 }}>
        {mockCatalog.map((p) => (
          <Button key={p.id} onClick={() => void add(p.id)}>
            + {p.name}
          </Button>
        ))}
      </Space>
      <DataTable<CartItem> columns={columns} data={cart?.items ?? []} rowKey="skuId" />
      <p>
        Tong: <strong>{(cart?.totalAmount ?? 0).toLocaleString('vi-VN')} ₫</strong> (
        {cart?.totalQuantity ?? 0} mon)
      </p>
    </div>
  );
}
