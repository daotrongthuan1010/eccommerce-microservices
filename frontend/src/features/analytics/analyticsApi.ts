import { createApi } from '@reduxjs/toolkit/query/react';
import { axiosBaseQuery, type ApiResponse } from '../../app/api';

// Analytics BE (gateway /api/analytics/**, ADMIN-only, envelope ApiResponse).
export interface RevenuePoint {
  statDate: string;
  grossRevenue: number;
  netRevenue: number;
  orderCount: number;
}

export interface AnalyticsKpis {
  todayRevenue: number;
  todayOrders: number;
  last7DaysRevenue: number;
  last30DaysRevenue: number;
  avgOrderValue30d: number;
}

export interface TopProduct {
  skuId: string;
  productName: string;
  totalQuantity: number;
  totalRevenue: number;
}

export interface RevenueParams {
  from: string;
  to: string;
  granularity?: 'day' | 'month';
}

export interface TopProductsParams {
  from: string;
  to: string;
  limit?: number;
}

export const analyticsApi = createApi({
  reducerPath: 'analyticsApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Revenue', 'Kpis', 'TopProducts'],
  endpoints: (build) => ({
    // GET /api/analytics/revenue?from=&to=&granularity=
    revenue: build.query<ApiResponse<RevenuePoint[]>, RevenueParams>({
      query: ({ from, to, granularity = 'day' }) => ({
        url: '/api/analytics/revenue',
        params: { from, to, granularity },
      }),
      providesTags: ['Revenue'],
    }),
    // GET /api/analytics/kpis
    kpis: build.query<ApiResponse<AnalyticsKpis>, void>({
      query: () => ({ url: '/api/analytics/kpis' }),
      providesTags: ['Kpis'],
    }),
    // GET /api/analytics/top-products?from=&to=&limit=
    topProducts: build.query<ApiResponse<TopProduct[]>, TopProductsParams>({
      query: ({ from, to, limit = 10 }) => ({
        url: '/api/analytics/top-products',
        params: { from, to, limit },
      }),
      providesTags: ['TopProducts'],
    }),
  }),
});

export const { useRevenueQuery, useKpisQuery, useTopProductsQuery } = analyticsApi;
