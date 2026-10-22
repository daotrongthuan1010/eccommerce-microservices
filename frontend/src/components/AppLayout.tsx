import { Layout, Menu } from 'antd';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { logout } from '../features/auth/authSlice';
import logoUrl from '../assets/logo.png';

const { Header, Sider, Content } = Layout;

// AppLayout: AntD Layout + menu loc theo role (FE_AUTH_GATEWAY §6).
export default function AppLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const roles = useAppSelector((s) => s.auth.roles);
  const username = useAppSelector((s) => s.auth.username);
  const isAdmin = roles.includes('ADMIN');

  const items = [
    { key: '/', label: <Link to="/">Trang chu</Link> },
    { key: '/products', label: <Link to="/products">San pham</Link> },
    { key: '/catalog', label: <Link to="/catalog">Danh muc</Link> },
    { key: '/inventory', label: <Link to="/inventory">Ton kho</Link> },
    { key: '/files', label: <Link to="/files">File</Link> },
    { key: '/cart', label: <Link to="/cart">Gio hang (mock)</Link> },
    { key: '/account', label: <Link to="/account">Tai khoan</Link> },
    ...(isAdmin
      ? [
          { key: '/admin', label: <Link to="/admin">Quan tri</Link> },
          { key: '/admin/reviews', label: <Link to="/admin/reviews">Duyet review</Link> },
        ]
      : []),
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider breakpoint="lg" collapsedWidth="0">
        <div style={{ padding: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
          <img src={logoUrl} alt="Ecommerce logo" style={{ height: 32 }} />
          <span style={{ color: '#fff', fontWeight: 600 }}>Ecommerce</span>
        </div>
        <Menu theme="dark" mode="inline" selectedKeys={[location.pathname]} items={items} />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', display: 'flex', justifyContent: 'flex-end', gap: 12, alignItems: 'center' }}>
          {username ? (
            <>
              <span>{username}</span>
              <a
                onClick={() => {
                  void dispatch(logout()).finally(() => navigate('/login'));
                }}
              >
                Dang xuat
              </a>
            </>
          ) : (
            <Link to="/login">Dang nhap</Link>
          )}
        </Header>
        <Content style={{ margin: 16 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
