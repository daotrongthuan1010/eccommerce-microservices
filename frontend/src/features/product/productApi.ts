import { createApi } from '@reduxjs/toolkit/query/react';
import { axiosBaseQuery, type ApiResponse } from '../../app/api';

// Wave 2 tasks 2.2 (nhóm A — prefix sạch) + 2.3 (nhóm B — controller mang
// /api/v1 nên qua gateway bị nhân đôi /api/product/api/v1/... verbatim).
// Mọi path copy từ docs/FE_API_CONTRACT.md §5.

export interface Product {
  id: string;
  name: string;
  status?: string;
  [k: string]: unknown;
}

export interface Sku {
  id: string;
  variantId?: string;
  status?: string;
  [k: string]: unknown;
}

export interface Variant {
  id: string;
  productId?: string;
  [k: string]: unknown;
}

export interface Media {
  id: string;
  url?: string;
  [k: string]: unknown;
}

export interface AttributeTemplate {
  id: string;
  name: string;
  [k: string]: unknown;
}

export interface AttributeTemplateItem {
  id: string;
  templateId?: string;
  [k: string]: unknown;
}

export interface ProductAttribute {
  id: string;
  name: string;
  [k: string]: unknown;
}

export interface AttributeOption {
  id: string;
  attributeId?: string;
  value?: string;
  [k: string]: unknown;
}

export interface PriceSchedule {
  id: string;
  skuId?: string;
  [k: string]: unknown;
}

export interface PublishingHistoryEntry {
  id?: string;
  status?: string;
  [k: string]: unknown;
}

export interface PageParams {
  page?: number;
  size?: number;
  [k: string]: unknown;
}

export const productApi = createApi({
  reducerPath: 'productApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Product', 'Sku', 'Variant', 'Media', 'AttrTemplate', 'Attribute', 'PriceSchedule', 'Publishing'],
  endpoints: (build) => ({
    // ---- Nhóm A: products (ProductController @RequestMapping("/products")) ----
    // real 2026-10-22: POST /api/product/products (draft → 201 CREATED)
    createProductDraft: build.mutation<ApiResponse<Product>, Record<string, unknown>>({
      query: (data) => ({ url: '/api/product/products', method: 'post', data }),
      invalidatesTags: ['Product'],
    }),
    // real 2026-10-22: GET /api/product/products/{productId}
    getProduct: build.query<ApiResponse<Product>, string>({
      query: (productId) => ({ url: `/api/product/products/${productId}` }),
      providesTags: (_r, _e, productId) => [{ type: 'Product', id: productId }],
    }),

    // ---- Nhóm A: skus (SkuController @RequestMapping("/skus")) ----
    // real 2026-10-22: POST /api/product/skus/variants/{variantId}/skus
    createSku: build.mutation<ApiResponse<Sku>, { variantId: string; body: Record<string, unknown> }>({
      query: ({ variantId, body }) => ({
        url: `/api/product/skus/variants/${variantId}/skus`,
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Sku'],
    }),
    // real 2026-10-22: GET /api/product/skus/skus/{skuId} (nhân đôi /skus/skus/ verbatim)
    getSku: build.query<ApiResponse<Sku>, string>({
      query: (skuId) => ({ url: `/api/product/skus/skus/${skuId}` }),
      providesTags: (_r, _e, skuId) => [{ type: 'Sku', id: skuId }],
    }),

    // ---- Nhóm A: variants (ProductVariantController @RequestMapping("/products-variant")) ----
    // real 2026-10-22: POST /api/product/products-variant/products/{productId}/variants
    createVariant: build.mutation<
      ApiResponse<Variant>,
      { productId: string; body: Record<string, unknown> }
    >({
      query: ({ productId, body }) => ({
        url: `/api/product/products-variant/products/${productId}/variants`,
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Variant'],
    }),
    // real 2026-10-22: GET /api/product/products-variant/products/{productId}/variants
    listVariants: build.query<ApiResponse<Variant[]>, string>({
      query: (productId) => ({
        url: `/api/product/products-variant/products/${productId}/variants`,
      }),
      providesTags: ['Variant'],
    }),
    // real 2026-10-22: GET /api/product/products-variant/variants/{variantId}
    getVariant: build.query<ApiResponse<Variant>, string>({
      query: (variantId) => ({ url: `/api/product/products-variant/variants/${variantId}` }),
      providesTags: (_r, _e, variantId) => [{ type: 'Variant', id: variantId }],
    }),

    // ---- Nhóm A: media (ProductMediaController @RequestMapping("/products-media")) ----
    // real 2026-10-22: POST /api/product/products-media (upload)
    uploadProductMedia: build.mutation<ApiResponse<Media>, Record<string, unknown>>({
      query: (data) => ({ url: '/api/product/products-media', method: 'post', data }),
      invalidatesTags: ['Media'],
    }),
    // real 2026-10-22: GET /api/product/product-media/{id} (singular per contract)
    getProductMedia: build.query<ApiResponse<Media>, string>({
      query: (id) => ({ url: `/api/product/product-media/${id}` }),
      providesTags: (_r, _e, id) => [{ type: 'Media', id }],
    }),
    // real 2026-10-22: PATCH /api/product/products-media/{id}
    updateProductMedia: build.mutation<ApiResponse<Media>, { id: string; body: Record<string, unknown> }>({
      query: ({ id, body }) => ({ url: `/api/product/products-media/${id}`, method: 'patch', data: body }),
      invalidatesTags: ['Media'],
    }),
    // real 2026-10-22: DELETE /api/product/products-media/{id}
    deleteProductMedia: build.mutation<ApiResponse<unknown>, string>({
      query: (id) => ({ url: `/api/product/products-media/${id}`, method: 'delete' }),
      invalidatesTags: ['Media'],
    }),
    // real 2026-10-22: POST /api/product/products-media/sku-media
    createSkuMedia: build.mutation<ApiResponse<Media>, Record<string, unknown>>({
      query: (data) => ({ url: '/api/product/products-media/sku-media', method: 'post', data }),
      invalidatesTags: ['Media'],
    }),
    // real 2026-10-22: GET /api/product/products-media/sku-media/{id}
    getSkuMedia: build.query<ApiResponse<Media>, string>({
      query: (id) => ({ url: `/api/product/products-media/sku-media/${id}` }),
      providesTags: (_r, _e, id) => [{ type: 'Media', id }],
    }),
    // real 2026-10-22: PATCH /api/product/products-media/sku-media/{id}
    updateSkuMedia: build.mutation<ApiResponse<Media>, { id: string; body: Record<string, unknown> }>({
      query: ({ id, body }) => ({
        url: `/api/product/products-media/sku-media/${id}`,
        method: 'patch',
        data: body,
      }),
      invalidatesTags: ['Media'],
    }),
    // real 2026-10-22: DELETE /api/product/products-media/sku-media/{id}
    deleteSkuMedia: build.mutation<ApiResponse<unknown>, string>({
      query: (id) => ({ url: `/api/product/products-media/sku-media/${id}`, method: 'delete' }),
      invalidatesTags: ['Media'],
    }),

    // ---- Nhóm A: attribute-templates (AttributeTemplateController @RequestMapping("/attribute-templates")) ----
    // real 2026-10-22: POST /api/product/attribute-templates
    createAttributeTemplate: build.mutation<ApiResponse<AttributeTemplate>, Record<string, unknown>>({
      query: (data) => ({ url: '/api/product/attribute-templates', method: 'post', data }),
      invalidatesTags: ['AttrTemplate'],
    }),
    // real 2026-10-22: GET /api/product/attribute-templates/{id}
    getAttributeTemplate: build.query<ApiResponse<AttributeTemplate>, string>({
      query: (id) => ({ url: `/api/product/attribute-templates/${id}` }),
      providesTags: (_r, _e, id) => [{ type: 'AttrTemplate', id }],
    }),
    // real 2026-10-22: PUT /api/product/attribute-templates/{id}
    updateAttributeTemplate: build.mutation<
      ApiResponse<AttributeTemplate>,
      { id: string; body: Record<string, unknown> }
    >({
      query: ({ id, body }) => ({ url: `/api/product/attribute-templates/${id}`, method: 'put', data: body }),
      invalidatesTags: ['AttrTemplate'],
    }),
    // real 2026-10-22: DELETE /api/product/attribute-templates/{id}
    deleteAttributeTemplate: build.mutation<ApiResponse<unknown>, string>({
      query: (id) => ({ url: `/api/product/attribute-templates/${id}`, method: 'delete' }),
      invalidatesTags: ['AttrTemplate'],
    }),
    // real 2026-10-22: POST /api/product/attribute-templates/{templateId}/items
    createAttributeTemplateItem: build.mutation<
      ApiResponse<AttributeTemplateItem>,
      { templateId: string; body: Record<string, unknown> }
    >({
      query: ({ templateId, body }) => ({
        url: `/api/product/attribute-templates/${templateId}/items`,
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['AttrTemplate'],
    }),
    // real 2026-10-22: GET /api/product/attribute-templates/items/{id}
    getAttributeTemplateItem: build.query<ApiResponse<AttributeTemplateItem>, string>({
      query: (id) => ({ url: `/api/product/attribute-templates/items/${id}` }),
      providesTags: (_r, _e, id) => [{ type: 'AttrTemplate', id }],
    }),
    // real 2026-10-22: PUT /api/product/attribute-templates/items/{id}
    updateAttributeTemplateItem: build.mutation<
      ApiResponse<AttributeTemplateItem>,
      { id: string; body: Record<string, unknown> }
    >({
      query: ({ id, body }) => ({
        url: `/api/product/attribute-templates/items/${id}`,
        method: 'put',
        data: body,
      }),
      invalidatesTags: ['AttrTemplate'],
    }),
    // real 2026-10-22: DELETE /api/product/attribute-templates/items/{id}
    deleteAttributeTemplateItem: build.mutation<ApiResponse<unknown>, string>({
      query: (id) => ({ url: `/api/product/attribute-templates/items/${id}`, method: 'delete' }),
      invalidatesTags: ['AttrTemplate'],
    }),

    // ---- Nhóm B: attributes (AttributeController @RequestMapping("/api/v1/attributes")) ----
    // real 2026-10-22: POST /api/product/api/v1/attributes (nhân đôi verbatim)
    createAttribute: build.mutation<ApiResponse<ProductAttribute>, Record<string, unknown>>({
      query: (data) => ({ url: '/api/product/api/v1/attributes', method: 'post', data }),
      invalidatesTags: ['Attribute'],
    }),
    // real 2026-10-22: GET /api/product/api/v1/attributes/{attributeId}
    getAttribute: build.query<ApiResponse<ProductAttribute>, string>({
      query: (attributeId) => ({ url: `/api/product/api/v1/attributes/${attributeId}` }),
      providesTags: (_r, _e, attributeId) => [{ type: 'Attribute', id: attributeId }],
    }),
    // real 2026-10-22: PUT /api/product/api/v1/attributes/{attributeId}
    updateAttribute: build.mutation<
      ApiResponse<ProductAttribute>,
      { attributeId: string; body: Record<string, unknown> }
    >({
      query: ({ attributeId, body }) => ({
        url: `/api/product/api/v1/attributes/${attributeId}`,
        method: 'put',
        data: body,
      }),
      invalidatesTags: ['Attribute'],
    }),
    // real 2026-10-22: DELETE /api/product/api/v1/attributes/{attributeId}
    deleteAttribute: build.mutation<ApiResponse<unknown>, string>({
      query: (attributeId) => ({ url: `/api/product/api/v1/attributes/${attributeId}`, method: 'delete' }),
      invalidatesTags: ['Attribute'],
    }),

    // ---- Nhóm B: options (AttributeOptionController @RequestMapping("/api/v1/attributes/{attributeId}/options")) ----
    // real 2026-10-22: POST /api/product/api/v1/attributes/{attributeId}/options
    createAttributeOption: build.mutation<
      ApiResponse<AttributeOption>,
      { attributeId: string; body: Record<string, unknown> }
    >({
      query: ({ attributeId, body }) => ({
        url: `/api/product/api/v1/attributes/${attributeId}/options`,
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Attribute'],
    }),
    // real 2026-10-22: GET /api/product/api/v1/attributes/{attributeId}/options
    listAttributeOptions: build.query<ApiResponse<AttributeOption[]>, string>({
      query: (attributeId) => ({ url: `/api/product/api/v1/attributes/${attributeId}/options` }),
      providesTags: ['Attribute'],
    }),
    // real 2026-10-22: PUT /api/product/api/v1/attributes/{attributeId}/options/{optionId}
    updateAttributeOption: build.mutation<
      ApiResponse<AttributeOption>,
      { attributeId: string; optionId: string; body: Record<string, unknown> }
    >({
      query: ({ attributeId, optionId, body }) => ({
        url: `/api/product/api/v1/attributes/${attributeId}/options/${optionId}`,
        method: 'put',
        data: body,
      }),
      invalidatesTags: ['Attribute'],
    }),
    // real 2026-10-22: DELETE /api/product/api/v1/attributes/{attributeId}/options/{optionId}
    deleteAttributeOption: build.mutation<
      ApiResponse<unknown>,
      { attributeId: string; optionId: string }
    >({
      query: ({ attributeId, optionId }) => ({
        url: `/api/product/api/v1/attributes/${attributeId}/options/${optionId}`,
        method: 'delete',
      }),
      invalidatesTags: ['Attribute'],
    }),

    // ---- Nhóm B: price-schedules (PriceScheduleController @RequestMapping("/api/v1/skus/{skuId}/price-schedules")) ----
    // real 2026-10-22: POST /api/product/api/v1/skus/{skuId}/price-schedules
    createPriceSchedule: build.mutation<
      ApiResponse<PriceSchedule>,
      { skuId: string; body: Record<string, unknown> }
    >({
      query: ({ skuId, body }) => ({
        url: `/api/product/api/v1/skus/${skuId}/price-schedules`,
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['PriceSchedule'],
    }),
    // real 2026-10-22: GET /api/product/api/v1/skus/{skuId}/price-schedules
    listPriceSchedules: build.query<ApiResponse<PriceSchedule[]>, string>({
      query: (skuId) => ({ url: `/api/product/api/v1/skus/${skuId}/price-schedules` }),
      providesTags: ['PriceSchedule'],
    }),

    // ---- Nhóm B: publishing (ProductPublishingController @RequestMapping("/api/v1")) ----
    // real 2026-10-22: POST /api/product/api/v1/products/{productId}/revisions/{revisionId}/submit-for-approval (header X-Actor-Id)
    submitForApproval: build.mutation<
      ApiResponse<unknown>,
      { productId: string; revisionId: string; actorId: string }
    >({
      query: ({ productId, revisionId, actorId }) => ({
        url: `/api/product/api/v1/products/${productId}/revisions/${revisionId}/submit-for-approval`,
        method: 'post',
        headers: { 'X-Actor-Id': actorId },
      }),
      invalidatesTags: ['Publishing'],
    }),
    // real 2026-10-22: POST /api/product/api/v1/publication-approvals/{approvalId}/approve
    approvePublication: build.mutation<ApiResponse<unknown>, { approvalId: string; actorId: string }>({
      query: ({ approvalId, actorId }) => ({
        url: `/api/product/api/v1/publication-approvals/${approvalId}/approve`,
        method: 'post',
        headers: { 'X-Actor-Id': actorId },
      }),
      invalidatesTags: ['Publishing'],
    }),
    // real 2026-10-22: POST /api/product/api/v1/publication-approvals/{approvalId}/reject (body {reason})
    rejectPublication: build.mutation<
      ApiResponse<unknown>,
      { approvalId: string; actorId: string; reason: string }
    >({
      query: ({ approvalId, actorId, reason }) => ({
        url: `/api/product/api/v1/publication-approvals/${approvalId}/reject`,
        method: 'post',
        data: { reason },
        headers: { 'X-Actor-Id': actorId },
      }),
      invalidatesTags: ['Publishing'],
    }),
    // real 2026-10-22: GET /api/product/api/v1/products/{productId}/publishing-history
    getPublishingHistory: build.query<ApiResponse<PublishingHistoryEntry[]>, string>({
      query: (productId) => ({
        url: `/api/product/api/v1/products/${productId}/publishing-history`,
      }),
      providesTags: ['Publishing'],
    }),

    // Product list: backend hiện chỉ có draft-create + detail (contract §5
    // không có GET list) — ProductsPage tra cứu detail theo ID và giữ
    // session list local; không gọi route list không tồn tại.
  }),
});

export const {
  useCreateProductDraftMutation,
  useGetProductQuery,
  useCreateSkuMutation,
  useGetSkuQuery,
  useCreateVariantMutation,
  useListVariantsQuery,
  useGetVariantQuery,
  useUploadProductMediaMutation,
  useGetProductMediaQuery,
  useUpdateProductMediaMutation,
  useDeleteProductMediaMutation,
  useCreateSkuMediaMutation,
  useGetSkuMediaQuery,
  useUpdateSkuMediaMutation,
  useDeleteSkuMediaMutation,
  useCreateAttributeTemplateMutation,
  useGetAttributeTemplateQuery,
  useUpdateAttributeTemplateMutation,
  useDeleteAttributeTemplateMutation,
  useCreateAttributeTemplateItemMutation,
  useGetAttributeTemplateItemQuery,
  useUpdateAttributeTemplateItemMutation,
  useDeleteAttributeTemplateItemMutation,
  useCreateAttributeMutation,
  useGetAttributeQuery,
  useUpdateAttributeMutation,
  useDeleteAttributeMutation,
  useCreateAttributeOptionMutation,
  useListAttributeOptionsQuery,
  useUpdateAttributeOptionMutation,
  useDeleteAttributeOptionMutation,
  useCreatePriceScheduleMutation,
  useListPriceSchedulesQuery,
  useSubmitForApprovalMutation,
  useApprovePublicationMutation,
  useRejectPublicationMutation,
  useGetPublishingHistoryQuery,
} = productApi;
