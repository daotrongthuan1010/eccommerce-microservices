import { Card, Empty, Rate, Skeleton, Typography } from 'antd';
import { Link } from 'react-router-dom';
import type { ProductCard } from '../../features/marketplace/browseApi';
import type { SearchProductHit } from '../../features/search/searchApi';

// Marketplace product card grid (2026-11-05 fe-marketplace D7): card kieu
// Shopee/Tiki — anh, ten, gia VND. Responsive: 2 cot o 360px -> 5 cot desktop.

export function formatVnd(v: number): string {
  return `${v.toLocaleString('vi-VN')} ₫`;
}

/** Card data dong nhat: browse (id) hoac ES search hit (productId). */
export type CardLike = {
  id: string;
  name: string;
  priceFrom: number;
  primaryImageKey?: string | null;
  sellableSkuCount?: number;
};

export function toCard(p: ProductCard | SearchProductHit): CardLike {
  const id = 'id' in p && p.id ? p.id : (p as SearchProductHit).productId;
  return {
    id: String(id),
    name: p.name,
    priceFrom: p.priceFrom,
    primaryImageKey: p.primaryImageKey,
    sellableSkuCount: p.sellableSkuCount,
  };
}

function ProductCardItem({ p }: { p: CardLike }) {
  return (
    <Link to={`/products/${p.id}`}>
      <Card
        hoverable
        size="small"
        styles={{ body: { padding: 10 } }}
        cover={
          p.primaryImageKey ? (
            <div
              style={{
                height: 180,
                background: '#f5f5f5 center/cover no-repeat',
                // Anh qua file-service: resolve URL o lop page (presign) —
                // tam dung placeholder khi chua co anh.
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#bbb',
                fontSize: 28,
              }}
            >
              🛍️
            </div>
          ) : (
            <div
              style={{
                height: 180,
                background: '#f5f5f5',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#bbb',
                fontSize: 28,
              }}
            >
              🛍️
            </div>
          )
        }
      >
        <Typography.Paragraph
          ellipsis={{ rows: 2 }}
          style={{ marginBottom: 6, minHeight: 42, fontSize: 13 }}
        >
          {p.name}
        </Typography.Paragraph>
        <Typography.Text strong style={{ color: '#e8543f', fontSize: 16 }}>
          {formatVnd(p.priceFrom)}
        </Typography.Text>
        {(p.sellableSkuCount ?? 0) > 1 && (
          <Typography.Text type="secondary" style={{ display: 'block', fontSize: 12 }}>
            {p.sellableSkuCount} phiên bản
          </Typography.Text>
        )}
      </Card>
    </Link>
  );
}

export function ProductCardGrid({
  products,
  loading,
  emptyText = 'Chưa có sản phẩm nào',
}: {
  products: (ProductCard | SearchProductHit)[];
  loading?: boolean;
  emptyText?: string;
}) {
  if (loading) {
    return (
      <div className="marketplace-grid">
        {Array.from({ length: 10 }).map((_, i) => (
          <Card key={i} size="small">
            <Skeleton.Image active style={{ width: '100%', height: 180 }} />
            <Skeleton active paragraph={{ rows: 1 }} title={{ width: '60%' }} />
          </Card>
        ))}
      </div>
    );
  }
  if (products.length === 0) {
    return <Empty description={emptyText} style={{ padding: 48 }} />;
  }
  return (
    <div className="marketplace-grid">
      {products.map((raw) => (
        <ProductCardItem key={toCard(raw).id} p={toCard(raw)} />
      ))}
    </div>
  );
}

// Rating summary cho PDP (doc review cong khai).
export function RatingSummary({ average, count }: { average?: number; count?: number }) {
  return (
    <SpaceLike>
      <Rate disabled allowHalf value={average ?? 0} style={{ fontSize: 14 }} />
      {count !== undefined && (
        <Typography.Text type="secondary">({count} đánh giá)</Typography.Text>
      )}
    </SpaceLike>
  );
}

function SpaceLike({ children }: { children: React.ReactNode }) {
  return <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>{children}</span>;
}
