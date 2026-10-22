import { Button, Space } from 'antd';
import { Link } from 'react-router-dom';
import { PageHeader } from '../components/ui';

// Wave 4.2 — landing cong khai, link sang /products va /catalog.
export default function HomePage() {
  return (
    <div>
      <PageHeader
        title="Ecommerce Microservices"
        subTitle="Cửa hàng demo — duyệt sản phẩm, quản lý kho, theo dõi đơn hàng"
      />
      <Space wrap>
        <Link to="/products">
          <Button type="primary">Xem sản phẩm</Button>
        </Link>
        <Link to="/catalog">
          <Button>Danh mục</Button>
        </Link>
        <Link to="/cart">
          <Button>Giỏ hàng (mock)</Button>
        </Link>
        <Link to="/dashboard">
          <Button>Dashboard (mock)</Button>
        </Link>
      </Space>
    </div>
  );
}
