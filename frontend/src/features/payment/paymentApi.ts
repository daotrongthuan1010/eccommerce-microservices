import { createApi } from '@reduxjs/toolkit/query/react';
import { axiosBaseQuery, type ApiResponse } from '../../app/api';

// Payment refund (gateway /api/payment/**). Chi refund khi payment PAID /
// PARTIALLY_REFUNDED — header Idempotency-Key bat buoc (UUID moi moi lan bam).
export interface CreateRefundPayload {
  paymentId: string;
  amount: number;
  reason?: string;
  idempotencyKey: string;
}

export interface Refund {
  id?: string;
  paymentId?: string;
  amount?: number;
  reason?: string | null;
  status?: string;
  [k: string]: unknown;
}

export const paymentApi = createApi({
  reducerPath: 'paymentApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Payment', 'Refund'],
  endpoints: (build) => ({
    // POST /api/payment/payments/{paymentId}/refunds → 201
    createRefund: build.mutation<ApiResponse<Refund>, CreateRefundPayload>({
      query: ({ paymentId, amount, reason, idempotencyKey }) => ({
        url: `/api/payment/payments/${paymentId}/refunds`,
        method: 'post',
        data: { amount, ...(reason ? { reason } : {}) },
        headers: { 'Idempotency-Key': idempotencyKey },
      }),
      invalidatesTags: ['Payment', 'Refund'],
    }),
  }),
});

export const { useCreateRefundMutation } = paymentApi;
