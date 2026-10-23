import React, { Suspense } from 'react';
import { Skeleton } from 'antd';
import { Navigate, createBrowserRouter } from 'react-router-dom';
import AppLayout from '../components/AppLayout';
import { RequireAuth, RequireRole } from '../components/ProtectedRoute';
import { ForbiddenPage, NotFoundPage } from '../components/ErrorPages';

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
const DashboardPage = React.lazy(() => import('../pages/DashboardPage'));
const AdminUsersPage = React.lazy(() => import('../pages/AdminUsersPage'));
const AdminRolesPage = React.lazy(() => import('../pages/AdminRolesPage'));
const PublishingPage = React.lazy(() => import('../pages/PublishingPage'));
const ReviewsPage = React.lazy(() => import('../pages/ReviewsPage'));

const fallback = <Skeleton active paragraph={{ rows: 8 }} style={{ padding: 24 }} />;

export const router = createBrowserRouter([
  { path: '/login', element: <Suspense fallback={fallback}><LoginPage /></Suspense> },
  // Chi dang nhap qua Keycloak: /register + /forgot-password redirect ve /login.
  { path: '/register', element: <Navigate to="/login" replace /> },
  { path: '/forgot-password', element: <Navigate to="/login" replace /> },
  { path: '/403', element: <ForbiddenPage /> },
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <Suspense fallback={fallback}><HomePage /></Suspense> },
      { path: 'products', element: <Suspense fallback={fallback}><ProductsPage /></Suspense> },
      { path: 'products/:id', element: <Suspense fallback={fallback}><ProductDetailPage /></Suspense> },
      { path: 'catalog', element: <Suspense fallback={fallback}><CatalogPage /></Suspense> },
      { path: 'inventory', element: <Suspense fallback={fallback}><InventoryPage /></Suspense> },
      { path: 'receipts', element: <Suspense fallback={fallback}><ReceiptsPage /></Suspense> },
      { path: 'files', element: <Suspense fallback={fallback}><FilesPage /></Suspense> },
      { path: 'publishing', element: <Suspense fallback={fallback}><PublishingPage /></Suspense> },
      { path: 'reviews', element: <Suspense fallback={fallback}><ReviewsPage /></Suspense> },
      {
        path: 'cart',
        element: <RequireAuth><Suspense fallback={fallback}><CartPage /></Suspense></RequireAuth>,
      },
      {
        path: 'checkout',
        element: (
          <RequireRole roles={['ADMIN', 'USER']}>
            <Suspense fallback={fallback}><CheckoutPage /></Suspense>
          </RequireRole>
        ),
      },
      {
        path: 'orders',
        element: <RequireAuth><Suspense fallback={fallback}><OrdersPage /></Suspense></RequireAuth>,
      },
      {
        path: 'dashboard',
        element: <RequireAuth><Suspense fallback={fallback}><DashboardPage /></Suspense></RequireAuth>,
      },
      {
        path: 'account',
        element: <RequireAuth><Suspense fallback={fallback}><ProfilePage /></Suspense></RequireAuth>,
      },
      {
        path: 'change-password',
        element: <RequireAuth><Suspense fallback={fallback}><ChangePasswordPage /></Suspense></RequireAuth>,
      },
      {
        path: 'admin',
        element: (
          <RequireRole roles={['ADMIN']}>
            <Suspense fallback={fallback}><DashboardPage /></Suspense>
          </RequireRole>
        ),
      },
      {
        path: 'admin/users',
        element: (
          <RequireRole roles={['ADMIN']}>
            <Suspense fallback={fallback}><AdminUsersPage /></Suspense>
          </RequireRole>
        ),
      },
      {
        path: 'admin/roles',
        element: (
          <RequireRole roles={['ADMIN']}>
            <Suspense fallback={fallback}><AdminRolesPage /></Suspense>
          </RequireRole>
        ),
      },
      {
        path: 'admin/reviews',
        element: (
          <RequireRole roles={['ADMIN', 'MODERATOR']}>
            <Suspense fallback={fallback}><ReviewsPage /></Suspense>
          </RequireRole>
        ),
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]);
