import { Alert, App, Button, Card, Checkbox, Form, Input } from 'antd';
import { useState } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { ApiErrorAlert, PageHeader } from '../components/ui';
import { register, type AuthError } from '../features/auth/authSlice';

// FE_AUTH_GATEWAY §7: 501 registration_not_configured → an form, hien Alert.
export default function RegisterPage() {
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const dispatch = useAppDispatch();
  const status = useAppSelector((s) => s.auth.status);
  const error = useAppSelector((s) => s.auth.error);
  const [done, setDone] = useState<string | null>(null);
  const [notConfigured, setNotConfigured] = useState(false);
  const loading = status === 'loading';

  const onFinish = async (values: {
    username: string;
    email: string;
    password: string;
    firstName?: string;
    lastName?: string;
    seller?: boolean;
  }) => {
    try {
      const res = await dispatch(register(values)).unwrap();
      setDone(
        values.seller
          ? `Đã tạo tài khoản bán hàng ${res.username}. Vui lòng kiểm tra email để xác minh rồi đăng nhập.`
          : `Đã tạo tài khoản ${res.username}. Vui lòng kiểm tra email để xác minh rồi đăng nhập.`,
      );
    } catch (e) {
      const err = e as AuthError;
      if (err?.code === 'registration_not_configured') {
        setNotConfigured(true);
      } else if (err?.code === 'validation_failed' && err.fields) {
        form.setFields(
          Object.entries(err.fields).map(([name, errors]) => ({ name, errors: [errors] })),
        );
      } else if (err?.message) {
        message.error(err.message);
      }
    }
  };

  return (
    <div style={{ maxWidth: 480, margin: '48px auto' }}>
      <PageHeader title="Đăng ký" subTitle="Tạo tài khoản mua sắm — xác minh qua email" />
      {notConfigured ? (
        <Alert
          type="warning"
          showIcon
          message="Chức năng đăng ký chưa được cấu hình"
          description="Hệ thống chưa bật đăng ký tự do. Vui lòng liên hệ quản trị viên để được tạo tài khoản."
        />
      ) : done ? (
        <Alert type="success" showIcon message={done} description={<a href="/login">Đăng nhập ngay</a>} />
      ) : (
        <Card>
          <ApiErrorAlert message={error} />
          <Form form={form} layout="vertical" onFinish={onFinish}>
            <Form.Item
              name="username"
              label="Tên đăng nhập"
              rules={[
                { required: true, message: 'Vui lòng nhập tên đăng nhập' },
                { min: 3, max: 64, message: 'Tên đăng nhập 3-64 ký tự' },
                { pattern: /^[a-zA-Z0-9._-]+$/, message: 'Chỉ gồm chữ, số, . _ -' },
              ]}
            >
              <Input placeholder="username" autoComplete="username" />
            </Form.Item>
            <Form.Item
              name="email"
              label="Email"
              rules={[
                { required: true, message: 'Vui lòng nhập email' },
                { type: 'email', message: 'Email không hợp lệ' },
              ]}
            >
              <Input placeholder="you@example.com" autoComplete="email" />
            </Form.Item>
            <Form.Item
              name="password"
              label="Mật khẩu"
              rules={[
                { required: true, message: 'Vui lòng nhập mật khẩu' },
                { min: 8, message: 'Mật khẩu tối thiểu 8 ký tự' },
              ]}
            >
              <Input.Password placeholder="Tối thiểu 8 ký tự" autoComplete="new-password" />
            </Form.Item>
            <Form.Item name="firstName" label="Tên">
              <Input placeholder="Tên (tùy chọn)" />
            </Form.Item>
            <Form.Item name="lastName" label="Họ">
              <Input placeholder="Họ (tùy chọn)" />
            </Form.Item>
            <Form.Item name="seller" valuePropName="checked">
              <Checkbox>Đăng ký bán hàng (mở shop trên sàn)</Checkbox>
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" block loading={loading}>
                Đăng ký
              </Button>
            </Form.Item>
          </Form>
        </Card>
      )}
    </div>
  );
}
