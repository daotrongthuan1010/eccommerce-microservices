import { Avatar, Button, Descriptions, Dropdown, Layout, Menu, Modal, Skeleton, Space, Typography } from 'antd';
import type { MenuProps } from 'antd';
import {
  AppstoreOutlined,
  AuditOutlined,
  DashboardOutlined,
  FileTextOutlined,
  HomeOutlined,
  KeyOutlined,
  LockOutlined,
  SafetyOutlined,
  SettingOutlined,
  ShoppingCartOutlined,
  ShoppingOutlined,
  SolutionOutlined,
  TeamOutlined,
  UnorderedListOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { logout } from '../features/auth/authSlice';
import { useGetMeQuery } from '../features/user/userApi';
import { useGetMyMenusQuery, type MyMenuItem } from '../features/admin/iamApi';
import { useLazyFileUrlQuery } from '../features/file/fileApi';
import { RoleTags, UserAvatar } from './ui';
import logoUrl from '../assets/logo.png';

const { Header, Sider, Content } = Layout;

// FE routes co that trong router.tsx — menu BE co path nam ngoai tap nay
// (VD /users, /admin/actions) se bi AN (gap bao cao, khong render link gay).
// /login-/register-/403 khong phai sider links nen khong co trong tap nay.
const KNOWN_SIDER_PATHS = new Set<string>([
  '/',
  '/products',
  '/catalog',
  '/inventory',
  '/receipts',
  '/files',
  '/publishing',
  '/reviews',
  '/cart',
  '/checkout',
  '/orders',
  '/dashboard',
  '/account',
  '/change-password',
  '/admin',
  '/admin/users',
  '/admin/roles',
  '/admin/menus',
  '/admin/actions',
  '/admin/permissions',
  '/admin/iam-roles',
  '/admin/groups',
  '/admin/reviews',
]);

// Chuoi icon tu DB (Menu.icon) -> @ant-design/icons. Khong map duoc -> null
// (khong icon) chu khong render sai icon.
function resolveMenuIcon(icon?: string | null): ReactNode {
  if (!icon) return null;
  const key = icon.trim().toLowerCase().replace(/[-_\s]+/g, '');
  switch (key) {
    case 'setting':
    case 'settings':
    case 'control':
      return <SettingOutlined />;
    case 'appstore':
    case 'app':
    case 'menu':
    case 'function':
      return <AppstoreOutlined />;
    case 'dashboard':
      return <DashboardOutlined />;
    case 'home':
      return <HomeOutlined />;
    case 'user':
    case 'users':
      return <UserOutlined />;
    case 'team':
    case 'group':
    case 'groups':
      return <TeamOutlined />;
    case 'key':
    case 'action':
    case 'actions':
      return <KeyOutlined />;
    case 'lock':
    case 'permission':
    case 'permissions':
      return <LockOutlined />;
    case 'safety':
    case 'shield':
    case 'role':
    case 'roles':
      return <SafetyOutlined />;
    case 'solution':
      return <SolutionOutlined />;
    case 'audit':
    case 'review':
    case 'reviews':
      return <AuditOutlined />;
    case 'file':
    case 'files':
      return <FileTextOutlined />;
    case 'shopping':
    case 'order':
    case 'orders':
    case 'product':
    case 'products':
      return <ShoppingOutlined />;
    case 'cart':
      return <ShoppingCartOutlined />;
    case 'list':
    case 'unorderedlist':
      return <UnorderedListOutlined />;
    default:
      return null;
  }
}

function sortMenus(menus: MyMenuItem[]): MyMenuItem[] {
  return [...menus].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
}

// Cay DB -> AntD Menu items: node co children hien thi -> SubMenu (label van
// la Link neu path hop le); leaf chi render khi path thuoc KNOWN_SIDER_PATHS,
// con lai AN han (khong fake link).
function buildDynamicItems(menus: MyMenuItem[]): NonNullable<MenuProps['items']> {
  const out: NonNullable<MenuProps['items']> = [];
  for (const m of sortMenus(menus)) {
    const kids = buildDynamicItems(m.children ?? []);
    const known = !!m.path && KNOWN_SIDER_PATHS.has(m.path);
    if (kids.length > 0) {
      out.push({
        key: `menu:${m.code}`,
        icon: resolveMenuIcon(m.icon),
        label: known && m.path ? <Link to={m.path}>{m.name}</Link> : <span>{m.name}</span>,
        children: kids,
      });
    } else if (known && m.path) {
      out.push({
        key: `menu:${m.code}`,
        icon: resolveMenuIcon(m.icon),
        label: <Link to={m.path}>{m.name}</Link>,
      });
    }
  }
  return out;
}

// Tim menu key tuong ung pathname hien tai de highlight + mo SubMenu cha.
function findMenuKey(menus: MyMenuItem[], pathname: string): string | null {
  for (const m of menus) {
    if (m.path && pathname === m.path) return `menu:${m.code}`;
    const kid = findMenuKey(m.children ?? [], pathname);
    if (kid) return kid;
  }
  return null;
}

function collectOpenKeys(menus: MyMenuItem[], pathname: string): string[] {
  const acc: string[] = [];
  for (const m of menus) {
    const kids = m.children ?? [];
    if (kids.length === 0) continue;
    const sub = collectOpenKeys(kids, pathname);
    const selfHit = !!m.path && (pathname === m.path || pathname.startsWith(`${m.path}/`));
    const kidHit = kids.some(
      (k) => !!k.path && (pathname === k.path || pathname.startsWith(`${k.path}/`)),
    );
    if (selfHit || kidHit || sub.length > 0) acc.push(`menu:${m.code}`, ...sub);
  }
  return acc;
}

// AppLayout: AntD Layout + menu loc theo role (FE_AUTH_GATEWAY §6).
// Header phai: avatar that (picture tu GET /api/auth/me) + dropdown tai khoan
// (Ho so / Doi mat khau / Dang xuat) + modal xem nhanh thong tin tai khoan.
export default function AppLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const username = useAppSelector((s) => s.auth.username);
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
  // getMyMenus da unwrap {menus,total} + map de quy children/actions ve
  // MyMenuItem[] trong transformResponse; guard Array.isArray de khong crash
  // neu BE doi shape.
  const myMenus: MyMenuItem[] = Array.isArray(myMenusQuery.data) ? myMenusQuery.data : [];

  // Sider dong hoan toan tu GET /auth/me/menus (DB menu tree + loc quyen VIEW
  // tu group; sysadmin thay tat ca — ref MeMenuController). Link shop co dinh
  // (Cart/Account/...) luon giu vi DB menus chi chua admin tree; rieng admin
  // section KHONG con fallback menu tinh — loading hien skeleton, loi hien
  // message, rong thi an han (khong fake menus).
  const userLinks: NonNullable<MenuProps['items']> = [
    { key: '/', label: <Link to="/">Trang chủ</Link> },
    { key: '/products', label: <Link to="/products">Sản phẩm</Link> },
    { key: '/catalog', label: <Link to="/catalog">Danh mục</Link> },
    { key: '/inventory', label: <Link to="/inventory">Tồn kho</Link> },
    { key: '/files', label: <Link to="/files">File</Link> },
    { key: '/cart', label: <Link to="/cart">Giỏ hàng</Link> },
    { key: '/account', label: <Link to="/account">Tài khoản</Link> },
  ];

  const adminSection: NonNullable<MenuProps['items']> = myMenusQuery.isLoading
    ? [
        {
          key: '__menus-loading',
          disabled: true,
          label: <Skeleton active paragraph={{ rows: 4 }} title={false} />,
        },
      ]
    : myMenusQuery.isError
      ? [
          {
            key: '__menus-error',
            disabled: true,
            danger: true,
            label: (
              <Typography.Text type="danger">
                Không tải được menu phân quyền. Kiểm tra lại kết nối rồi tải lại trang.
              </Typography.Text>
            ),
          },
        ]
      : buildDynamicItems(myMenus);

  const items: MenuProps['items'] = [...userLinks, ...adminSection];
  const activeMenuKey = findMenuKey(myMenus, location.pathname) ?? location.pathname;

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider breakpoint="lg" collapsedWidth="0">
        <div style={{ padding: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
          <img src={logoUrl} alt="Ecommerce logo" style={{ height: 32 }} />
          <span style={{ color: '#fff', fontWeight: 600 }}>Ecommerce</span>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[activeMenuKey]}
          defaultOpenKeys={collectOpenKeys(myMenus, location.pathname)}
          items={items}
        />
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
