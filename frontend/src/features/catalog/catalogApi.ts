import { createApi } from '@reduxjs/toolkit/query/react';
import { axiosBaseQuery, type ApiResponse, type PageResponse } from '../../app/api';

// Wave 2 task 2.1 — Catalog data slice. Mọi path gateway copy verbatim từ
// docs/FE_API_CONTRACT.md §3 (StripPrefix=2, controllers giữ prefix gốc).

export interface Category {
  id: string;
  name: string;
  slug?: string;
  parentId?: string | null;
  status?: string;
  [k: string]: unknown;
}

export interface CategoryTreeNode extends Category {
  children?: CategoryTreeNode[];
}

export interface Brand {
  id: string;
  name: string;
  slug?: string;
  status?: string;
  [k: string]: unknown;
}

export interface Manufacturer {
  id: string;
  name: string;
  status?: string;
  [k: string]: unknown;
}

export interface Collection {
  id: string;
  name: string;
  slug?: string;
  status?: string;
  [k: string]: unknown;
}

export interface SynonymSet {
  id: string;
  name: string;
  status?: string;
  [k: string]: unknown;
}

export interface Synonym {
  id: string;
  term: string;
  [k: string]: unknown;
}

export interface DisplayRule {
  id: string;
  collectionId: string;
  status?: string;
  [k: string]: unknown;
}

export interface PageParams {
  page?: number;
  size?: number;
  [k: string]: unknown;
}

export const catalogApi = createApi({
  reducerPath: 'catalogApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Category', 'Brand', 'Manufacturer', 'Collection', 'Synonym', 'DisplayRule'],
  endpoints: (build) => ({
    // ---- Categories (CategoryController @RequestMapping("/categories")) ----
    // real 2026-10-22: GET /api/catalog/categories
    getCategories: build.query<ApiResponse<Category[]>, PageParams | void>({
      query: (params) => ({ url: '/api/catalog/categories', params: params ?? undefined }),
      providesTags: ['Category'],
    }),
    // real 2026-10-22: GET /api/catalog/categories/tree
    getCategoryTree: build.query<ApiResponse<CategoryTreeNode[]>, void>({
      query: () => ({ url: '/api/catalog/categories/tree' }),
      providesTags: ['Category'],
    }),
    // real 2026-10-22: GET /api/catalog/categories/{id}
    getCategory: build.query<ApiResponse<Category>, string>({
      query: (id) => ({ url: `/api/catalog/categories/${id}` }),
      providesTags: (_r, _e, id) => [{ type: 'Category', id }],
    }),
    // real 2026-10-22: GET /api/catalog/categories/{parentId}/children
    getCategoryChildren: build.query<ApiResponse<Category[]>, string>({
      query: (parentId) => ({ url: `/api/catalog/categories/${parentId}/children` }),
      providesTags: ['Category'],
    }),
    // real 2026-10-22: POST /api/catalog/categories
    createCategory: build.mutation<ApiResponse<Category>, Partial<Category>>({
      query: (data) => ({ url: '/api/catalog/categories', method: 'post', data }),
      invalidatesTags: ['Category'],
    }),
    // real 2026-10-22: PUT /api/catalog/categories/{id}
    updateCategory: build.mutation<ApiResponse<Category>, { id: string; body: Partial<Category> }>({
      query: ({ id, body }) => ({ url: `/api/catalog/categories/${id}`, method: 'put', data: body }),
      invalidatesTags: ['Category'],
    }),
    // real 2026-10-22: DELETE /api/catalog/categories/{id}
    deleteCategory: build.mutation<ApiResponse<unknown>, string>({
      query: (id) => ({ url: `/api/catalog/categories/${id}`, method: 'delete' }),
      invalidatesTags: ['Category'],
    }),

    // ---- Brands (BrandsController @RequestMapping("/brands")) ----
    // real 2026-10-22: GET /api/catalog/brands
    listBrands: build.query<ApiResponse<PageResponse<Brand>>, PageParams | void>({
      query: (params) => ({
        url: '/api/catalog/brands',
        params: { page: 0, size: 20, ...(params ?? {}) },
      }),
      providesTags: ['Brand'],
    }),
    // real 2026-10-22: GET /api/catalog/brands/{id}
    getBrand: build.query<ApiResponse<Brand>, string>({
      query: (id) => ({ url: `/api/catalog/brands/${id}` }),
      providesTags: (_r, _e, id) => [{ type: 'Brand', id }],
    }),
    // real 2026-10-22: GET /api/catalog/brands/status/active
    getActiveBrands: build.query<ApiResponse<Brand[]>, void>({
      query: () => ({ url: '/api/catalog/brands/status/active' }),
      providesTags: ['Brand'],
    }),
    // real 2026-10-22: GET /api/catalog/brands/search
    searchBrands: build.query<ApiResponse<PageResponse<Brand>>, PageParams | void>({
      query: (params) => ({
        url: '/api/catalog/brands/search',
        params: { page: 0, size: 20, ...(params ?? {}) },
      }),
      providesTags: ['Brand'],
    }),
    // real 2026-10-22: POST /api/catalog/brands
    createBrand: build.mutation<ApiResponse<Brand>, Partial<Brand>>({
      query: (data) => ({ url: '/api/catalog/brands', method: 'post', data }),
      invalidatesTags: ['Brand'],
    }),
    // real 2026-10-22: PUT /api/catalog/brands/{id}
    updateBrand: build.mutation<ApiResponse<Brand>, { id: string; body: Partial<Brand> }>({
      query: ({ id, body }) => ({ url: `/api/catalog/brands/${id}`, method: 'put', data: body }),
      invalidatesTags: ['Brand'],
    }),
    // real 2026-10-22: DELETE /api/catalog/brands/{id}
    deleteBrand: build.mutation<ApiResponse<unknown>, string>({
      query: (id) => ({ url: `/api/catalog/brands/${id}`, method: 'delete' }),
      invalidatesTags: ['Brand'],
    }),
    // real 2026-10-22: PATCH /api/catalog/brands/{id}/status
    patchBrandStatus: build.mutation<ApiResponse<Brand>, { id: string; body: Record<string, unknown> }>({
      query: ({ id, body }) => ({ url: `/api/catalog/brands/${id}/status`, method: 'patch', data: body }),
      invalidatesTags: ['Brand'],
    }),

    // ---- Manufacturers (ManufacturerController @RequestMapping("/manufacturers")) ----
    // real 2026-10-22: POST /api/catalog/manufacturers
    createManufacturer: build.mutation<ApiResponse<Manufacturer>, Partial<Manufacturer>>({
      query: (data) => ({ url: '/api/catalog/manufacturers', method: 'post', data }),
      invalidatesTags: ['Manufacturer'],
    }),
    // real 2026-10-22: GET /api/catalog/manufacturers/{id}
    getManufacturer: build.query<ApiResponse<Manufacturer>, string>({
      query: (id) => ({ url: `/api/catalog/manufacturers/${id}` }),
      providesTags: (_r, _e, id) => [{ type: 'Manufacturer', id }],
    }),
    // real 2026-10-22: GET /api/catalog/manufacturers
    listManufacturers: build.query<ApiResponse<PageResponse<Manufacturer>>, PageParams | void>({
      query: (params) => ({
        url: '/api/catalog/manufacturers',
        params: { page: 0, size: 20, ...(params ?? {}) },
      }),
      providesTags: ['Manufacturer'],
    }),
    // real 2026-10-22: GET /api/catalog/manufacturers/active
    getActiveManufacturers: build.query<ApiResponse<Manufacturer[]>, void>({
      query: () => ({ url: '/api/catalog/manufacturers/active' }),
      providesTags: ['Manufacturer'],
    }),
    // real 2026-10-22: GET /api/catalog/manufacturers/search
    searchManufacturers: build.query<ApiResponse<PageResponse<Manufacturer>>, PageParams | void>({
      query: (params) => ({
        url: '/api/catalog/manufacturers/search',
        params: { page: 0, size: 20, ...(params ?? {}) },
      }),
      providesTags: ['Manufacturer'],
    }),
    // real 2026-10-22: PUT /api/catalog/manufacturers/{id}
    updateManufacturer: build.mutation<
      ApiResponse<Manufacturer>,
      { id: string; body: Partial<Manufacturer> }
    >({
      query: ({ id, body }) => ({ url: `/api/catalog/manufacturers/${id}`, method: 'put', data: body }),
      invalidatesTags: ['Manufacturer'],
    }),
    // real 2026-10-22: PATCH /api/catalog/manufacturers/{id}/status
    patchManufacturerStatus: build.mutation<
      ApiResponse<Manufacturer>,
      { id: string; body: Record<string, unknown> }
    >({
      query: ({ id, body }) => ({
        url: `/api/catalog/manufacturers/${id}/status`,
        method: 'patch',
        data: body,
      }),
      invalidatesTags: ['Manufacturer'],
    }),
    // real 2026-10-22: DELETE /api/catalog/manufacturers/{id}
    deleteManufacturer: build.mutation<ApiResponse<unknown>, string>({
      query: (id) => ({ url: `/api/catalog/manufacturers/${id}`, method: 'delete' }),
      invalidatesTags: ['Manufacturer'],
    }),

    // ---- Collections (CollectionController @RequestMapping("/collections")) ----
    // real 2026-10-22: POST /api/catalog/collections
    createCollection: build.mutation<ApiResponse<Collection>, Partial<Collection>>({
      query: (data) => ({ url: '/api/catalog/collections', method: 'post', data }),
      invalidatesTags: ['Collection'],
    }),
    // real 2026-10-22: GET /api/catalog/collections/{id}
    getCollection: build.query<ApiResponse<Collection>, string>({
      query: (id) => ({ url: `/api/catalog/collections/${id}` }),
      providesTags: (_r, _e, id) => [{ type: 'Collection', id }],
    }),
    // real 2026-10-22: GET /api/catalog/collections
    listCollections: build.query<ApiResponse<PageResponse<Collection>>, PageParams | void>({
      query: (params) => ({
        url: '/api/catalog/collections',
        params: { page: 0, size: 20, ...(params ?? {}) },
      }),
      providesTags: ['Collection'],
    }),
    // real 2026-10-22: GET /api/catalog/collections/active
    getActiveCollections: build.query<ApiResponse<Collection[]>, void>({
      query: () => ({ url: '/api/catalog/collections/active' }),
      providesTags: ['Collection'],
    }),
    // real 2026-10-22: GET /api/catalog/collections/homepage
    getHomepageCollections: build.query<ApiResponse<Collection[]>, void>({
      query: () => ({ url: '/api/catalog/collections/homepage' }),
      providesTags: ['Collection'],
    }),
    // real 2026-10-22: PUT /api/catalog/collections/{id}
    updateCollection: build.mutation<
      ApiResponse<Collection>,
      { id: string; body: Partial<Collection> }
    >({
      query: ({ id, body }) => ({ url: `/api/catalog/collections/${id}`, method: 'put', data: body }),
      invalidatesTags: ['Collection'],
    }),
    // real 2026-10-22: PATCH /api/catalog/collections/{id}/status
    patchCollectionStatus: build.mutation<
      ApiResponse<Collection>,
      { id: string; body: Record<string, unknown> }
    >({
      query: ({ id, body }) => ({
        url: `/api/catalog/collections/${id}/status`,
        method: 'patch',
        data: body,
      }),
      invalidatesTags: ['Collection'],
    }),
    // real 2026-10-22: POST /api/catalog/collections/{id}/publish
    publishCollection: build.mutation<ApiResponse<Collection>, string>({
      query: (id) => ({ url: `/api/catalog/collections/${id}/publish`, method: 'post' }),
      invalidatesTags: ['Collection'],
    }),
    // real 2026-10-22: POST /api/catalog/collections/{id}/unpublish
    unpublishCollection: build.mutation<ApiResponse<Collection>, string>({
      query: (id) => ({ url: `/api/catalog/collections/${id}/unpublish`, method: 'post' }),
      invalidatesTags: ['Collection'],
    }),
    // real 2026-10-22: PUT /api/catalog/collections/reorder
    reorderCollections: build.mutation<ApiResponse<unknown>, unknown>({
      query: (data) => ({ url: '/api/catalog/collections/reorder', method: 'put', data }),
      invalidatesTags: ['Collection'],
    }),
    // real 2026-10-22: DELETE /api/catalog/collections/{id}
    deleteCollection: build.mutation<ApiResponse<unknown>, string>({
      query: (id) => ({ url: `/api/catalog/collections/${id}`, method: 'delete' }),
      invalidatesTags: ['Collection'],
    }),

    // ---- Synonym sets (SynonymSetController @RequestMapping("/synonym-sets")) ----
    // real 2026-10-22: POST /api/catalog/synonym-sets
    createSynonymSet: build.mutation<ApiResponse<SynonymSet>, Partial<SynonymSet>>({
      query: (data) => ({ url: '/api/catalog/synonym-sets', method: 'post', data }),
      invalidatesTags: ['Synonym'],
    }),
    // real 2026-10-22: GET /api/catalog/synonym-sets/{id}
    getSynonymSet: build.query<ApiResponse<SynonymSet>, string>({
      query: (id) => ({ url: `/api/catalog/synonym-sets/${id}` }),
      providesTags: (_r, _e, id) => [{ type: 'Synonym', id }],
    }),
    // real 2026-10-22: GET /api/catalog/synonym-sets
    listSynonymSets: build.query<ApiResponse<PageResponse<SynonymSet>>, PageParams | void>({
      query: (params) => ({
        url: '/api/catalog/synonym-sets',
        params: { page: 0, size: 20, ...(params ?? {}) },
      }),
      providesTags: ['Synonym'],
    }),
    // real 2026-10-22: PUT /api/catalog/synonym-sets/{id}
    updateSynonymSet: build.mutation<
      ApiResponse<SynonymSet>,
      { id: string; body: Partial<SynonymSet> }
    >({
      query: ({ id, body }) => ({ url: `/api/catalog/synonym-sets/${id}`, method: 'put', data: body }),
      invalidatesTags: ['Synonym'],
    }),
    // real 2026-10-22: PATCH /api/catalog/synonym-sets/{id}/status
    patchSynonymSetStatus: build.mutation<
      ApiResponse<SynonymSet>,
      { id: string; body: Record<string, unknown> }
    >({
      query: ({ id, body }) => ({
        url: `/api/catalog/synonym-sets/${id}/status`,
        method: 'patch',
        data: body,
      }),
      invalidatesTags: ['Synonym'],
    }),
    // real 2026-10-22: DELETE /api/catalog/synonym-sets/{id}
    deleteSynonymSet: build.mutation<ApiResponse<unknown>, string>({
      query: (id) => ({ url: `/api/catalog/synonym-sets/${id}`, method: 'delete' }),
      invalidatesTags: ['Synonym'],
    }),
    // real 2026-10-22: POST /api/catalog/synonym-sets/{synonymSetId}/synonyms
    createSynonym: build.mutation<
      ApiResponse<Synonym>,
      { synonymSetId: string; body: Partial<Synonym> }
    >({
      query: ({ synonymSetId, body }) => ({
        url: `/api/catalog/synonym-sets/${synonymSetId}/synonyms`,
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Synonym'],
    }),
    // real 2026-10-22: GET /api/catalog/synonym-sets/{synonymSetId}/synonyms
    listSynonyms: build.query<ApiResponse<Synonym[]>, string>({
      query: (synonymSetId) => ({ url: `/api/catalog/synonym-sets/${synonymSetId}/synonyms` }),
      providesTags: ['Synonym'],
    }),
    // real 2026-10-22: GET /api/catalog/synonym-sets/synonyms/{id}
    getSynonym: build.query<ApiResponse<Synonym>, string>({
      query: (id) => ({ url: `/api/catalog/synonym-sets/synonyms/${id}` }),
      providesTags: (_r, _e, id) => [{ type: 'Synonym', id }],
    }),
    // real 2026-10-22: PUT /api/catalog/synonym-sets/synonyms/{id}
    updateSynonym: build.mutation<ApiResponse<Synonym>, { id: string; body: Partial<Synonym> }>({
      query: ({ id, body }) => ({
        url: `/api/catalog/synonym-sets/synonyms/${id}`,
        method: 'put',
        data: body,
      }),
      invalidatesTags: ['Synonym'],
    }),
    // real 2026-10-22: DELETE /api/catalog/synonym-sets/synonyms/{id}
    deleteSynonym: build.mutation<ApiResponse<unknown>, string>({
      query: (id) => ({ url: `/api/catalog/synonym-sets/synonyms/${id}`, method: 'delete' }),
      invalidatesTags: ['Synonym'],
    }),

    // ---- Display rules (DisplayRuleController @RequestMapping("/display-rules")) ----
    // real 2026-10-22: POST /api/catalog/display-rules/collections/{collectionId}
    createDisplayRule: build.mutation<
      ApiResponse<DisplayRule>,
      { collectionId: string; body: Partial<DisplayRule> }
    >({
      query: ({ collectionId, body }) => ({
        url: `/api/catalog/display-rules/collections/${collectionId}`,
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['DisplayRule'],
    }),
    // real 2026-10-22: GET /api/catalog/display-rules/collections/{collectionId}
    listDisplayRules: build.query<ApiResponse<DisplayRule[]>, string>({
      query: (collectionId) => ({ url: `/api/catalog/display-rules/collections/${collectionId}` }),
      providesTags: ['DisplayRule'],
    }),
    // real 2026-10-22: GET /api/catalog/display-rules/collections/{collectionId}/active
    getActiveDisplayRules: build.query<ApiResponse<DisplayRule[]>, string>({
      query: (collectionId) => ({
        url: `/api/catalog/display-rules/collections/${collectionId}/active`,
      }),
      providesTags: ['DisplayRule'],
    }),
    // real 2026-10-22: GET /api/catalog/display-rules/{id}
    getDisplayRule: build.query<ApiResponse<DisplayRule>, string>({
      query: (id) => ({ url: `/api/catalog/display-rules/${id}` }),
      providesTags: (_r, _e, id) => [{ type: 'DisplayRule', id }],
    }),
    // real 2026-10-22: PUT /api/catalog/display-rules/{id}
    updateDisplayRule: build.mutation<
      ApiResponse<DisplayRule>,
      { id: string; body: Partial<DisplayRule> }
    >({
      query: ({ id, body }) => ({ url: `/api/catalog/display-rules/${id}`, method: 'put', data: body }),
      invalidatesTags: ['DisplayRule'],
    }),
    // real 2026-10-22: PATCH /api/catalog/display-rules/{id}/status
    patchDisplayRuleStatus: build.mutation<
      ApiResponse<DisplayRule>,
      { id: string; body: Record<string, unknown> }
    >({
      query: ({ id, body }) => ({
        url: `/api/catalog/display-rules/${id}/status`,
        method: 'patch',
        data: body,
      }),
      invalidatesTags: ['DisplayRule'],
    }),
    // real 2026-10-22: DELETE /api/catalog/display-rules/{id}
    deleteDisplayRule: build.mutation<ApiResponse<unknown>, string>({
      query: (id) => ({ url: `/api/catalog/display-rules/${id}`, method: 'delete' }),
      invalidatesTags: ['DisplayRule'],
    }),
  }),
});

export const {
  useGetCategoriesQuery,
  useGetCategoryTreeQuery,
  useGetCategoryQuery,
  useGetCategoryChildrenQuery,
  useCreateCategoryMutation,
  useUpdateCategoryMutation,
  useDeleteCategoryMutation,
  useListBrandsQuery,
  useGetBrandQuery,
  useGetActiveBrandsQuery,
  useSearchBrandsQuery,
  useCreateBrandMutation,
  useUpdateBrandMutation,
  useDeleteBrandMutation,
  usePatchBrandStatusMutation,
  useCreateManufacturerMutation,
  useGetManufacturerQuery,
  useListManufacturersQuery,
  useGetActiveManufacturersQuery,
  useSearchManufacturersQuery,
  useUpdateManufacturerMutation,
  usePatchManufacturerStatusMutation,
  useDeleteManufacturerMutation,
  useCreateCollectionMutation,
  useGetCollectionQuery,
  useListCollectionsQuery,
  useGetActiveCollectionsQuery,
  useGetHomepageCollectionsQuery,
  useUpdateCollectionMutation,
  usePatchCollectionStatusMutation,
  usePublishCollectionMutation,
  useUnpublishCollectionMutation,
  useReorderCollectionsMutation,
  useDeleteCollectionMutation,
  useCreateSynonymSetMutation,
  useGetSynonymSetQuery,
  useListSynonymSetsQuery,
  useUpdateSynonymSetMutation,
  usePatchSynonymSetStatusMutation,
  useDeleteSynonymSetMutation,
  useCreateSynonymMutation,
  useListSynonymsQuery,
  useGetSynonymQuery,
  useUpdateSynonymMutation,
  useDeleteSynonymMutation,
  useCreateDisplayRuleMutation,
  useListDisplayRulesQuery,
  useGetActiveDisplayRulesQuery,
  useGetDisplayRuleQuery,
  useUpdateDisplayRuleMutation,
  usePatchDisplayRuleStatusMutation,
  useDeleteDisplayRuleMutation,
} = catalogApi;
