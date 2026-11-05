import React, { Suspense } from 'react';
import { Skeleton } from 'antd';
import { Navigate, createBrowserRouter } from 'react-router-dom';
import AppLayout from '../components/AppLayout';
import MarketplaceLayout from '../components/MarketplaceLayout';
import { RequireAuth, RequireRole } from '../components/ProtectedRoute';
import { ForbiddenPage, NotFoundPage } from '../components/ErrorPages';

// 2026-11-05 fe-marketplace: tach 2 khu vuc:
// - Shopper (MarketplaceLayout, kieu Shopee/Tiki): home, search/list, PDP,
//   cart, checkout, orders, payment result — browse KHONG can login (D3);
//   cart/checkout/orders can login (RequireAuth/RequireRole).
// - Backoffice (AppLayout, giu nguyen): catalog, inventory, receipts, files,
//   publishing, reviews moderation, admin/*.
// FE_AUTH_GATEWAY §6 — ma tran trang × quyen. Lazy + Suspense, SPA fallback
// do nginx try_files.
const LoginPage = React.lazy(() => import('../pages/LoginPage'));
const HomePage = React.lazy(() => import('../pages/HomePage'));
const ProductsPage = React.lazy(() => import('../pages/ProductsPage'));
const ProductDetailPage = React.lazy(() => import('../pages/ProductDetailPage'));
const ChangePasswordPage = React.lazy(() => import('../pages/ChangePasswordPage'));
const ProfilePage = React.lazy(() => import('../pages/ProfilePage'));
const CatalogPage = React.lazy(() => import('../pages/CatalogPage'));
const InventoryPage = React.lazy(() => import('../pages/InventoryPage'));
const ReceiptsPage = React.lazy(() => import('../pages/ReceiptsPage'));
const FilesPage = React.lazy(() => import('../pages/FilesPage'));
const CartPage = React.lazy(() => import('../pages/CartPage'));
const CheckoutPage = React.lazy(() => import('../pages/CheckoutPage'));
const OrdersPage = React.lazy(() => import('../pages/OrdersPage'));
const OrderDetailPage = React.lazy(() => import('../pages/OrderDetailPage'));
const PaymentResultPage = React.lazy(() => import('../pages/PaymentResultPage'));
const DashboardPage = React.lazy(() => import('../pages/DashboardPage'));
const AdminUsersPage = React.lazy(() => import('../pages/AdminUsersPage'));
const AdminRolesPage = React.lazy(() => import('../pages/AdminRolesPage'));
const AdminMenusPage = React.lazy(() => import('../pages/AdminMenusPage'));
const AdminPermissionsPage = React.lazy(() => import('../pages/AdminPermissionsPage'));
const AdminIamRolesPage = React.lazy(() => import('../pages/AdminIamRolesPage'));
const AdminGroupsPage = React.lazy(() => import('../pages/AdminGroupsPage'));
const RegisterPage = React.lazy(() => import('../pages/RegisterPage'));
const PublishingPage = React.lazy(() => import('../pages/PublishingPage'));
const ReviewsPage = React.lazy(() => import('../pages/ReviewsPage'));
const StaffUsersPage = React.lazy(() => import('../pages/StaffUsersPage'));

const fallback = <Skeleton active paragraph={{ rows: 8 }} style={{ padding: 24 }} />;

const wrap = (el: React.ReactNode) => <Suspense fallback={fallback}>{el}</Suspense>;

export const router = createBrowserRouter([
  { path: '/login', element: wrap(<LoginPage />) },
  // Login quan tri rieng (tong navy, landing /admin sau SSO) — tach khoi
  // login shopper (tong cam, landing /) 2026-11-05.
  { path: '/admin/login', element: wrap(<LoginPage variant="admin" />) },
  // Marketplace (buyer/seller kieu Shopee/Tiki) tu dang ky + verify mail
  // qua POST /api/auth/register; staff do cap tren tao o /admin/users.
  { path: '/register', element: wrap(<RegisterPage />) },
  { path: '/forgot-password', element: <Navigate to="/login" replace /> },
  { path: '/403', element: <ForbiddenPage /> },

  // ---- Khu vuc shopper: MarketplaceLayout (browse guest OK, D3/D7) ----
  {
    path: '/',
    element: <MarketplaceLayout />,
    children: [
      { index: true, element: wrap(<HomePage />) },
      { path: 'products', element: wrap(<ProductsPage />) },
      { path: 'products/:id', element: wrap(<ProductDetailPage />) },
      {
        path: 'cart',
        element: <RequireAuth>{wrap(<CartPage />)}</RequireAuth>,
      },
      {
        path: 'checkout',
        element: (
          <RequireRole roles={['ADMIN', 'USER']}>{wrap(<CheckoutPage />)}</RequireRole>
        ),
      },
      {
        path: 'orders',
        element: <RequireAuth>{wrap(<OrdersPage />)}</RequireAuth>,
      },
      {
        path: 'orders/:orderId',
        element: <RequireAuth>{wrap(<OrderDetailPage />)}</RequireAuth>,
      },
      {
        path: 'payment/result',
        element: <RequireAuth>{wrap(<PaymentResultPage />)}</RequireAuth>,
      },
      {
        path: 'account',
        element: <RequireAuth>{wrap(<ProfilePage />)}</RequireAuth>,
      },
      {
        path: 'change-password',
        element: <RequireAuth>{wrap(<ChangePasswordPage />)}</RequireAuth>,
      },
      {
        path: 'dashboard',
        element: <RequireAuth>{wrap(<DashboardPage />)}</RequireAuth>,
      },
    ],
  },

  // ---- Khu vuc backoffice: AppLayout (giu nguyen nhu cu) ----
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { path: 'catalog', element: wrap(<CatalogPage />) },
      { path: 'inventory', element: wrap(<InventoryPage />) },
      { path: 'receipts', element: wrap(<ReceiptsPage />) },
      { path: 'files', element: wrap(<FilesPage />) },
      { path: 'publishing', element: wrap(<PublishingPage />) },
      { path: 'reviews', element: wrap(<ReviewsPage />) },
      {
        path: 'admin',
        element: (
          <RequireRole roles={['ADMIN']}>{wrap(<DashboardPage />)}</RequireRole>
        ),
      },
      {
        path: 'admin/users',
        element: (
          <RequireRole roles={['ADMIN']}>{wrap(<AdminUsersPage />)}</RequireRole>
        ),
      },
      {
        path: 'admin/roles',
        element: (
          <RequireRole roles={['ADMIN']}>{wrap(<AdminRolesPage />)}</RequireRole>
        ),
      },
      {
        path: 'admin/menus',
        element: (
          <RequireRole roles={['ADMIN']}>{wrap(<AdminMenusPage />)}</RequireRole>
        ),
      },
      {
        path: 'admin/actions',
        element: (
          <RequireRole roles={['ADMIN']}>{wrap(<AdminMenusPage />)}</RequireRole>
        ),
      },
      {
        path: 'admin/permissions',
        element: (
          <RequireRole roles={['ADMIN']}>{wrap(<AdminPermissionsPage />)}</RequireRole>
        ),
      },
      {
        path: 'admin/iam-roles',
        element: (
          <RequireRole roles={['ADMIN']}>{wrap(<AdminIamRolesPage />)}</RequireRole>
        ),
      },
      {
        path: 'admin/groups',
        element: (
          <RequireRole roles={['ADMIN']}>{wrap(<AdminGroupsPage />)}</RequireRole>
        ),
      },
      {
        path: 'admin/reviews',
        element: (
          <RequireRole roles={['ADMIN', 'MODERATOR']}>{wrap(<ReviewsPage />)}</RequireRole>
        ),
      },
      {
        // Cap tren quan ly cap duoi truc tiep (chuoi doanh nghiep) — khac
        // /admin/users (ADMIN toan he thong). BE: StaffUserController.
        path: 'staff/users',
        element: (
          <RequireRole roles={['ADMIN', 'MODERATOR']}>{wrap(<StaffUsersPage />)}</RequireRole>
        ),
      },
    ],
  },

  // 404 that o ngoai ca 2 layout de khong an theo layout.
  { path: '*', element: <NotFoundPage /> },
]);
