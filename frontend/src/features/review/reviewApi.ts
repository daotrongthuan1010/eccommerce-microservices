import { createApi } from '@reduxjs/toolkit/query/react';
import { axiosBaseQuery, type ApiResponse } from '../../app/api';

// Wave 2 task 2.4 — Review data slice. Paths verbatim từ
// docs/FE_API_CONTRACT.md §6.
//
// NOTE (mock, không nhãn real): backend HIỆN KHÔNG CÓ GET list/search
// review công khai (contract §6: "FE dùng search-service (khi có) hoặc
// mock"). ReviewsPage đọc danh sách từ src/mocks/reviewListMock.ts.
// Chỉ các mutation dưới đây gọi API thật.

export interface ModerationRequest {
  action: 'APPROVE' | 'REJECT' | 'HIDE';
  reason?: string;
  [k: string]: unknown;
}

export interface ReviewRequest {
  productId: string;
  rating: number;
  title?: string;
  content?: string;
  [k: string]: unknown;
}

export interface Review {
  id: string;
  productId?: string;
  rating?: number;
  status?: string;
  [k: string]: unknown;
}

export const reviewApi = createApi({
  reducerPath: 'reviewApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Review'],
  endpoints: (build) => ({
    // real 2026-10-22: POST /api/review/admin/reviews/{id}/moderate (cần MODERATOR hoặc ADMIN)
    moderateReview: build.mutation<ApiResponse<Review>, { id: string; body: ModerationRequest }>({
      query: ({ id, body }) => ({
        url: `/api/review/admin/reviews/${id}/moderate`,
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Review'],
    }),
    // real 2026-10-22: POST /api/review/api/reviews (header Idempotency-Key BẮT BUỘC → 201)
    // Page tạo uuid mỗi lần submit và giữ nguyên khi retry để double-submit chỉ tạo 1 review.
    createReview: build.mutation<ApiResponse<Review>, { body: ReviewRequest; idempotencyKey: string }>({
      query: ({ body, idempotencyKey }) => ({
        url: '/api/review/api/reviews',
        method: 'post',
        data: body,
        headers: { 'Idempotency-Key': idempotencyKey },
      }),
      invalidatesTags: ['Review'],
    }),
    // real 2026-10-22: PUT /api/review/api/reviews/{id}
    updateReview: build.mutation<ApiResponse<Review>, { id: string; body: Partial<ReviewRequest> }>({
      query: ({ id, body }) => ({ url: `/api/review/api/reviews/${id}`, method: 'put', data: body }),
      invalidatesTags: ['Review'],
    }),
    // real 2026-10-22: DELETE /api/review/api/reviews/{id}
    deleteReview: build.mutation<ApiResponse<unknown>, string>({
      query: (id) => ({ url: `/api/review/api/reviews/${id}`, method: 'delete' }),
      invalidatesTags: ['Review'],
    }),
    // real 2026-10-22: POST /api/review/api/reviews/{id}/vote?isHelpful=
    voteReview: build.mutation<ApiResponse<unknown>, { id: string; isHelpful: boolean }>({
      query: ({ id, isHelpful }) => ({
        url: `/api/review/api/reviews/${id}/vote`,
        method: 'post',
        params: { isHelpful },
      }),
      invalidatesTags: ['Review'],
    }),
    // real 2026-10-22: DELETE /api/review/api/reviews/{id}/vote
    unvoteReview: build.mutation<ApiResponse<unknown>, string>({
      query: (id) => ({ url: `/api/review/api/reviews/${id}/vote`, method: 'delete' }),
      invalidatesTags: ['Review'],
    }),
  }),
});

export const {
  useModerateReviewMutation,
  useCreateReviewMutation,
  useUpdateReviewMutation,
  useDeleteReviewMutation,
  useVoteReviewMutation,
  useUnvoteReviewMutation,
} = reviewApi;
