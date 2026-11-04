import { Input, Pagination, Segmented, Select, Space, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useBrowseProductsQuery } from '../features/marketplace/browseApi';
import { useGetCategoriesQuery } from '../features/catalog/catalogApi';
import { ProductCardGrid } from '../components/marketplace/ProductCardGrid';
import { ApiErrorAlert } from '../components/ui';

// Marketplace ProductsPage (2026-11-05 fe-marketplace D2, spec customer/02):
// tim kiem + loc danh muc + sort + paging — kieu trang search Shopee/Tiki.
// Query nam tren URL (?q=&categoryId=&sort=&page=) de share duoc link.
const PAGE_SIZE = 20;

export function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [keywordInput, setKeywordInput] = useState(searchParams.get('q') ?? '');

  const q = searchParams.get('q') ?? undefined;
  const categoryId = searchParams.get('categoryId') ?? undefined;
  const sort = (searchParams.get('sort') ?? 'newest') as 'newest' | 'price_asc' | 'price_desc';
  const page = Math.max(0, Number(searchParams.get('page') ?? 0));

  const browseQ = useBrowseProductsQuery({
    page,
    size: PAGE_SIZE,
    keyword: q,
    categoryId,
    sort,
  });
  const categoriesQ = useGetCategoriesQuery();

  // Dong bo o search header (MarketplaceLayout) khi user sua URL tay.
  useEffect(() => {
    setKeywordInput(q ?? '');
  }, [q]);

  const products = browseQ.data?.data?.content ?? [];
  const total = browseQ.data?.data?.totalElements ?? 0;

  const setParam = (key: string, value?: string) => {
    const next = new URLSearchParams(searchParams);
    if (value) next.set(key, value);
    else next.delete(key);
    if (key !== 'page') next.delete('page'); // doi filter ve trang 1
    setSearchParams(next, { replace: true });
  };

  const err = (browseQ.error as { message?: string } | undefined)?.message;
  const activeCategory = categoryId ?? '';

  return (
    <div>
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Space wrap style={{ justifyContent: 'space-between', width: '100%' }}>
          <Input.Search
            placeholder="Tìm trong kết quả..."
            defaultValue={q}
            onSearch={(v) => setParam('q', v || undefined)}
            style={{ maxWidth: 360 }}
            allowClear
            value={keywordInput}
            onChange={(e) => setKeywordInput(e.target.value)}
          />
          <Segmented
            value={sort}
            onChange={(v) => setParam('sort', String(v))}
            options={[
              { label: 'Mới nhất', value: 'newest' },
              { label: 'Giá ↑', value: 'price_asc' },
              { label: 'Giá ↓', value: 'price_desc' },
            ]}
          />
        </Space>

        <Select
          style={{ minWidth: 220 }}
          placeholder="Tất cả danh mục"
          value={activeCategory || undefined}
          onChange={(v) => setParam('categoryId', v || undefined)}
          allowClear
          options={(categoriesQ.data?.data ?? []).map((c) => ({
            value: String(c.id),
            label: String(c.name),
          }))}
        />

        <ApiErrorAlert message={err ?? null} />
        {q && (
          <Typography.Text type="secondary">
            Kết quả cho “{q}” — {total} sản phẩm
          </Typography.Text>
        )}

        <ProductCardGrid products={products} loading={browseQ.isLoading} />

        {total > PAGE_SIZE && (
          <Pagination
            current={page + 1}
            pageSize={PAGE_SIZE}
            total={total}
            showSizeChanger={false}
            onChange={(p) => setParam('page', String(p - 1))}
            style={{ justifyContent: 'center', display: 'flex' }}
          />
        )}
      </Space>
    </div>
  );
}

export default ProductsPage;
