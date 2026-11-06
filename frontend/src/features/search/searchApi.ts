import { createApi } from '@reduxjs/toolkit/query/react';
import { axiosBaseQuery, type ApiResponse, type PageResponse } from '../../app/api';

// vi-search (2026-11-06): tim kiem tieng Viet qua search-service (ES CocCoc)
// — bo dau tu dong, fuzzy (go sai van ra), synonyms ("dtdd" ≈ "điện thoại").
// Gateway: /api/search/** → search-service GET /products (StripPrefix=2).
// Response la PageResponse chuan cua common → FE dung chung card grid.

export interface SearchProductHit {
  productId: string;
  productCode: string;
  name: string;
  shortDescription?: string | null;
  priceFrom: number;
  categoryId?: string | null;
  brandId?: string | null;
  primaryImageKey?: string | null;
  sellableSkuCount?: number;
  score?: number;
}

export const searchApi = createApi({
  reducerPath: 'searchApi',
  baseQuery: axiosBaseQuery(),
  endpoints: (build) => ({
    // real 2026-11-06: GET /api/search/products?q=... — public (guest OK).
    // Read-only: khong co mutation → khong can tagTypes.
    searchProducts: build.query<
      ApiResponse<PageResponse<SearchProductHit>>,
      { q: string; page?: number; size?: number }
    >({
      query: ({ q, page = 0, size = 20 }) => ({
        url: 'search/products',
        params: { q, page, size },
      }),
    }),
  }),
});

export const { useSearchProductsQuery } = searchApi;
