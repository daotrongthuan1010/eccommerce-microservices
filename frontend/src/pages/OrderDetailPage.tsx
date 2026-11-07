import { App, Button, Card, Descriptions, Form, Input, InputNumber, Modal, Space, Tag, Typography } from 'antd';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useCancelOrderMutation, useMyOrderQuery, usePaymentByOrderQuery, type Order } from '../features/order/orderApi';
import { useCreateRefundMutation } from '../features/payment/paymentApi';
import { useAppSelector } from '../app/hooks';
import { ApiErrorAlert, ListSkeleton } from '../components/ui';
import { formatVnd } from '../components/marketplace/ProductCardGrid';

// Marketplace OrderDetailPage (2026-11-05 fe-marketplace D6, spec
// customer/10): chi tiet don + huy don khi PENDING_PAYMENT (BE chi cho huy
// trang thai nay — OrderServiceImpl).
const STATUS_COLORS: Record<string, string> = {
  PENDING_PAYMENT: 'orange',
  PAID: 'green',
  PROCESSING: 'blue',
  SHIPPED: 'geekblue',
  DELIVERED: 'green',
  CANCELLED: 'default',
  COMPLETED: 'green',
};

export default function OrderDetailPage() {
  const { orderId = '' } = useParams<{ orderId: string }>();
  const { message } = App.useApp();
  const isAdmin = useAppSelector((s) => s.auth.roles).includes('ADMIN');
  const orderQ = useMyOrderQuery(orderId, { skip: !orderId });
  const [cancelMut, cancelState] = useCancelOrderMutation();
  const [cancelOpen, setCancelOpen] = useState(false);
  // Hoan tien (ADMIN): can payment de lay paymentId + trang thai PAID/PARTIALLY_REFUNDED.
  const paymentQ = usePaymentByOrderQuery(orderId, { skip: !orderId || !isAdmin });
  const [createRefund, refundState] = useCreateRefundMutation();
  const [refundOpen, setRefundOpen] = useState(false);
  const [refundForm] = Form.useForm();

  const order: Order | undefined = orderQ.data?.data;
  const canCancel = order?.status === 'PENDING_PAYMENT';

  const rawPayment = paymentQ.data?.data;
  const payment = Array.isArray(rawPayment) ? rawPayment[0] : rawPayment;
  const paymentId = (payment?.id as string | undefined) ?? null;
  const paymentStatus = payment?.status as string | undefined;
  const canRefund =
    isAdmin &&
    !!paymentId &&
    (paymentStatus === 'PAID' || paymentStatus === 'PARTIALLY_REFUNDED');
  const refundedAmount = Number((payment as { refundedAmount?: unknown } | undefined)?.refundedAmount ?? 0) || 0;
  const maxRefund = Math.max((order?.totalAmount ?? 0) - refundedAmount, 0);

  const onConfirmCancel = async () => {
    try {
      await cancelMut({ orderId }).unwrap();
      message.success('Đã hủy đơn hàng');
      setCancelOpen(false);
      void orderQ.refetch();
    } catch {
      message.error('Hủy đơn hàng thất bại');
    }
  };

  const onConfirmRefund = async () => {
    if (!paymentId) return;
    try {
      const v = await refundForm.validateFields();
      await createRefund({
        paymentId,
        amount: v.amount as number,
        reason: (v.reason as string | undefined)?.trim() || undefined,
        idempotencyKey: crypto.randomUUID(),
      }).unwrap();
      message.success('Đã tạo hoàn tiền');
      setRefundOpen(false);
      refundForm.resetFields();
      void paymentQ.refetch();
    } catch {
      // validate loi → form tu hien thi; loi BE → hien message.
      if (refundForm.getFieldsError().every((f) => f.errors.length === 0)) {
        message.error('Tạo hoàn tiền thất bại');
      }
    }
  };

  if (orderQ.isLoading) return <ListSkeleton />;

  return (
    <div style={{ maxWidth: 960, margin: '0 auto' }}>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space style={{ justifyContent: 'space-between', width: '100%' }} wrap>
          <Typography.Title level={3} style={{ margin: 0 }}>
            Đơn hàng {order?.orderCode ?? orderId}
          </Typography.Title>
          <Space>
            {canCancel && (
              <Button danger onClick={() => setCancelOpen(true)} loading={cancelState.isLoading}>
                Hủy đơn
              </Button>
            )}
            {canRefund && (
              <Button
                onClick={() => {
                  refundForm.setFieldsValue({ amount: maxRefund, reason: '' });
                  setRefundOpen(true);
                }}
              >
                Hoàn tiền
              </Button>
            )}
            <Link to="/orders">
              <Button>← Đơn hàng của tôi</Button>
            </Link>
          </Space>
        </Space>

        <ApiErrorAlert message={(orderQ.error as { message?: string } | undefined)?.message ?? null} />

        {order && (
          <>
            <Card size="small">
              <Descriptions column={{ xs: 1, sm: 2 }} size="small">
                <Descriptions.Item label="Trạng thái">
                  <Tag color={STATUS_COLORS[order.status] ?? 'default'}>{order.status}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Phương thức">{order.paymentMethod}</Descriptions.Item>
                <Descriptions.Item label="Người nhận">{order.recipientName}</Descriptions.Item>
                <Descriptions.Item label="SĐT">{order.recipientPhone}</Descriptions.Item>
                <Descriptions.Item label="Địa chỉ" span={2}>
                  {order.shippingAddress}
                </Descriptions.Item>
                {order.cancelReason && (
                  <Descriptions.Item label="Lý do hủy" span={2}>
                    {order.cancelReason}
                  </Descriptions.Item>
                )}
              </Descriptions>
            </Card>

            <Card size="small" title="Sản phẩm">
              {order.lines.map((l) => (
                <Space
                  key={l.skuId}
                  style={{ justifyContent: 'space-between', width: '100%' }}
                  align="center"
                >
                  <Typography.Text>
                    {l.productName} × {l.quantity}
                  </Typography.Text>
                  <Typography.Text className="money">{formatVnd(l.lineAmount)}</Typography.Text>
                </Space>
              ))}
              <Space style={{ justifyContent: 'space-between', width: '100%', marginTop: 12 }}>
                <Typography.Text strong>Tổng cộng</Typography.Text>
                <Typography.Text strong className="money" style={{ color: '#e8543f', fontSize: 18 }}>
                  {formatVnd(order.totalAmount)}
                </Typography.Text>
              </Space>
            </Card>

            {order.status === 'PENDING_PAYMENT' && order.paymentMethod !== 'COD' && (
              <Card size="small" title="Thanh toán">
                <Space direction="vertical">
                  <Typography.Text type="secondary">
                    Đơn đang chờ thanh toán qua {order.paymentMethod}.
                  </Typography.Text>
                  <Link to={`/payment/result?orderId=${encodeURIComponent(order.id)}`}>
                    <Button type="primary">Kiểm tra trạng thái thanh toán</Button>
                  </Link>
                </Space>
              </Card>
            )}
          </>
        )}
      </Space>

      <Modal
        title="Hủy đơn hàng?"
        open={cancelOpen}
        onCancel={() => setCancelOpen(false)}
        onOk={() => void onConfirmCancel()}
        okText="Hủy đơn"
        okButtonProps={{ danger: true, loading: cancelState.isLoading }}
        cancelText="Giữ đơn"
      >
        <Typography.Paragraph>
          Đơn hàng đang chờ thanh toán nên có thể hủy. Hành động này không thể hoàn tác.
        </Typography.Paragraph>
      </Modal>

      <Modal
        title="Hoàn tiền"
        open={refundOpen}
        onCancel={() => setRefundOpen(false)}
        onOk={() => void onConfirmRefund()}
        okText="Xác nhận hoàn tiền"
        okButtonProps={{ loading: refundState.isLoading }}
        cancelText="Hủy"
        destroyOnClose
      >
        <Form form={refundForm} layout="vertical" preserve={false}>
          <Form.Item
            name="amount"
            label={`Số tiền (tối đa ${formatVnd(maxRefund)})`}
            rules={[
              { required: true, message: 'Nhập số tiền hoàn' },
              { type: 'number', min: 1, max: maxRefund, message: `Số tiền từ 1 đến ${formatVnd(maxRefund)}` },
            ]}
          >
            <InputNumber style={{ width: '100%' }} min={1} max={maxRefund} />
          </Form.Item>
          <Form.Item
            name="reason"
            label="Lý do"
            rules={[{ max: 512, message: 'Tối đa 512 ký tự' }]}
          >
            <Input.TextArea rows={3} placeholder="Lý do hoàn tiền (không bắt buộc)" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
