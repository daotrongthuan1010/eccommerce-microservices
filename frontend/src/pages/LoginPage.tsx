import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Card, Form, Input, Typography, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { ApiErrorAlert, PageHeader } from '../components/ui';
import { login, type AuthError } from '../features/auth/authSlice';

// FE_AUTH_GATEWAY §7: invalid_grant → message, validation_failed → setFields.
export default function LoginPage() {
  const [form] = Form.useForm();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const status = useAppSelector((s) => s.auth.status);
  const error = useAppSelector((s) => s.auth.error);
  const loading = status === 'loading';

  const onFinish = async (values: { username: string; password: string }) => {
    try {
      await dispatch(login(values)).unwrap();
      message.success('Đăng nhập thành công');
      navigate('/');
    } catch (e) {
      const err = e as AuthError;
      if (err?.code === 'validation_failed' && err.fields) {
        form.setFields(
          Object.entries(err.fields).map(([name, errors]) => ({
            name,
            errors: [errors],
          })),
        );
      } else if (err?.code === 'invalid_grant') {
        message.error('Sai tên đăng nhập hoặc mật khẩu');
      } else if (err?.message) {
        message.error(err.message);
      }
    }
  };

  return (
    <div style={{ maxWidth: 420, margin: '48px auto' }}>
      <PageHeader title="Đăng nhập" subTitle="Ecommerce storefront" />
      <ApiErrorAlert message={error} />
      <Card>
        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Form.Item
            name="username"
            label="Tên đăng nhập"
            rules={[{ required: true, message: 'Vui lòng nhập tên đăng nhập' }]}
          >
            <Input prefix={<UserOutlined />} placeholder="admin" autoComplete="username" />
          </Form.Item>
          <Form.Item
            name="password"
            label="Mật khẩu"
            rules={[{ required: true, message: 'Vui lòng nhập mật khẩu' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="Mật khẩu"
              autoComplete="current-password"
            />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading}>
              Đăng nhập
            </Button>
          </Form.Item>
        </Form>
        <Typography.Text type="secondary">
          Chưa có tài khoản? <a href="/register">Đăng ký</a> · <a href="/forgot">Quên mật khẩu</a>
        </Typography.Text>
      </Card>
    </div>
  );
}
