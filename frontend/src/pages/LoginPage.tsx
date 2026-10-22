import { useEffect, useState } from 'react';
import { Button, Card, Form, Input, Typography, message, Spin } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { ApiErrorAlert, PageHeader } from '../components/ui';
import keycloak, { initKeycloak, keycloakTokens } from '../features/auth/keycloak';
import {
  fetchMe,
  keycloakAuthenticated,
  login,
  type AuthError,
} from '../features/auth/authSlice';
import logoUrl from '../assets/logo.png';

// FE_AUTH_GATEWAY §7: man chao + Dang nhap bang Keycloak (SSO PKCE S256).
// Form username/password cu giu lai (proxy password-grant qua auth-service).
const BRAND = '#0F4C81';

export default function LoginPage() {
  const [form] = Form.useForm();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const status = useAppSelector((s) => s.auth.status);
  const error = useAppSelector((s) => s.auth.error);
  const loading = status === 'loading';
  const [ssoBusy, setSsoBusy] = useState(false);
  const [ssoChecking, setSsoChecking] = useState(true);

  // Vao /login: neu Keycloak da authenticated (tro ve tu redirect) thi
  // nap tokens vao memory + GET /api/auth/me lay roles roi vao app.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const authenticated = await initKeycloak();
        if (cancelled) return;
        if (authenticated) {
          const tokens = keycloakTokens();
          if (tokens) {
            dispatch(keycloakAuthenticated(tokens));
            await dispatch(fetchMe()).unwrap();
            navigate('/', { replace: true });
            return;
          }
        }
      } catch {
        // Keycloak khong kha dung: van cho login form cu
      } finally {
        if (!cancelled) setSsoChecking(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [dispatch, navigate]);

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

  const onKeycloakLogin = async () => {
    setSsoBusy(true);
    try {
      const authenticated = await initKeycloak();
      if (authenticated) {
        const tokens = keycloakTokens();
        if (tokens) {
          dispatch(keycloakAuthenticated(tokens));
          await dispatch(fetchMe()).unwrap();
          message.success('Đăng nhập thành công');
          navigate('/', { replace: true });
          return;
        }
      }
      await keycloak.login();
    } catch (e) {
      message.error('Không kết nối được Keycloak, vui lòng thử lại sau');
    } finally {
      setSsoBusy(false);
    }
  };

  return (
    <div style={{ maxWidth: 420, margin: '48px auto', textAlign: 'center' }}>
      <img src={logoUrl} alt="Ecommerce logo" style={{ height: 72, marginBottom: 12 }} />
      <PageHeader title="Chào mừng đến với Ecommerce" subTitle="Đăng nhập để tiếp tục" />
      <ApiErrorAlert message={error} />
      <Card>
        {ssoChecking ? (
          <Spin tip="Đang kiểm tra phiên Keycloak..." style={{ margin: '12px 0' }} />
        ) : (
          <Button
            type="primary"
            block
            loading={ssoBusy}
            onClick={onKeycloakLogin}
            style={{ background: BRAND, borderColor: BRAND, marginBottom: 16 }}
          >
            Đăng nhập bằng Keycloak
          </Button>
        )}
        <Typography.Text type="secondary">hoặc đăng nhập bằng tài khoản</Typography.Text>
        <Form form={form} layout="vertical" onFinish={onFinish} style={{ marginTop: 12 }}>
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
            <Button type="default" htmlType="submit" block loading={loading}>
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
