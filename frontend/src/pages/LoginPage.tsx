import { useEffect, useState } from 'react';
import { Alert, Button, Spin, Typography } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import keycloak, { initKeycloak, keycloakTokens } from '../features/auth/keycloak';
import { fetchMe, keycloakAuthenticated, loggedOut, logout } from '../features/auth/authSlice';
import logoUrl from '../assets/logo.png';

// FE_AUTH_GATEWAY: chi dang nhap qua Keycloak SSO (PKCE S256), KHONG con form
// username/password. Vao /login la tu redirect sang Keycloak; man hinh nay chi
// la trang cho trong luc redirect (logo + spinner).
//
// 2 bien the (2026-11-05 tach login shopper / quan tri):
// - shopper (default, /login): tông cam marketplace, copy mua sam, sau SSO ve "/"
// - admin (/admin/login): tông navy backoffice, copy quan tri, sau SSO ve "/admin"
// Cung 1 flow Keycloak — chi khac theme + landing.
const SHOPPER = {
  brand: '#e8543f',
  gradient:
    'radial-gradient(1100px 500px at 85% -10%, rgba(255,255,255,0.16), transparent 60%), linear-gradient(135deg, #b3301f 0%, #e8543f 60%, #f07a4b 100%)',
  title: 'Mua sắm cùng HTAD',
  subtitle: 'Hàng ngàn sản phẩm chính hãng — giá tốt mỗi ngày.',
  spinner: 'Đang chuyển đến trang đăng nhập...',
  landing: '/',
};

const ADMIN = {
  brand: '#00214f',
  gradient:
    'radial-gradient(1100px 500px at 85% -10%, rgba(255,255,255,0.14), transparent 60%), linear-gradient(135deg, #0a3559 0%, #00214f 60%, #14619f 100%)',
  title: 'Hằng Tuấn An Đạt',
  subtitle: 'Ecommerce — quản trị bán hàng, kho vận và đơn hàng trên một nền tảng.',
  spinner: 'Đang chuyển đến trang quản trị...',
  landing: '/admin',
};

export default function LoginPage({ variant = 'shopper' }: { variant?: 'shopper' | 'admin' }) {
  const t = variant === 'admin' ? ADMIN : SHOPPER;
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const status = useAppSelector((s) => s.auth.status);
  const [failed, setFailed] = useState(false);
  const [busy, setBusy] = useState(false);
  // Gate xac thuc email: tai khoan tu dang ky chua verify -> khong vao app,
  // hien man huong dan kiem tra email thay vi redirect loop vo ich.
  const [needVerify, setNeedVerify] = useState(false);
  // Variant admin: user da login SSO nhung KHONG CO role ADMIN — khong dieu
  // huong (se bi /403 roi hut ve trang mua hang), hien canh bao tai cho.
  const [noAdminRole, setNoAdminRole] = useState(false);

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
            const me = await dispatch(fetchMe()).unwrap();
            if (me.emailVerified === false) {
              // Chua xac thuc email: thoat SSO (xoa token Keycloak + memory),
              // giu man huong dan cho user thay vi vao app that bai lan sau.
              dispatch(loggedOut());
              setNeedVerify(true);
              return;
            }
            // Admin login: chi ADMIN moi duoc vao khu quan tri. User thuong
            // (buyer) co session SSO song cung khong duoc hut vao /admin → 403.
            if (variant === 'admin' && !me.admin) {
              setNoAdminRole(true);
              return;
            }
            navigate(t.landing, { replace: true });
            return;
          }
        }
        await keycloak.login();
      } catch (e) {
        // eslint-disable-next-line no-console
        console.error('[login] keycloak init failed:', e);
        if (!cancelled) setFailed(true);
      }
    })();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dispatch, navigate, location.pathname]);

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
          background: t.gradient,
        }}
      >
        <div style={{ maxWidth: 460 }}>
          <img
            src={logoUrl}
            alt="HTAD"
            style={{ height: 76, background: '#fff', borderRadius: 12, padding: '8px 14px', marginBottom: 28 }}
          />
          <h1 style={{ fontSize: 34, margin: '0 0 12px' }}>{t.title}</h1>
          <p style={{ fontSize: 16, opacity: 0.92 }}>{t.subtitle}</p>
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
            boxShadow: `0 12px 40px ${t.brand}22`,
            padding: '36px',
            textAlign: 'center',
          }}
        >
          <img src={logoUrl} alt="HTAD Ecommerce" style={{ height: 64, marginBottom: 14 }} />
          {noAdminRole ? (
            <>
              <Alert
                type="error"
                showIcon
                message="Tài khoản không có quyền quản trị"
                description="Tài khoản hiện tại không có role ADMIN. Hãy đăng xuất tài khoản mua sắm và đăng nhập bằng tài khoản quản trị (do hệ thống cấp)."
              />
              <Button
                block
                style={{ marginTop: 16, height: 44 }}
                onClick={async () => {
                  await dispatch(logout()).catch(() => undefined);
                  window.location.reload();
                }}
              >
                Đăng xuất tài khoản này để đổi tài khoản quản trị
              </Button>
            </>
          ) : needVerify ? (
            <>
              <Alert
                type="warning"
                showIcon
                message="Tài khoản chưa xác thực email"
                description="Chúng tôi đã gửi liên kết xác nhận đến email của bạn. Vui lòng kiểm tra hộp thư (kể cả mục Spam) và nhấn liên kết để kích hoạt tài khoản trước khi đăng nhập."
              />
              <Button
                type="primary"
                block
                style={{ background: t.brand, borderColor: t.brand, marginTop: 16, height: 44 }}
                onClick={() => window.location.reload()}
              >
                Tôi đã xác thực — thử lại
              </Button>
            </>
          ) : failed ? (
            <>
              <Typography.Text type="danger">
                Không kết nối được Keycloak, vui lòng thử lại sau.
              </Typography.Text>
              <Button
                type="primary"
                block
                loading={busy}
                onClick={retry}
                style={{ background: t.brand, borderColor: t.brand, marginTop: 16, height: 44 }}
              >
                Thử lại
              </Button>
            </>
          ) : (
            <>
              <Spin tip={t.spinner} style={{ margin: '12px 0' }} />
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
                style={{ background: t.brand, borderColor: t.brand, marginTop: 8, height: 44 }}
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
