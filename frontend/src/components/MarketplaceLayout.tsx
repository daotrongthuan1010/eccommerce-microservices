import { Badge, Button, ConfigProvider, Dropdown, Input, Layout, Space, Typography } from 'antd';
import { App as AntApp } from 'antd';
import {
  HeartOutlined,
  LoginOutlined,
  OrderedListOutlined,
  SearchOutlined,
  ShoppingCartOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useEffect, useState, type ReactNode } from 'react';
import { Link, Outlet, useNavigate, useSearchParams } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { logout } from '../features/auth/authSlice';
import {
  guestCartCount,
  mergeDone,
  mergeFailed,
  mergeStarted,
} from '../features/marketplace/guestCartSlice';
import { useAddToCartMutation } from '../features/order/orderApi';
import { useGetCategoriesQuery } from '../features/catalog/catalogApi';
import { marketplaceThemeConfig } from '../app/theme';

const { Header, Content, Footer } = Layout;

// MarketplaceLayout (2026-11-05 fe-marketplace D7): shell rieng cho shopper
// kieu Shopee/Tiki — header (logo + search trung tam + gio hang badge + login/
// avatar), nav danh muc ngang, footer. Admin/backoffice giu nguyen AppLayout.
// Theme do cam ap dung o ConfigProvider (app/main.tsx).

export default function MarketplaceLayout() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { message } = AntApp.useApp();
  const username = useAppSelector((s) => s.auth.username);
  const guestItems = useAppSelector((s) => s.guestCart.items);
  const merging = useAppSelector((s) => s.guestCart.merging);
  const [searchParams] = useSearchParams();
  const [keyword, setKeyword] = useState(searchParams.get('q') ?? '');

  // Merge guest cart vao cart server ngay khi co token (D5): tung item
  // POST /api/order/cart/items; xong clear local. Chay dung 1 lan khi username
  // xuat hien (co the login vua xong).
  const [addToCart] = useAddToCartMutation();
  const mergedForRef = useState(() => new Set<string>())[0];
  useEffect(() => {
    if (!username || merging || guestItems.length === 0) return;
    if (mergedForRef.has(username)) return;
    mergedForRef.add(username);
    void (async () => {
      dispatch(mergeStarted());
      try {
        for (const item of guestItems) {
          await addToCart({
            skuId: item.skuId,
            productName: item.productName,
            unitPrice: item.unitPrice,
            quantity: item.quantity,
          }).unwrap();
        }
        dispatch(mergeDone());
        message.success('Đã đồng bộ giỏ hàng vào tài khoản của bạn');
      } catch (e) {
        dispatch(mergeFailed(e instanceof Error ? e.message : 'Đồng bộ giỏ hàng thất bại'));
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [username, merging, guestItems.length]);

  // Nav danh muc ngang: categories root cong khai (guest xem duoc).
  const categoriesQ = useGetCategoriesQuery(undefined, { skip: false });
  const roots = (categoriesQ.data?.data ?? []).slice(0, 8);

  const onSearch = () => {
    const q = keyword.trim();
    navigate(q ? `/products?q=${encodeURIComponent(q)}` : '/products');
  };

  const doLogout = () => {
    void dispatch(logout()).finally(() => navigate('/'));
  };

  const count = guestCartCount(guestItems);

  return (
    <Layout style={{ minHeight: '100vh', background: '#fff' }}>
      <Header
        style={{
          background: 'linear-gradient(180deg, #fff 0%, #fff 100%)',
          borderBottom: '1px solid #f0f0f0',
          padding: '0 clamp(12px, 4vw, 48px)',
          display: 'flex',
          alignItems: 'center',
          gap: 16,
          height: 72,
          position: 'sticky',
          top: 0,
          zIndex: 100,
        }}
      >
        <Link to="/" style={{ display: 'flex', alignItems: 'center', flexShrink: 0 }}>
          <span
            style={{
              color: '#e8543f',
              fontWeight: 800,
              fontSize: 20,
              letterSpacing: 0.5,
              whiteSpace: 'nowrap',
            }}
          >
            Ecommerce
          </span>
        </Link>
        <Input
          size="large"
          placeholder="Tìm sản phẩm, danh mục..."
          prefix={<SearchOutlined style={{ color: '#bbb' }} />}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onPressEnter={onSearch}
          style={{ maxWidth: 560, flex: 1 }}
        />
        <Space size={12} style={{ marginLeft: 'auto', flexShrink: 0 }}>
          {username ? (
            <Dropdown
              trigger={['click']}
              menu={{
                items: [
                  { key: 'orders', label: <Link to="/orders">Đơn hàng của tôi</Link> },
                  { key: 'account', label: <Link to="/account">Tài khoản</Link> },
                  { type: 'divider' },
                  { key: 'admin', label: <Link to="/admin">Khu vực quản trị</Link> },
                  { key: 'logout', danger: true, label: 'Đăng xuất', onClick: doLogout },
                ],
              }}
            >
              <Space style={{ cursor: 'pointer' }}>
                <UserOutlined style={{ fontSize: 18 }} />
                <Typography.Text style={{ fontWeight: 500 }}>{username}</Typography.Text>
              </Space>
            </Dropdown>
          ) : (
            <Link to="/login">
              <Button icon={<LoginOutlined />}>Đăng nhập</Button>
            </Link>
          )}
          <Link to="/orders" className="marketplace-icon-link">
            <OrderedListOutlined style={{ fontSize: 18 }} />
          </Link>
          <span style={{ color: '#ddd' }}>|</span>
          <Link to="/cart">
            <Badge count={count} size="small" color="#e8543f">
              <ShoppingCartOutlined style={{ fontSize: 22, color: '#333' }} />
            </Badge>
          </Link>
        </Space>
      </Header>

      {roots.length > 0 && (
        <div
          style={{
            background: '#fff',
            borderBottom: '1px solid #f0f0f0',
            padding: '8px clamp(12px, 4vw, 48px)',
            display: 'flex',
            gap: 20,
            overflowX: 'auto',
            whiteSpace: 'nowrap',
          }}
        >
          {roots.map((c) => (
            <Link
              key={c.id}
              to={`/products?categoryId=${encodeURIComponent(String(c.id))}`}
              style={{ color: '#555', fontWeight: 500 }}
            >
              {String(c.name)}
            </Link>
          ))}
        </div>
      )}

      <Content style={{ padding: '16px clamp(12px, 4vw, 48px)', maxWidth: 1280, margin: '0 auto', width: '100%' }}>
        <ConfigProvider theme={marketplaceThemeConfig}>{<Outlet /> as ReactNode}</ConfigProvider>
      </Content>

      <Footer style={{ background: '#fafafa', textAlign: 'center', color: '#888' }}>
        <Space split={<span style={{ color: '#ccc' }}>·</span>} wrap>
          <span>Ecommerce Microservices — demo học tập</span>
          <Link to="/products">Sản phẩm</Link>
          <Link to="/orders">Đơn hàng</Link>
          <HeartOutlined />
        </Space>
      </Footer>
    </Layout>
  );
}
