import { Input, Pagination, Segmented, Select, Space, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useBrowseProductsQuery } from '../features/marketplace/browseApi';
import { useSearchProductsQuery } from '../features/search/searchApi';
import { useGetCategoriesQuery } from '../features/catalog/catalogApi';
import { ProductCardGrid } from '../components/marketplace/ProductCardGrid';
import { ApiErrorAlert } from '../components/ui';

// Marketplace ProductsPage (2026-11-05 fe-marketplace D2, spec customer/02):
// tim kiem + loc danh muc + sort + paging — kieu trang search Shopee/Tiki.
// Query nam tren URL (?q=&categoryId=&sort=&page=) de share duoc link.
//
// vi-search (2026-11-06): co q → search-service (ES CocCoc: bo dau, fuzzy,
// synonyms). ES loi/rong → fallback browse cu (contains) — khach van co ket
// qua, khong thay loi he thong. Loc danh muc + sort gia chi ap dung luc
// browse (ES search chua ho tro facet — P2).
const PAGE_SIZE = 20;

export function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [keywordInput, setKeywordInput] = useState(searchParams.get('q') ?? '');

  const q = searchParams.get('q') ?? undefined;
  const categoryId = searchParams.get('categoryId') ?? undefined;
  const sort = (searchParams.get('sort') ?? 'newest') as 'newest' | 'price_asc' | 'price_desc';
  const page = Math.max(0, Number(searchParams.get('page') ?? 0));

  // Search ES chi chay khi co tu khoa; skip khi chi loc danh muc.
  const searchQ = useSearchProductsQuery(
    { q: q ?? '', page, size: PAGE_SIZE },
    { skip: !q },
  );
  // Browse: fallback khi ES loi, hoac nguon chinh khi khong co q.
  const browseQ = useBrowseProductsQuery(
    { page, size: PAGE_SIZE, keyword: q, categoryId, sort },
    { skip: !!q && !searchQ.isError }, // co q va ES khong loi → khong can browse
  );
  const categoriesQ = useGetCategoriesQuery();

  // Dong bo o search header (MarketplaceLayout) khi user sua URL tay.
  useEffect(() => {
    setKeywordInput(q ?? '');
  }, [q]);

  // Ưu tiên ES khi có q; lỗi ES → browse (q vẫn truyền để contains cũ chạy).
  const useEs = !!q && !searchQ.isError;
  const esContent = searchQ.data?.data?.content ?? [];
  const browseContent = browseQ.data?.data?.content ?? [];
  const products = useEs ? esContent : browseContent;
  const total = useEs
    ? searchQ.data?.data?.totalElements ?? 0
    : browseQ.data?.data?.totalElements ?? 0;

  const setParam = (key: string, value?: string) => {
    const next = new URLSearchParams(searchParams);
    if (value) next.set(key, value);
    else next.delete(key);
    if (key !== 'page') next.delete('page'); // doi filter ve trang 1
    setSearchParams(next, { replace: true });
  };

  const err =
    (searchQ.error as { message?: string } | undefined)?.message ??
    (browseQ.error as { message?: string } | undefined)?.message;
  const activeCategory = categoryId ?? '';
  const loading = useEs ? searchQ.isLoading : browseQ.isLoading;

  return (
    <div>
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Space wrap style={{ justifyContent: 'space-between', width: '100%' }}>
          <Input.Search
            placeholder="Tìm trong kết quả... (gõ không dấu vẫn ra)"
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

        <ApiErrorAlert message={useEs ? null : err ?? null} />
        {q && (
          <Typography.Text type="secondary">
            Kết quả cho “{q}” — {total} sản phẩm
          </Typography.Text>
        )}

        <ProductCardGrid
          products={products}
          loading={loading}
          emptyText="Không tìm thấy sản phẩm phù hợp"
        />

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
