import { useEffect, useState } from 'react';
import { Button, Spin, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import keycloak, { initKeycloak, keycloakTokens } from '../features/auth/keycloak';
import { fetchMe, keycloakAuthenticated } from '../features/auth/authSlice';
import logoUrl from '../assets/logo.png';

// FE_AUTH_GATEWAY: chi dang nhap qua Keycloak SSO (PKCE S256), KHONG con form
// username/password. Vao /login la tu redirect sang Keycloak; man hinh nay chi
// la trang cho kieu MISA trong luc redirect (logo HTAD + spinner).
const BRAND = '#0F4C81';

export default function LoginPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const status = useAppSelector((s) => s.auth.status);
  const [failed, setFailed] = useState(false);
  const [busy, setBusy] = useState(false);

  // Vao /login: neu Keycloak da authenticated (tro ve tu redirect) thi nap
  // tokens + GET /api/auth/me lay roles roi vao app; chua thi redirect thang.
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
        await keycloak.login();
      } catch {
        if (!cancelled) setFailed(true);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [dispatch, navigate]);

  const retry = async () => {
    setBusy(true);
    try {
      await keycloak.login();
    } catch {
      setFailed(true);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div style={{ display: 'flex', minHeight: '100vh' }}>
      <div
        style={{
          flex: '1 1 52%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: 48,
          color: '#fff',
          background:
            'radial-gradient(1100px 500px at 85% -10%, rgba(255,255,255,0.14), transparent 60%), linear-gradient(135deg, #0a3559 0%, #0F4C81 60%, #14619f 100%)',
        }}
      >
        <div style={{ maxWidth: 460 }}>
          <img
            src={logoUrl}
            alt="HTAD"
            style={{ height: 76, background: '#fff', borderRadius: 12, padding: '8px 14px', marginBottom: 28 }}
          />
          <h1 style={{ fontSize: 34, margin: '0 0 12px' }}>Hằng Tuấn An Đạt</h1>
          <p style={{ fontSize: 16, opacity: 0.92 }}>
            Ecommerce — quản trị bán hàng, kho vận và đơn hàng trên một nền tảng.
          </p>
        </div>
      </div>
      <div
        style={{
          flex: '1 1 48%',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: 40,
          background: '#f4f6f9',
        }}
      >
        <div
          style={{
            width: '100%',
            maxWidth: 420,
            background: '#fff',
            borderRadius: 14,
            boxShadow: '0 12px 40px rgba(15,76,129,0.12)',
            padding: '36px',
            textAlign: 'center',
          }}
        >
          <img src={logoUrl} alt="HTAD Ecommerce" style={{ height: 64, marginBottom: 14 }} />
          {failed ? (
            <>
              <Typography.Text type="danger">
                Không kết nối được Keycloak, vui lòng thử lại sau.
              </Typography.Text>
              <Button
                type="primary"
                block
                loading={busy}
                onClick={retry}
                style={{ background: BRAND, borderColor: BRAND, marginTop: 16, height: 44 }}
              >
                Thử lại
              </Button>
            </>
          ) : (
            <>
              <Spin tip="Đang chuyển đến trang đăng nhập..." style={{ margin: '12px 0' }} />
              <Typography.Paragraph type="secondary" style={{ marginTop: 12 }}>
                {status === 'loading'
                  ? 'Đang tải thông tin tài khoản...'
                  : 'Nếu không tự chuyển, nhấn nút bên dưới.'}
              </Typography.Paragraph>
              <Button
                type="primary"
                block
                loading={busy}
                onClick={retry}
                style={{ background: BRAND, borderColor: BRAND, marginTop: 8, height: 44 }}
              >
                Đăng nhập bằng Keycloak
              </Button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
