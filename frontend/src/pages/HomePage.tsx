import { Carousel, Typography } from 'antd';
import { Link } from 'react-router-dom';
import { useBrowseProductsQuery } from '../features/marketplace/browseApi';
import { useGetCategoriesQuery } from '../features/catalog/catalogApi';
import { ProductCardGrid } from '../components/marketplace/ProductCardGrid';

// Marketplace HomePage (2026-11-05 fe-marketplace D2, spec customer/01):
// banner + danh muc + luoi san pham moi (browse API cong khai, guest OK).
// Banner la anh tam (chua co API collections) — P2 se wire /api/v1/home/collections.
export default function HomePage() {
  const newestQ = useBrowseProductsQuery({ size: 20, sort: 'newest' });
  const categoriesQ = useGetCategoriesQuery();
  const roots = categoriesQ.data?.data ?? [];
  const products = newestQ.data?.data?.content ?? [];

  return (
    <div>
      <Carousel autoplay style={{ marginBottom: 16 }}>
        <div>
          <div
            style={{
              height: 200,
              background: 'linear-gradient(90deg, #e8543f 0%, #f7921e 100%)',
              color: '#fff',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 8,
            }}
          >
            <Typography.Title level={2} style={{ color: '#fff', margin: 0 }}>
              Săn deal mỗi ngày
            </Typography.Title>
            <Typography.Text style={{ color: '#fff', fontSize: 16 }}>
              Hàng ngàn sản phẩm đang chờ bạn khám phá
            </Typography.Text>
          </div>
        </div>
      </Carousel>

      {roots.length > 0 && (
        <div style={{ marginBottom: 20 }}>
          <Typography.Title level={4} style={{ marginTop: 0 }}>
            Danh mục
          </Typography.Title>
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
            {roots.slice(0, 10).map((c) => (
              <Link
                key={c.id}
                to={`/products?categoryId=${encodeURIComponent(String(c.id))}`}
                style={{
                  padding: '6px 14px',
                  background: '#fff',
                  border: '1px solid #f0f0f0',
                  borderRadius: 16,
                  color: '#555',
                }}
              >
                {String(c.name)}
              </Link>
            ))}
          </div>
        </div>
      )}

      <Typography.Title level={4}>Sản phẩm mới</Typography.Title>
      <ProductCardGrid products={products} loading={newestQ.isLoading} />
    </div>
  );
}
