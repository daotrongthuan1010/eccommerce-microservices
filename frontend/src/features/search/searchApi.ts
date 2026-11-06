import { createApi } from '@reduxjs/toolkit/query/react';
import { axiosBaseQuery, type ApiResponse } from '../../app/api';

// vi-search (2026-11-06): tim kiem tieng Viet qua search-service (ES CocCoc)
// — bo dau tu dong, fuzzy (go sai van ra), synonyms ("dtdd" ≈ "điện thoại").
// Gateway: /api/search/** → search-service GET /products (StripPrefix=2).
// Response tra PageResponse-shape { content, totalElements } de FE dung chung
// card grid voi browse.

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

export interface SearchPage {
  content: SearchProductHit[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const searchApi = createApi({
  reducerPath: 'searchApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Search'],
  endpoints: (build) => ({
    // real 2026-11-06: GET /api/search/products?q=... — public (guest OK).
    searchProducts: build.query<ApiResponse<SearchPage>, { q: string; page?: number; size?: number }>({
      query: ({ q, page = 0, size = 20 }) => ({
        url: 'search/products',
        params: { q, page, size },
      }),
      providesTags: ['Search'],
    }),
  }),
});

export const { useSearchProductsQuery } = searchApi;
