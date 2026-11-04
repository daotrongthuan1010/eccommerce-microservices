import { App, Button, Card, Descriptions, Modal, Space, Tag, Typography } from 'antd';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useCancelOrderMutation, useMyOrderQuery, type Order } from '../features/order/orderApi';
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
  const orderQ = useMyOrderQuery(orderId, { skip: !orderId });
  const [cancelMut, cancelState] = useCancelOrderMutation();
  const [cancelOpen, setCancelOpen] = useState(false);

  const order: Order | undefined = orderQ.data?.data;
  const canCancel = order?.status === 'PENDING_PAYMENT';

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
    </div>
  );
}
