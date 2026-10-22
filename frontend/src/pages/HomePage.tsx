import { Button, Space } from 'antd';
import { Link } from 'react-router-dom';
import { PageHeader } from '../components/ui';

// Wave 4.2 — landing cong khai, link sang /products va /catalog.
export default function HomePage() {
  return (
    <div>
      <PageHeader
        title="Ecommerce Microservices"
        subTitle="Cua hang demo — duyet san pham, quan ly kho, theo doi don hang"
      />
      <Space wrap>
        <Link to="/products">
          <Button type="primary">Xem san pham</Button>
        </Link>
        <Link to="/catalog">
          <Button>Danh muc</Button>
        </Link>
        <Link to="/cart">
          <Button>Gio hang (mock)</Button>
        </Link>
        <Link to="/dashboard">
          <Button>Dashboard (mock)</Button>
        </Link>
      </Space>
    </div>
  );
}
