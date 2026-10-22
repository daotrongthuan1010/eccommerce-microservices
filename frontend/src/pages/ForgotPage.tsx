import { Alert, Button, Card, Form, Input, message } from 'antd';
import { useState } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { ApiErrorAlert, PageHeader } from '../components/ui';
import { forgotPassword, type AuthError } from '../features/auth/authSlice';

// real 2026-10-22: POST /api/auth/forgot-password — luon { sent: true }.
export default function ForgotPage() {
  const dispatch = useAppDispatch();
  const status = useAppSelector((s) => s.auth.status);
  const error = useAppSelector((s) => s.auth.error);
  const [sent, setSent] = useState(false);
  const loading = status === 'loading';

  const onFinish = async (values: { username: string }) => {
    try {
      await dispatch(forgotPassword(values.username)).unwrap();
      setSent(true);
    } catch (e) {
      const err = e as AuthError;
      if (err?.message) message.error(err.message);
    }
  };

  return (
    <div style={{ maxWidth: 420, margin: '48px auto' }}>
      <PageHeader title="Quên mật khẩu" subTitle="Nhận hướng dẫn đặt lại mật khẩu" />
      {sent ? (
        <Alert
          type="success"
          showIcon
          message="Đã gửi yêu cầu"
          description="Nếu tài khoản tồn tại, hướng dẫn đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra email."
        />
      ) : (
        <Card>
          <ApiErrorAlert message={error} />
          <Form layout="vertical" onFinish={onFinish}>
            <Form.Item
              name="username"
              label="Tên đăng nhập"
              rules={[{ required: true, message: 'Vui lòng nhập tên đăng nhập' }]}
            >
              <Input placeholder="Tên đăng nhập hoặc email" />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" block loading={loading}>
                Gửi yêu cầu
              </Button>
            </Form.Item>
          </Form>
        </Card>
      )}
    </div>
  );
}
