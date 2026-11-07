import { createApi } from '@reduxjs/toolkit/query/react';
import { axiosBaseQuery, type ApiResponse, type PageResponse } from '../../app/api';

// Admin orders BE (gateway /api/order/admin/**, ADMIN-only, envelope ApiResponse).
export interface AdminOrder {
  orderId: string;
  orderCode: string;
  userId: string;
  status: string;
  totalAmount: number;
  currency: string;
  paymentMethod: string;
  createdAt: string;
}

export interface ListAdminOrdersParams {
  page?: number;
  size?: number;
  status?: string;
  search?: string;
}

export type OrderStatusTransition = 'CONFIRMED' | 'SHIPPED' | 'DELIVERED';

export const adminOrderApi = createApi({
  reducerPath: 'adminOrderApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['AdminOrder'],
  endpoints: (build) => ({
    // GET /api/order/admin/orders?page=&size=&status=&search=
    listAdminOrders: build.query<ApiResponse<PageResponse<AdminOrder>>, ListAdminOrdersParams | void>({
      query: (params) => ({
        url: '/api/order/admin/orders',
        params: { page: 0, size: 20, ...((params ?? {}) as Record<string, unknown>) },
      }),
      providesTags: ['AdminOrder'],
    }),
    // POST /api/order/admin/orders/{orderId}/status {status}
    updateOrderStatus: build.mutation<ApiResponse<unknown>, { orderId: string; status: OrderStatusTransition }>({
      query: ({ orderId, status }) => ({
        url: `/api/order/admin/orders/${orderId}/status`,
        method: 'post',
        data: { status },
      }),
      invalidatesTags: ['AdminOrder'],
    }),
    // POST /api/order/admin/orders/{orderId}/cancel {reason?}
    adminCancelOrder: build.mutation<ApiResponse<unknown>, { orderId: string; reason?: string }>({
      query: ({ orderId, reason }) => ({
        url: `/api/order/admin/orders/${orderId}/cancel`,
        method: 'post',
        data: reason ? { reason } : {},
      }),
      invalidatesTags: ['AdminOrder'],
    }),
  }),
});

export const {
  useListAdminOrdersQuery,
  useUpdateOrderStatusMutation,
  useAdminCancelOrderMutation,
} = adminOrderApi;
