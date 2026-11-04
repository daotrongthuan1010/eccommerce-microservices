import { createApi } from '@reduxjs/toolkit/query/react';
import { axiosBaseQuery, type ApiResponse, type PageResponse } from '../../app/api';

// Marketplace checkout (2026-11-05 fe-marketplace D6) — thay the 100% mock
// orderMock bang API that order-service (da smoke test 2026-11-04).
// Gateway StripPrefix=2: /api/order/** -> order-service /cart, /me/...
// Checkout bat buoc header Idempotency-Key (UUID moi lan bam "Dat hang",
// giu nguyen khi retry trong cung lan submit — khach bam nhieu chi tao 1 don).

export interface CartItem {
  skuId: string;
  productName: string;
  unitPrice: number; // VND long
  quantity: number;
  lineAmount: number;
}

export interface Cart {
  id: string;
  userId: string;
  items: CartItem[];
  totalQuantity: number;
  totalAmount: number;
  updatedAt?: string;
}

export interface AddCartItemPayload {
  skuId: string;
  productName: string;
  unitPrice: number;
  quantity: number;
}

export interface CheckoutPayload {
  shippingAddress: string;
  recipientName: string;
  recipientPhone: string;
  paymentMethod: 'VNPAY' | 'ZALOPAY' | 'COD';
}

export interface CheckoutResult {
  orderId: string;
  orderCode: string;
  status: string;
  totalAmount: number;
  currency: string;
  paymentMethod: string;
  paymentId?: string | null;
  paymentStatus?: string | null;
  checkoutUrl?: string | null;
  qrContent?: string | null;
  expiresAt?: string | null;
  lines: { skuId: string; productName: string; unitPrice: number; quantity: number; lineAmount: number }[];
}

export interface Order {
  id: string;
  orderCode: string;
  status: string;
  totalAmount: number;
  currency: string;
  paymentMethod: string;
  paymentId?: string | null;
  shippingAddress: string;
  recipientName: string;
  recipientPhone: string;
  cancelReason?: string | null;
  createdAt: string;
  updatedAt?: string;
  lines: CheckoutResult['lines'];
}

export interface PaymentStatus {
  id?: string;
  orderId?: string;
  status?: string;
  amount?: number;
  currency?: string;
  method?: string;
  [k: string]: unknown;
}

export const orderApi = createApi({
  reducerPath: 'orderApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Cart', 'Order', 'Payment'],
  endpoints: (build) => ({
    // real 2026-11-05: GET /api/order/cart (CartController GET /cart)
    getCart: build.query<ApiResponse<Cart>, void>({
      query: () => ({ url: '/api/order/cart' }),
      providesTags: ['Cart'],
    }),
    // real 2026-11-05: POST /api/order/cart/items — BE cong quantity khi trung SKU.
    addToCart: build.mutation<ApiResponse<Cart>, AddCartItemPayload>({
      query: (body) => ({ url: '/api/order/cart/items', method: 'post', data: body }),
      invalidatesTags: ['Cart'],
    }),
    // real 2026-11-05: PUT /api/order/cart/items/{skuId}
    updateCartItem: build.mutation<ApiResponse<Cart>, { skuId: string; quantity: number }>({
      query: ({ skuId, quantity }) => ({
        url: `/api/order/cart/items/${skuId}`,
        method: 'put',
        data: { quantity },
      }),
      invalidatesTags: ['Cart'],
    }),
    // real 2026-11-05: DELETE /api/order/cart/items/{skuId}
    removeFromCart: build.mutation<ApiResponse<Cart>, string>({
      query: (skuId) => ({ url: `/api/order/cart/items/${skuId}`, method: 'delete' }),
      invalidatesTags: ['Cart'],
    }),
    // real 2026-11-05: POST /api/order/me/checkout — Idempotency-Key bat buoc.
    checkout: build.mutation<ApiResponse<CheckoutResult>, { body: CheckoutPayload; idempotencyKey: string }>({
      query: ({ body, idempotencyKey }) => ({
        url: '/api/order/me/checkout',
        method: 'post',
        data: body,
        headers: { 'Idempotency-Key': idempotencyKey },
      }),
      invalidatesTags: ['Order', 'Cart'],
    }),
    // real 2026-11-05: GET /api/order/me/orders
    myOrders: build.query<ApiResponse<PageResponse<Order>>, { page?: number; size?: number }>({
      query: ({ page = 0, size = 10 } = {}) => ({
        url: '/api/order/me/orders',
        params: { page, size },
      }),
      providesTags: ['Order'],
    }),
    // real 2026-11-05: GET /api/order/me/orders/{orderId}
    myOrder: build.query<ApiResponse<Order>, string>({
      query: (orderId) => ({ url: `/api/order/me/orders/${orderId}` }),
      providesTags: (_r, _e, orderId) => [{ type: 'Order', id: orderId }],
    }),
    // real 2026-11-05: POST /api/order/me/orders/{orderId}/cancel (chi PENDING_PAYMENT)
    cancelOrder: build.mutation<ApiResponse<Order>, { orderId: string; reason?: string }>({
      query: ({ orderId, reason }) => ({
        url: `/api/order/me/orders/${orderId}/cancel`,
        method: 'post',
        data: reason ? { reason } : {},
      }),
      invalidatesTags: ['Order'],
    }),
    // real 2026-11-05: GET /api/payment/payments/by-order/{orderId} — poll ket
    // qua thanh toan tren trang /payment/result (nguon su that la IPN; FE poll
    // de hien thi, khong tu ket luan UNKNOWN la thanh cong — AGENTS.md luat 8).
    paymentByOrder: build.query<ApiResponse<PaymentStatus[] | PaymentStatus>, string>({
      query: (orderId) => ({ url: `/api/payment/payments/by-order/${orderId}` }),
      providesTags: (_r, _e, orderId) => [{ type: 'Payment', id: orderId }],
    }),
  }),
});

export const {
  useGetCartQuery,
  useAddToCartMutation,
  useUpdateCartItemMutation,
  useRemoveFromCartMutation,
  useCheckoutMutation,
  useMyOrdersQuery,
  useMyOrderQuery,
  useCancelOrderMutation,
  usePaymentByOrderQuery,
} = orderApi;
