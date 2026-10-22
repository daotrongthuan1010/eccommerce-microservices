import { Button, Card, Form, Input, message } from 'antd';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { ApiErrorAlert, PageHeader } from '../components/ui';
import { changePassword, type AuthError } from '../features/auth/authSlice';

// real 2026-10-22: POST /api/auth/change-password (Bearer) — sai pass cu → 400 wrong_password.
export default function ChangePasswordPage() {
  const [form] = Form.useForm();
  const dispatch = useAppDispatch();
  const status = useAppSelector((s) => s.auth.status);
  const error = useAppSelector((s) => s.auth.error);
  const loading = status === 'loading';

  const onFinish = async (values: { currentPassword: string; newPassword: string }) => {
    try {
      await dispatch(changePassword(values)).unwrap();
      message.success('Đổi mật khẩu thành công');
      form.resetFields();
    } catch (e) {
      const err = e as AuthError;
      if (err?.code === 'wrong_password') {
        form.setFields([{ name: 'currentPassword', errors: ['Mật khẩu hiện tại không đúng'] }]);
      } else if (err?.message) {
        message.error(err.message);
      }
    }
  };

  return (
    <div style={{ maxWidth: 480, margin: '32px auto' }}>
      <PageHeader title="Đổi mật khẩu" />
      <Card>
        <ApiErrorAlert message={error} />
        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Form.Item
            name="currentPassword"
            label="Mật khẩu hiện tại"
            rules={[{ required: true, message: 'Vui lòng nhập mật khẩu hiện tại' }]}
          >
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Form.Item
            name="newPassword"
            label="Mật khẩu mới"
            rules={[
              { required: true, message: 'Vui lòng nhập mật khẩu mới' },
              { min: 8, message: 'Mật khẩu tối thiểu 8 ký tự' },
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading}>
              Đổi mật khẩu
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
