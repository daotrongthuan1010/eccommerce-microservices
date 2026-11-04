import { Alert, Button, Result, Space, Spin, Typography } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { usePaymentByOrderQuery } from '../features/order/orderApi';

// Marketplace PaymentResultPage (2026-11-05 fe-marketplace D6, spec
// customer/08): sau khi quay lai tu gateway thanh toan (VNPAY/ZaloPay) hoac
// dat COD. Nguon su that la IPN webhook; FE poll GET /payments/by-order
// 2s/lan toi da 60s de hien thi. Khong bao gio ket luan UNKNOWN la thanh
// cong (AGENTS.md luat 8) — het han poll thi huong dan kiem tra lai sau.

const POLL_INTERVAL_MS = 2000;
const MAX_POLLS = 30;

interface PaymentInfo {
  id?: string;
  status?: string;
  amount?: number;
  method?: string;
}

function isTerminal(status?: string): boolean {
  return (
    status === 'PAID' ||
    status === 'FAILED' ||
    status === 'CANCELLED' ||
    status === 'EXPIRED' ||
    status === 'REFUNDED'
  );
}

export default function PaymentResultPage() {
  const [searchParams] = useSearchParams();
  const orderId = searchParams.get('orderId') ?? '';
  const [pollCount, setPollCount] = useState(0);
  const stopRef = useRef(false);

  const paymentQ = usePaymentByOrderQuery(orderId, {
    skip: !orderId,
    // Poll tay qua effect duoi de kiem soat so lan + dung khi terminal.
    refetchOnFocus: false,
  });

  useEffect(() => {
    if (!orderId || stopRef.current) return;
    const data = paymentQ.data?.data;
    const list: PaymentInfo[] = Array.isArray(data) ? data : data ? [data] : [];
    const latest = list[0];
    if (pollCount >= MAX_POLLS || isTerminal(latest?.status)) {
      stopRef.current = true;
      return;
    }
    const t = setTimeout(() => {
      setPollCount((c) => c + 1);
      void paymentQ.refetch();
    }, POLL_INTERVAL_MS);
    return () => clearTimeout(t);
  }, [pollCount, paymentQ.data, orderId]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!orderId) {
    return (
      <Result
        status="warning"
        title="Thiếu mã đơn hàng"
        subTitle="Không tìm thấy orderId trên URL."
        extra={
          <Link to="/orders">
            <Button type="primary">Đơn hàng của tôi</Button>
          </Link>
        }
      />
    );
  }

  const data = paymentQ.data?.data;
  const list: PaymentInfo[] = Array.isArray(data) ? data : data ? [data] : [];
  const latest = list[0];
  const status = latest?.status;
  const timedOut = pollCount >= MAX_POLLS && !isTerminal(status);

  if (paymentQ.isLoading && pollCount === 0) {
    return (
      <div style={{ textAlign: 'center', padding: 64 }}>
        <Spin size="large" />
        <Typography.Paragraph style={{ marginTop: 16 }}>
          Đang kiểm tra kết quả thanh toán...
        </Typography.Paragraph>
      </div>
    );
  }

  if (status === 'PAID') {
    return (
      <Result
        status="success"
        title="Thanh toán thành công!"
        subTitle={`Đơn hàng ${orderId} đã được thanh toán. Chúng tôi sẽ sớm chuẩn bị đơn cho bạn.`}
        extra={
          <Space>
            <Link to={`/orders/${orderId}`}>
              <Button type="primary">Xem đơn hàng</Button>
            </Link>
            <Link to="/products">
              <Button>Tiếp tục mua sắm</Button>
            </Link>
          </Space>
        }
      />
    );
  }

  if (status === 'FAILED' || status === 'CANCELLED' || status === 'EXPIRED') {
    return (
      <Result
        status="error"
        title="Thanh toán không thành công"
        subTitle={`Trạng thái: ${status ?? 'UNKNOWN'}. Bạn có thể thử thanh toán lại từ đơn hàng.`}
        extra={
          <Space>
            <Link to={`/orders/${orderId}`}>
              <Button type="primary">Xem đơn hàng</Button>
            </Link>
            <Link to="/products">
              <Button>Tiếp tục mua sắm</Button>
            </Link>
          </Space>
        }
      />
    );
  }

  return (
    <div style={{ textAlign: 'center', padding: 48 }}>
      <Spin size="large" />
      <Typography.Title level={4} style={{ marginTop: 16 }}>
        Đang chờ xác nhận thanh toán...
      </Typography.Title>
      <Typography.Paragraph type="secondary">
        Đơn hàng {orderId} đang được xử lý. Hệ thống tự động kiểm tra mỗi 2 giây
        (tối đa 1 phút).
      </Typography.Paragraph>
      {timedOut && (
        <Alert
          style={{ maxWidth: 480, margin: '0 auto' }}
          type="warning"
          showIcon
          message="Chưa nhận được xác nhận trong thời gian chờ"
          description="Thanh toán có thể đang được đối soát. Vui lòng kiểm tra lại trong Đơn hàng của tôi sau vài phút."
        />
      )}
      <Space style={{ marginTop: 16 }}>
        <Link to={`/orders/${orderId}`}>
          <Button>Xem đơn hàng</Button>
        </Link>
        <Link to="/products">
          <Button type="primary">Tiếp tục mua sắm</Button>
        </Link>
      </Space>
    </div>
  );
}
