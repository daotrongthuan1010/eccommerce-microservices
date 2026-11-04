import { App, Alert, Button, Card, Form, Input, Radio, Space, Typography } from 'antd';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useGetCartQuery, useCheckoutMutation } from '../features/order/orderApi';
import { ApiErrorAlert, ListSkeleton } from '../components/ui';
import { formatVnd } from '../components/marketplace/ProductCardGrid';

// Marketplace CheckoutPage (2026-11-05 fe-marketplace D6, spec customer/07):
// dat hang TU GIO SERVER (da login, guest cart da merge). Idempotency-Key
// UUID sinh 1 lan khi vao trang — bam "Dat hang" nhieu lan trong phien chi
// tao 1 don (BE dedup); bam "Thu lai" sau loi GIU NGUYEN key.
// VNPAY/ZALOPAY: BE tra checkoutUrl (fake-gateway) — redirect sang do;
// ngu ve /payment/result?orderId=... poll trang thai. COD: thang sang ket qua.
type FormValues = {
  shippingAddress: string;
  recipientName: string;
  recipientPhone: string;
  paymentMethod: 'VNPAY' | 'ZALOPAY' | 'COD';
};

export default function CheckoutPage() {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const cartQ = useGetCartQuery();
  const [checkoutMut, checkoutState] = useCheckoutMutation();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const idempotencyKey = useMemo(() => crypto.randomUUID(), []);
  const cart = cartQ.data?.data;
  const items = cart?.items ?? [];
  const total = cart?.totalAmount ?? 0;

  const onFinish = async (values: FormValues) => {
    if (items.length === 0) {
      message.warning('Giỏ hàng trống');
      return;
    }
    setSubmitError(null);
    try {
      const res = await checkoutMut({ body: values, idempotencyKey }).unwrap();
      const data = res.data;
      if (data.checkoutUrl) {
        // Sang trang thanh toan cua gateway (fake VNPAY/ZaloPay). Ket qua
        // that do IPN cap nhat; ngu ve poll.
        window.location.assign(data.checkoutUrl);
        return;
      }
      // COD: khong co checkoutUrl — di thang trang ket qua.
      navigate(`/payment/result?orderId=${encodeURIComponent(data.orderId)}`);
    } catch (e) {
      const err = e as { status?: number; message?: string };
      setSubmitError(
        err.message ??
          (err.status === 409
            ? 'Thông tin đơn thay đổi (giá/tồn). Vui lòng kiểm tra lại giỏ hàng.'
            : 'Đặt hàng thất bại. Vui lòng thử lại.'),
      );
    }
  };

  if (cartQ.isLoading) return <ListSkeleton />;

  return (
    <div style={{ maxWidth: 960, margin: '0 auto' }}>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          Thanh toán
        </Typography.Title>
        <ApiErrorAlert message={(cartQ.error as { message?: string } | undefined)?.message ?? null} />
        {submitError && <Alert type="error" showIcon message={submitError} closable />}

        <Card size="small" title={`Giỏ hàng (${cart?.totalQuantity ?? 0} món)`}>
          {items.map((i) => (
            <Space key={i.skuId} style={{ justifyContent: 'space-between', width: '100%' }} align="center">
              <Typography.Text>
                {i.productName} × {i.quantity}
              </Typography.Text>
              <Typography.Text className="money">{formatVnd(i.lineAmount)}</Typography.Text>
            </Space>
          ))}
          <Space style={{ justifyContent: 'space-between', width: '100%', marginTop: 12 }}>
            <Typography.Text strong>Tổng thanh toán</Typography.Text>
            <Typography.Text strong className="money" style={{ color: '#e8543f', fontSize: 18 }}>
              {formatVnd(total)}
            </Typography.Text>
          </Space>
        </Card>

        <Card size="small" title="Thông tin giao hàng">
          <Form<FormValues>
            layout="vertical"
            onFinish={onFinish}
            initialValues={{ paymentMethod: 'COD' }}
            disabled={checkoutState.isLoading}
          >
            <Form.Item
              name="recipientName"
              label="Tên người nhận"
              rules={[{ required: true, message: 'Nhập tên người nhận' }]}
            >
              <Input placeholder="Nguyễn Văn A" maxLength={128} />
            </Form.Item>
            <Form.Item
              name="recipientPhone"
              label="Số điện thoại"
              rules={[
                { required: true, message: 'Nhập số điện thoại' },
                { pattern: /^0\d{8,11}$/, message: 'Số điện thoại Việt Nam không hợp lệ' },
              ]}
            >
              <Input placeholder="09xxxxxxxx" maxLength={32} />
            </Form.Item>
            <Form.Item
              name="shippingAddress"
              label="Địa chỉ giao hàng"
              rules={[{ required: true, message: 'Nhập địa chỉ giao hàng' }]}
            >
              <Input.TextArea placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành" maxLength={512} rows={2} />
            </Form.Item>
            <Form.Item name="paymentMethod" label="Phương thức thanh toán">
              <Radio.Group>
                <Space direction="vertical">
                  <Radio value="COD">Thanh toán khi nhận hàng (COD)</Radio>
                  <Radio value="VNPAY">VNPAY — chuyển đến trang thanh toán</Radio>
                  <Radio value="ZALOPAY">ZaloPay — quét mã QR</Radio>
                </Space>
              </Radio.Group>
            </Form.Item>
            <Button type="primary" size="large" htmlType="submit" loading={checkoutState.isLoading}>
              Đặt hàng
            </Button>
          </Form>
        </Card>
      </Space>
    </div>
  );
}
