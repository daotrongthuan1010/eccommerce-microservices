import { useEffect, useState } from 'react';
import { Button, Form, Input, Select, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { checkout, getCart, type Cart } from '../mocks/orderMock';
import { createPayment } from '../mocks/paymentMock';
import { ActionBar, ApiErrorAlert, FormSection, ListSkeleton, PageHeader } from '../components/ui';

// Wave 4.2 — checkout hoan toan offline: orderMock.checkout + paymentMock.
// KHONG goi /api/order/** hay /api/payment/**.
export default function CheckoutPage() {
  const [cart, setCart] = useState<Cart | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    getCart().then(setCart).catch((e: unknown) => {
      setError(e instanceof Error ? e.message : 'Khong tai duoc gio hang');
    });
  }, []);

  const onFinish = async (values: {
    shippingAddress: string;
    paymentMethod: 'COD' | 'BANK_TRANSFER' | 'E_WALLET';
  }) => {
    if (!cart || cart.items.length === 0) {
      message.warning('Gio hang trong');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const order = await checkout({
        items: cart.items,
        shippingAddress: values.shippingAddress,
        paymentMethod: values.paymentMethod,
      });
      await createPayment({
        orderId: order.id,
        amount: order.totalAmount,
        method: values.paymentMethod,
      });
      message.success(`Dat hang thanh cong: ${order.code}`);
      navigate('/orders');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Checkout that bai');
    } finally {
      setLoading(false);
    }
  };

  if (!cart) return <ListSkeleton />;

  return (
    <div>
      <PageHeader title="Thanh toan (mock)" subTitle="Checkout offline — khong goi API that" />
      <ApiErrorAlert message={error} />
      <p>
        {cart.totalQuantity} mon — Tong:{' '}
        <strong>{cart.totalAmount.toLocaleString('vi-VN')} ₫</strong>
      </p>
      <Form layout="vertical" onFinish={(v) => void onFinish(v)} style={{ maxWidth: 520 }}>
        <FormSection title="Giao hang">
          <Form.Item
            name="shippingAddress"
            label="Dia chi nhan hang"
            rules={[{ required: true, message: 'Nhap dia chi' }]}
          >
            <Input placeholder="So nha, duong, quan, thanh pho" />
          </Form.Item>
        </FormSection>
        <FormSection title="Thanh toan">
          <Form.Item
            name="paymentMethod"
            label="Phuong thuc"
            initialValue="COD"
            rules={[{ required: true }]}
          >
            <Select
              options={[
                { value: 'COD', label: 'COD — Tien mat' },
                { value: 'BANK_TRANSFER', label: 'Chuyen khoan' },
                { value: 'E_WALLET', label: 'Vi dien tu' },
              ]}
            />
          </Form.Item>
        </FormSection>
        <ActionBar>
          <Button type="primary" htmlType="submit" loading={loading} disabled={cart.items.length === 0}>
            Dat hang
          </Button>
        </ActionBar>
      </Form>
    </div>
  );
}
