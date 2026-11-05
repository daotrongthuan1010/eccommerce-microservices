import { createApi } from '@reduxjs/toolkit/query/react';
import { axiosBaseQuery, type ApiResponse, type PageResponse } from '../../app/api';

// Marketplace browse (2026-11-05 fe-marketplace D4): GET cong khai, khach
// chua login van goi duoc (gateway permitAll GET /api/product/**, /api/catalog/**,
// /api/review/**). Path qua gateway verbatim theo FE_API_CONTRACT §0.2.

export interface ProductCard {
  id: string;
  productCode: string;
  name: string;
  shortDescription?: string | null;
  categoryId: string;
  brandId?: string | null;
  priceFrom: number; // VND long
  currency: string;
  primaryImageKey?: string | null;
  sellableSkuCount: number;
}

export interface BrowseParams {
  page?: number;
  size?: number;
  categoryId?: string;
  brandId?: string;
  minPrice?: number;
  maxPrice?: number;
  keyword?: string;
  sort?: 'newest' | 'price_asc' | 'price_desc';
}

// real 2026-11-05: GET /api/product/products/browse (ProductController nhom A
// /products + /browse) — paging + filter + sort, chi san pham PUBLISHED.
export const browseApi = createApi({
  reducerPath: 'browseApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Browse'],
  endpoints: (build) => ({
    browseProducts: build.query<ApiResponse<PageResponse<ProductCard>>, BrowseParams | void>({
      query: (params) => ({
        url: 'product/products/browse',
        params: (params ?? {}) as Record<string, unknown>,
      }),
      providesTags: ['Browse'],
    }),
    // real 2026-11-05: GET /api/review/api/products/{productId}/reviews
    // (ReviewQueryController @RequestMapping("/api") — doubled prefix verbatim).
    listProductReviews: build.query<ApiResponse<unknown>, { productId: string; page?: number; size?: number }>({
      query: ({ productId, page = 0, size = 10 }) => ({
        url: `review/api/products/${productId}/reviews`,
        params: { page, size },
      }),
    }),
    // real 2026-11-05: GET /api/review/api/products/{productId}/rating
    getProductRating: build.query<ApiResponse<unknown>, string>({
      query: (productId) => ({ url: `review/api/products/${productId}/rating` }),
    }),
  }),
});

export const { useBrowseProductsQuery, useListProductReviewsQuery, useGetProductRatingQuery } = browseApi;
