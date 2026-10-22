import { Button, Result } from 'antd';
import { Link } from 'react-router-dom';

export function ForbiddenPage() {
  return (
    <Result
      status="403"
      title="403"
      subTitle="Ban khong co quyen truy cap trang nay."
      extra={
        <Link to="/">
          <Button type="primary">Ve trang chu</Button>
        </Link>
      }
    />
  );
}

export function NotFoundPage() {
  return (
    <Result
      status="404"
      title="404"
      subTitle="Trang khong ton tai."
      extra={
        <Link to="/">
          <Button type="primary">Ve trang chu</Button>
        </Link>
      }
    />
  );
}
