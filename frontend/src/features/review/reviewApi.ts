import { createApi } from '@reduxjs/toolkit/query/react';
import { axiosBaseQuery, type ApiResponse } from '../../app/api';

// Review data slice — endpoints copy tu review-service controllers (2026-11-05
// review-fix, boi lai tu mock):
// - ReviewController @RequestMapping("/api/reviews"): POST create (Idempotency-Key),
//   PUT /{id}, DELETE /{id}
// - ReviewInteractionController @RequestMapping("/api/reviews"): PUT /{id}/helpful,
//   POST /{id}/reports, POST /{id}/reply
// - ReviewQueryController @RequestMapping("/api"): GET /reviews/me,
//   /reviews/{id}, /products/{id}/reviews, /products/{id}/rating
// - ReviewModerationController @RequestMapping("/admin/reviews/moderation"):
//   GET /revisions (hang cho duyet), POST /revisions/{id}/decision,
//   /reviews/{id}/hide, /reviews/{id}/restore
// Gateway /api/review/** StripPrefix=2 + controller prefix → FE url: review/...

export interface ReviewRequest {
  productId: string;
  rating: number;
  title?: string;
  content?: string;
  media?: { fileId: string; mediaType: 'IMAGE' | 'VIDEO' }[];
  [k: string]: unknown;
}

export interface ReviewReply {
  id: string;
  content: string;
  publishedAt: string;
  [k: string]: unknown;
}

export interface ReviewMedia {
  fileId: string;
  mediaType: string;
  mediaUrl?: string | null;
  thumbnailUrl?: string | null;
  sortOrder?: number;
}

export interface Review {
  id: string;
  productId?: string;
  rating?: number;
  content?: string;
  title?: string;
  reviewerDisplayName?: string;
  anonymous?: boolean;
  verifiedPurchase?: boolean;
  status?: string;
  revisionStatus?: string;
  revisionId?: string;
  revisionNo?: number;
  helpfulCount?: number;
  replies?: ReviewReply[];
  media?: ReviewMedia[];
  [k: string]: unknown;
}

export interface ReviewListItem {
  id: string;
  displayUserName: string;
  rating: number;
  comment: string;
  verifiedPurchase: boolean;
  anonymous: boolean;
  edited: boolean;
  helpfulCount: number;
  likedByMe?: boolean;
  replies?: ReviewReply[];
  media?: ReviewMedia[];
  [k: string]: unknown;
}

/** Duyet revision: isApproved=true → PUBLISH, false → REJECT (kem reason). */
export interface DecisionRequest {
  isApproved: boolean;
  reasonCode?: string;
  reasonNote?: string;
}

export interface ModerationActionRequest {
  reasonCode?: string;
  reasonNote?: string;
  abuseReportId?: string;
}

export interface ReviewPage {
  content: Review[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ReviewListPage {
  content: ReviewListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const reviewApi = createApi({
  reducerPath: 'reviewApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Review', 'Moderation'],
  endpoints: (build) => ({
    // GET /api/review/api/reviews/me — review cua toi (phan trang).
    listMyReviews: build.query<ApiResponse<ReviewPage>, { page?: number; size?: number } | void>({
      query: (params) => ({
        url: 'review/api/reviews/me',
        params: { page: 0, size: 10, ...(params ?? {}) },
      }),
      providesTags: ['Review'],
    }),
    // GET /api/review/api/products/{productId}/reviews — review cong khai theo SP.
    listProductReviews: build.query<
      ApiResponse<ReviewListPage>,
      { productId: string; page?: number; size?: number }
    >({
      query: ({ productId, page = 0, size = 10 }) => ({
        url: `review/api/products/${productId}/reviews`,
        params: { page, size },
      }),
      providesTags: ['Review'],
    }),
    // GET /api/review/api/products/{productId}/rating — diem tong hop.
    getProductRating: build.query<ApiResponse<unknown>, string>({
      query: (productId) => ({ url: `review/api/products/${productId}/rating` }),
      providesTags: ['Review'],
    }),
    // GET /api/review/admin/reviews/moderation/revisions — hang cho duyet (MODERATOR/ADMIN).
    listPendingReviews: build.query<ApiResponse<ReviewPage>, { page?: number; size?: number } | void>({
      query: (params) => ({
        url: 'review/admin/reviews/moderation/revisions',
        params: { page: 0, size: 20, ...(params ?? {}) },
      }),
      providesTags: ['Moderation'],
    }),
    // POST /api/review/api/reviews — tao review (Idempotency-Key bat buoc → 201).
    createReview: build.mutation<ApiResponse<Review>, { body: ReviewRequest; idempotencyKey: string }>({
      query: ({ body, idempotencyKey }) => ({
        url: 'review/api/reviews',
        method: 'post',
        data: body,
        headers: { 'Idempotency-Key': idempotencyKey },
      }),
      invalidatesTags: ['Review'],
    }),
    // PUT /api/review/api/reviews/{id} — sua review cua minh.
    updateReview: build.mutation<ApiResponse<Review>, { id: string; body: Partial<ReviewRequest> }>({
      query: ({ id, body }) => ({ url: `review/api/reviews/${id}`, method: 'put', data: body }),
      invalidatesTags: ['Review'],
    }),
    // DELETE /api/review/api/reviews/{id} — xoa review cua minh.
    deleteReview: build.mutation<ApiResponse<unknown>, string>({
      query: (id) => ({ url: `review/api/reviews/${id}`, method: 'delete' }),
      invalidatesTags: ['Review'],
    }),
    // PUT /api/review/api/reviews/{id}/helpful?isHelpful= — danh gia "hữu ích".
    setHelpful: build.mutation<ApiResponse<unknown>, { id: string; isHelpful: boolean }>({
      query: ({ id, isHelpful }) => ({
        url: `review/api/reviews/${id}/helpful`,
        method: 'put',
        params: { isHelpful },
      }),
      invalidatesTags: ['Review'],
    }),
    // POST /api/review/admin/reviews/moderation/revisions/{revisionId}/decision — duyet/tuchoi revision.
    decideReview: build.mutation<ApiResponse<unknown>, { revisionId: string; body: DecisionRequest }>({
      query: ({ revisionId, body }) => ({
        url: `review/admin/reviews/moderation/revisions/${revisionId}/decision`,
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Moderation', 'Review'],
    }),
    // POST /api/review/admin/reviews/moderation/reviews/{id}/hide — an review vi pham.
    hideReview: build.mutation<ApiResponse<unknown>, { id: string; body: ModerationActionRequest }>({
      query: ({ id, body }) => ({
        url: `review/admin/reviews/moderation/reviews/${id}/hide`,
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Moderation', 'Review'],
    }),
    // POST /api/review/admin/reviews/moderation/reviews/{id}/restore — mo lai review bi an.
    restoreReview: build.mutation<ApiResponse<unknown>, { id: string; body: ModerationActionRequest }>({
      query: ({ id, body }) => ({
        url: `review/admin/reviews/moderation/reviews/${id}/restore`,
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Moderation', 'Review'],
    }),
  }),
});

export const {
  useListMyReviewsQuery,
  useListProductReviewsQuery,
  useGetProductRatingQuery,
  useListPendingReviewsQuery,
  useCreateReviewMutation,
  useUpdateReviewMutation,
  useDeleteReviewMutation,
  useSetHelpfulMutation,
  useDecideReviewMutation,
  useHideReviewMutation,
  useRestoreReviewMutation,
} = reviewApi;
