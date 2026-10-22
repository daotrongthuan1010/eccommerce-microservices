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
      setError(e instanceof Error ? e.message : 'Không tải được giỏ hàng');
    });
  }, []);

  const onFinish = async (values: {
    shippingAddress: string;
    paymentMethod: 'COD' | 'BANK_TRANSFER' | 'E_WALLET';
  }) => {
    if (!cart || cart.items.length === 0) {
      message.warning('Giỏ hàng trống');
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
      message.success(`Đặt hàng thành công: ${order.code}`);
      navigate('/orders');
    } catch (e) {
setError(e instanceof Error ? e.message : 'Checkout thất bại');
    } finally {
      setLoading(false);
    }
  };

  if (!cart) return <ListSkeleton />;

  return (
    <div>
      <PageHeader title="Thanh toán (mock)" subTitle="Checkout offline — không gọi API thật" />
      <ApiErrorAlert message={error} />
      <p>
        {cart.totalQuantity} món — Tổng:{' '}
        <strong>{cart.totalAmount.toLocaleString('vi-VN')} ₫</strong>
      </p>
      <Form layout="vertical" onFinish={(v) => void onFinish(v)} style={{ maxWidth: 520 }}>
        <FormSection title="Giao hàng">
          <Form.Item
            name="shippingAddress"
            label="Địa chỉ nhận hàng"
            rules={[{ required: true, message: 'Nhập địa chỉ' }]}
          >
            <Input placeholder="Số nhà, đường, quận, thành phố" />
          </Form.Item>
        </FormSection>
        <FormSection title="Thanh toán">
          <Form.Item
            name="paymentMethod"
            label="Phương thức"
            initialValue="COD"
            rules={[{ required: true }]}
          >
            <Select
              options={[
                { value: 'COD', label: 'COD — Tiền mặt' },
                { value: 'BANK_TRANSFER', label: 'Chuyển khoản' },
                { value: 'E_WALLET', label: 'Ví điện tử' },
              ]}
            />
          </Form.Item>
        </FormSection>
        <ActionBar>
          <Button type="primary" htmlType="submit" loading={loading} disabled={cart.items.length === 0}>
            Đặt hàng
          </Button>
        </ActionBar>
      </Form>
    </div>
  );
}
