import { Button, Result } from 'antd';
import { Link, useLocation } from 'react-router-dom';

// 403 phan theo khu vuc: dang o /admin|/staff (backoffice) → nút ve
// /admin/login (login quan tri, tong navy); con lai → trang chu marketplace.
// Truoc day chi co "Về trang chủ" — user quan tri bi 403 roi bi hut ve
// trang mua hang (bug bao 2026-11-06).
export function ForbiddenPage() {
  const { pathname } = useLocation();
  const inBackoffice = pathname.startsWith('/admin') || pathname.startsWith('/staff');
  return (
    <Result
      status="403"
      title="403"
      subTitle={
        inBackoffice
          ? 'Bạn không có quyền truy cập khu vực quản trị. Cần đăng nhập bằng tài khoản ADMIN.'
          : 'Bạn không có quyền truy cập trang này.'
      }
      extra={
        inBackoffice ? (
          <Link to="/admin/login">
            <Button type="primary">Đăng nhập quản trị</Button>
          </Link>
        ) : (
          <Link to="/">
            <Button type="primary">Về trang chủ</Button>
          </Link>
        )
      }
    />
  );
}

export function NotFoundPage() {
  return (
    <Result
      status="404"
      title="404"
      subTitle="Trang không tồn tại."
      extra={
        <Link to="/">
          <Button type="primary">Về trang chủ</Button>
        </Link>
      }
    />
  );
}
