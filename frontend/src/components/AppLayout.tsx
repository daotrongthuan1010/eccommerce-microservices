import { Avatar, Button, Descriptions, Dropdown, Layout, Menu, Modal, Space, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { logout } from '../features/auth/authSlice';
import { useGetMeQuery } from '../features/user/userApi';
import { useGetMyMenusQuery, type MyMenuItem } from '../features/admin/iamApi';
import { useLazyFileUrlQuery } from '../features/file/fileApi';
import { RoleTags, UserAvatar } from './ui';
import logoUrl from '../assets/logo.png';

const { Header, Sider, Content } = Layout;

// AppLayout: AntD Layout + menu loc theo role (FE_AUTH_GATEWAY §6).
// Header phai: avatar that (picture tu GET /api/auth/me) + dropdown tai khoan
// (Ho so / Doi mat khau / Dang xuat) + modal xem nhanh thong tin tai khoan.
export default function AppLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const roles = useAppSelector((s) => s.auth.roles);
  const username = useAppSelector((s) => s.auth.username);
  const isAdmin = roles.includes('ADMIN');
  const [accountOpen, setAccountOpen] = useState(false);

  // Header hien avatar that ngay khi login (khong doi mo modal);
  // RTK Query cache chung voi ProfilePage nen khong goi thua.
  const meQuery = useGetMeQuery(undefined, { skip: !username });
  const me = meQuery.data;
  const [headerAvatar, setHeaderAvatar] = useState<string | null>(null);
  const [triggerFileUrl] = useLazyFileUrlQuery();

  // Resolve file-key picture → URL tuoi tho ngan, giong ProfilePage.
  useEffect(() => {
    let cancelled = false;
    setHeaderAvatar(null);
    if (!me?.picture) return;
    triggerFileUrl({ key: me.picture })
      .unwrap()
      .then((res) => {
        if (!cancelled) setHeaderAvatar(res.data?.url ?? null);
      })
      .catch(() => {
        if (!cancelled) setHeaderAvatar(null);
      });
    return () => {
      cancelled = true;
    };
  }, [me?.picture, triggerFileUrl]);

  const doLogout = () => {
    void dispatch(logout()).finally(() => navigate('/login'));
  };

  const myMenusQuery = useGetMyMenusQuery(undefined, { skip: !username });
  // getMyMenus da unwrap {menus,total} + map actions ve string[] trong
  // transformResponse; guard Array.isArray de khong crash neu BE doi shape.
  const myMenus: MyMenuItem[] = Array.isArray(myMenusQuery.data) ? myMenusQuery.data : [];
  const dynamicItems = myMenus
    .filter((m: MyMenuItem) => !!m.path)
    .map((m: MyMenuItem) => ({
      key: m.path as string,
      label: <Link to={m.path as string}>{m.name}</Link>,
    }));
  // Sider dong theo GET /auth/me/menus (merge quyen tu group). Chi thay THE
  // admin section bang menu dong; link nguoi dung (Cart/Account/Orders...)
  // luon giu — khong thay toan bo nav. Khi BE chua co/tra rong thi fallback
  // menu tinh cu (loading/error degradation chu khong phai steady-state).
  const useDynamicSider =
    !!username && !myMenusQuery.isLoading && !myMenusQuery.isError && dynamicItems.length > 0;

  const userLinks = [
    { key: '/', label: <Link to="/">Trang chủ</Link> },
    { key: '/products', label: <Link to="/products">Sản phẩm</Link> },
    { key: '/catalog', label: <Link to="/catalog">Danh mục</Link> },
    { key: '/inventory', label: <Link to="/inventory">Tồn kho</Link> },
    { key: '/files', label: <Link to="/files">File</Link> },
    { key: '/cart', label: <Link to="/cart">Giỏ hàng</Link> },
    { key: '/account', label: <Link to="/account">Tài khoản</Link> },
  ];
  const adminFallback = isAdmin
    ? [
        { key: '/admin', label: <Link to="/admin">Quản trị</Link> },
        { key: '/admin/users', label: <Link to="/admin/users">Người dùng (staff)</Link> },
        { key: '/admin/roles', label: <Link to="/admin/roles">Vai trò (Keycloak)</Link> },
        { key: '/admin/reviews', label: <Link to="/admin/reviews">Duyệt review</Link> },
        { key: '/admin/menus', label: <Link to="/admin/menus">Menus & Actions</Link> },
        { key: '/admin/permissions', label: <Link to="/admin/permissions">Phân quyền</Link> },
        { key: '/admin/iam-roles', label: <Link to="/admin/iam-roles">Vai trò (DB)</Link> },
        { key: '/admin/groups', label: <Link to="/admin/groups">Nhóm người dùng</Link> },
      ]
    : [];
  const items = [...userLinks, ...(useDynamicSider ? dynamicItems : adminFallback)];

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
        <Header
          style={{
            background: '#fff',
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center',
            padding: '0 24px',
            boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
          }}
        >
          {username ? (
            <Dropdown
              trigger={['click']}
              menu={{
                items: [
                  {
                    key: 'quickview',
                    label: 'Thông tin tài khoản',
                    onClick: () => setAccountOpen(true),
                  },
                  {
                    key: 'account',
                    label: <Link to="/account">Hồ sơ của tôi</Link>,
                  },
                  {
                    key: 'password',
                    label: <Link to="/change-password">Đổi mật khẩu</Link>,
                  },
                  { type: 'divider' },
                  {
                    key: 'logout',
                    danger: true,
                    label: 'Đăng xuất',
                    onClick: doLogout,
                  },
                ],
              }}
            >
              <Space style={{ cursor: 'pointer' }} size={10}>
                <UserAvatar name={username} picture={headerAvatar} />
                <span style={{ fontWeight: 500 }}>{username}</span>
              </Space>
            </Dropdown>
          ) : (
            <Link to="/login">Đăng nhập</Link>
          )}
        </Header>
        <Content style={{ margin: 16 }}>
          <Outlet />
        </Content>
      </Layout>

      <Modal
        title="Tài khoản của tôi"
        open={accountOpen}
        onCancel={() => setAccountOpen(false)}
        footer={
          <Space>
            <Link to="/account" onClick={() => setAccountOpen(false)}>
              <Button type="primary">Xem hồ sơ đầy đủ</Button>
            </Link>
            <Button onClick={() => setAccountOpen(false)}>Đóng</Button>
          </Space>
        }
      >
        {meQuery.isLoading ? (
          <Space>
            <Avatar size={48} />
            <Typography.Text type="secondary">Đang tải...</Typography.Text>
          </Space>
        ) : me ? (
          <Space align="start" size={16}>
            <UserAvatar name={me.username} picture={headerAvatar} size={48} />
            <Descriptions column={1} size="small">
              <Descriptions.Item label="Tên đăng nhập">{me.username}</Descriptions.Item>
              <Descriptions.Item label="Email">{me.email ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="Vai trò">
                <RoleTags roles={me.roles ?? []} admin={me.admin} />
              </Descriptions.Item>
            </Descriptions>
          </Space>
        ) : (
          <Typography.Text type="secondary">Không tải được thông tin tài khoản.</Typography.Text>
        )}
      </Modal>
    </Layout>
  );
}
