import { createApi } from '@reduxjs/toolkit/query/react';
import { axiosBaseQuery, type ApiResponse, type PageResponse } from '../../app/api';

// FE_API_CONTRACT §4 — Inventory dual paths. JWT tu dong qua axios interceptor.
// Mọi endpoint giu comment `// real 2026-10-22: <METHOD path>` verbatim theo contract.

export interface InventoryRecord {
  id: string;
  enterpriseId?: string;
  warehouseId?: string;
  locationId?: string;
  sku?: string;
  quantity?: number;
  availableQuantity?: number;
  reservedQuantity?: number;
  [key: string]: unknown;
}

export interface InventoryMovement {
  id: string;
  sku?: string;
  type?: string;
  quantity?: number;
  createdAt?: string;
  [key: string]: unknown;
}

export interface Warehouse {
  id: string;
  code?: string;
  name?: string;
  status?: string;
  [key: string]: unknown;
}

export interface StockLocation {
  id: string;
  code?: string;
  name?: string;
  warehouseId?: string;
  status?: string;
  [key: string]: unknown;
}

export interface SupplierReceipt {
  id: string;
  warehouseId?: string;
  status?: string;
  [key: string]: unknown;
}

export interface StockTransfer {
  id: string;
  status?: string;
  [key: string]: unknown;
}

export const inventoryApi = createApi({
  reducerPath: 'inventoryApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Inventory', 'Warehouse', 'Location', 'Transfer', 'Receipt'],
  endpoints: (build) => ({
    // ---- 4a. Special routes /api/v1/** ----

    // real 2026-10-22: GET /api/v1/warehouses/{warehouseId}/receipts
    listWarehouseReceipts: build.query<
      ApiResponse<SupplierReceipt[]>,
      { warehouseId: string }
    >({
      query: ({ warehouseId }) => ({
        url: `/api/v1/warehouses/${warehouseId}/receipts`,
      }),
      providesTags: ['Receipt'],
    }),

    // real 2026-10-22: POST /api/v1/warehouses/{warehouseId}/receipts
    createWarehouseReceipt: build.mutation<
      ApiResponse<SupplierReceipt>,
      { warehouseId: string; body: unknown }
    >({
      query: ({ warehouseId, body }) => ({
        url: `/api/v1/warehouses/${warehouseId}/receipts`,
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Receipt'],
    }),

    // real 2026-10-22: PATCH /api/v1/receipts/{receiptId}/actuals
    updateReceiptActuals: build.mutation<
      ApiResponse<SupplierReceipt>,
      { receiptId: string; body: unknown }
    >({
      query: ({ receiptId, body }) => ({
        url: `/api/v1/receipts/${receiptId}/actuals`,
        method: 'patch',
        data: body,
      }),
      invalidatesTags: ['Receipt'],
    }),

    // ---- 4b. Stock — doubled prefix /api/inventory/inventory/... ----

    // real 2026-10-22: GET /api/inventory/inventory/{id}
    getInventoryById: build.query<ApiResponse<InventoryRecord>, string>({
      query: (id) => ({ url: `/api/inventory/inventory/${id}` }),
      providesTags: (_r, _e, id) => [{ type: 'Inventory', id }],
    }),

    // real 2026-10-22: GET /api/inventory/inventory?enterpriseId=&warehouseId=&locationId=&sku=&page=&size=
    listInventories: build.query<
      ApiResponse<PageResponse<InventoryRecord>>,
      {
        enterpriseId?: string;
        warehouseId?: string;
        locationId?: string;
        sku?: string;
        page?: number;
        size?: number;
      } | void
    >({
      query: (params) => ({
        url: '/api/inventory/inventory',
        params: { page: 0, size: 20, ...(params ?? {}) },
      }),
      providesTags: ['Inventory'],
    }),

    // real 2026-10-22: GET /api/inventory/inventory/sku/{sku}
    getInventoryBySku: build.query<ApiResponse<InventoryRecord[]>, string>({
      query: (sku) => ({ url: `/api/inventory/inventory/sku/${sku}` }),
      providesTags: ['Inventory'],
    }),

    // real 2026-10-22: GET /api/inventory/inventory/warehouse/{warehouseId}
    getInventoryByWarehouse: build.query<
      ApiResponse<InventoryRecord[]>,
      string
    >({
      query: (warehouseId) => ({
        url: `/api/inventory/inventory/warehouse/${warehouseId}`,
      }),
      providesTags: ['Inventory'],
    }),

    // real 2026-10-22: GET /api/inventory/inventory/location/{locationId}
    getInventoryByLocation: build.query<
      ApiResponse<InventoryRecord[]>,
      string
    >({
      query: (locationId) => ({
        url: `/api/inventory/inventory/location/${locationId}`,
      }),
      providesTags: ['Inventory'],
    }),

    // real 2026-10-22: GET /api/inventory/inventory/available
    getAvailableStock: build.query<
      ApiResponse<InventoryRecord[]>,
      { sku?: string; warehouseId?: string } | void
    >({
      query: (params) => ({
        url: '/api/inventory/inventory/available',
        params: params ?? {},
      }),
      providesTags: ['Inventory'],
    }),

    // real 2026-10-22: GET /api/inventory/inventory/movements
    listMovements: build.query<
      ApiResponse<PageResponse<InventoryMovement>>,
      { sku?: string; warehouseId?: string; page?: number; size?: number } | void
    >({
      query: (params) => ({
        url: '/api/inventory/inventory/movements',
        params: { page: 0, size: 20, ...(params ?? {}) },
      }),
      providesTags: ['Inventory'],
    }),

    // real 2026-10-22: POST /api/inventory/inventory
    createInventory: build.mutation<ApiResponse<InventoryRecord>, unknown>({
      query: (body) => ({
        url: '/api/inventory/inventory',
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Inventory'],
    }),

    // real 2026-10-22: POST /api/inventory/inventory/batch
    createInventoryBatch: build.mutation<ApiResponse<InventoryRecord[]>, unknown>({
      query: (body) => ({
        url: '/api/inventory/inventory/batch',
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Inventory'],
    }),

    // real 2026-10-22: POST /api/inventory/inventory/receive-stock
    receiveStock: build.mutation<ApiResponse<InventoryRecord>, unknown>({
      query: (body) => ({
        url: '/api/inventory/inventory/receive-stock',
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Inventory', 'Receipt'],
    }),

    // real 2026-10-22: POST /api/inventory/inventory/returns/receive
    receiveReturn: build.mutation<ApiResponse<InventoryRecord>, unknown>({
      query: (body) => ({
        url: '/api/inventory/inventory/returns/receive',
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Inventory'],
    }),

    // real 2026-10-22: POST /api/inventory/inventory/returns/{receiptId}/inspect
    inspectReturn: build.mutation<
      ApiResponse<InventoryRecord>,
      { receiptId: string; body: unknown }
    >({
      query: ({ receiptId, body }) => ({
        url: `/api/inventory/inventory/returns/${receiptId}/inspect`,
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Inventory'],
    }),

    // real 2026-10-22: POST /api/inventory/inventory/receive
    receiveInventory: build.mutation<ApiResponse<InventoryRecord>, unknown>({
      query: (body) => ({
        url: '/api/inventory/inventory/receive',
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Inventory'],
    }),

    // real 2026-10-22: POST /api/inventory/inventory/reserve
    reserveInventory: build.mutation<ApiResponse<InventoryRecord>, unknown>({
      query: (body) => ({
        url: '/api/inventory/inventory/reserve',
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Inventory'],
    }),

    // real 2026-10-22: POST /api/inventory/inventory/release
    releaseInventory: build.mutation<ApiResponse<InventoryRecord>, unknown>({
      query: (body) => ({
        url: '/api/inventory/inventory/release',
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Inventory'],
    }),

    // real 2026-10-22: POST /api/inventory/inventory/issue
    issueInventory: build.mutation<ApiResponse<InventoryRecord>, unknown>({
      query: (body) => ({
        url: '/api/inventory/inventory/issue',
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Inventory'],
    }),

    // real 2026-10-22: POST /api/inventory/inventory/adjust
    adjustInventory: build.mutation<ApiResponse<InventoryRecord>, unknown>({
      query: (body) => ({
        url: '/api/inventory/inventory/adjust',
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Inventory'],
    }),

    // real 2026-10-22: POST /api/inventory/inventory/transfer
    transferInventory: build.mutation<ApiResponse<StockTransfer>, unknown>({
      query: (body) => ({
        url: '/api/inventory/inventory/transfer',
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Inventory', 'Transfer'],
    }),

    // real 2026-10-22: POST /api/inventory/inventory/transfers
    createTransfer: build.mutation<ApiResponse<StockTransfer>, unknown>({
      query: (body) => ({
        url: '/api/inventory/inventory/transfers',
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Transfer'],
    }),

    // real 2026-10-22: POST /api/inventory/inventory/transfers/{transferId}/dispatch
    dispatchTransfer: build.mutation<
      ApiResponse<StockTransfer>,
      { transferId: string; body?: unknown }
    >({
      query: ({ transferId, body }) => ({
        url: `/api/inventory/inventory/transfers/${transferId}/dispatch`,
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Transfer', 'Inventory'],
    }),

    // real 2026-10-22: POST /api/inventory/inventory/transfers/{transferId}/receive
    receiveTransfer: build.mutation<
      ApiResponse<StockTransfer>,
      { transferId: string; body?: unknown }
    >({
      query: ({ transferId, body }) => ({
        url: `/api/inventory/inventory/transfers/${transferId}/receive`,
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Transfer', 'Inventory'],
    }),

    // ---- Warehouses /api/inventory/warehouses ----

    // real 2026-10-22: POST /api/inventory/warehouses
    createWarehouse: build.mutation<ApiResponse<Warehouse>, unknown>({
      query: (body) => ({
        url: '/api/inventory/warehouses',
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Warehouse'],
    }),

    // real 2026-10-22: GET /api/inventory/warehouses
    listWarehouses: build.query<
      ApiResponse<PageResponse<Warehouse>>,
      { page?: number; size?: number } | void
    >({
      query: (params) => ({
        url: '/api/inventory/warehouses',
        params: { page: 0, size: 20, ...(params ?? {}) },
      }),
      providesTags: ['Warehouse'],
    }),

    // real 2026-10-22: GET /api/inventory/warehouses/{warehouseId}
    getWarehouseById: build.query<ApiResponse<Warehouse>, string>({
      query: (warehouseId) => ({
        url: `/api/inventory/warehouses/${warehouseId}`,
      }),
      providesTags: (_r, _e, id) => [{ type: 'Warehouse', id }],
    }),

    // real 2026-10-22: PUT /api/inventory/warehouses/{warehouseId}
    updateWarehouse: build.mutation<
      ApiResponse<Warehouse>,
      { warehouseId: string; body: unknown }
    >({
      query: ({ warehouseId, body }) => ({
        url: `/api/inventory/warehouses/${warehouseId}`,
        method: 'put',
        data: body,
      }),
      invalidatesTags: ['Warehouse'],
    }),

    // real 2026-10-22: PATCH /api/inventory/warehouses/{warehouseId}/status
    updateWarehouseStatus: build.mutation<
      ApiResponse<Warehouse>,
      { warehouseId: string; body: unknown }
    >({
      query: ({ warehouseId, body }) => ({
        url: `/api/inventory/warehouses/${warehouseId}/status`,
        method: 'patch',
        data: body,
      }),
      invalidatesTags: ['Warehouse'],
    }),

    // ---- Locations — doubled /api/inventory/inventory/locations ----

    // real 2026-10-22: POST /api/inventory/inventory/locations
    createLocation: build.mutation<ApiResponse<StockLocation>, unknown>({
      query: (body) => ({
        url: '/api/inventory/inventory/locations',
        method: 'post',
        data: body,
      }),
      invalidatesTags: ['Location'],
    }),

    // real 2026-10-22: GET /api/inventory/inventory/locations
    listLocations: build.query<
      ApiResponse<PageResponse<StockLocation>>,
      { warehouseId?: string; page?: number; size?: number } | void
    >({
      query: (params) => ({
        url: '/api/inventory/inventory/locations',
        params: { page: 0, size: 20, ...(params ?? {}) },
      }),
      providesTags: ['Location'],
    }),

    // real 2026-10-22: GET /api/inventory/inventory/locations/{locationId}
    getLocationById: build.query<ApiResponse<StockLocation>, string>({
      query: (locationId) => ({
        url: `/api/inventory/inventory/locations/${locationId}`,
      }),
      providesTags: (_r, _e, id) => [{ type: 'Location', id }],
    }),

    // real 2026-10-22: PUT /api/inventory/inventory/locations/{locationId}
    updateLocation: build.mutation<
      ApiResponse<StockLocation>,
      { locationId: string; body: unknown }
    >({
      query: ({ locationId, body }) => ({
        url: `/api/inventory/inventory/locations/${locationId}`,
        method: 'put',
        data: body,
      }),
      invalidatesTags: ['Location'],
    }),

    // real 2026-10-22: PATCH /api/inventory/inventory/locations/{locationId}/status
    updateLocationStatus: build.mutation<
      ApiResponse<StockLocation>,
      { locationId: string; body: unknown }
    >({
      query: ({ locationId, body }) => ({
        url: `/api/inventory/inventory/locations/${locationId}/status`,
        method: 'patch',
        data: body,
      }),
      invalidatesTags: ['Location'],
    }),

    // ---- Receipt lifecycle /api/inventory/receipts/... ----

    // real 2026-10-22: GET /api/inventory/receipts/{receiptId}
    getReceiptById: build.query<ApiResponse<SupplierReceipt>, string>({
      query: (receiptId) => ({
        url: `/api/inventory/receipts/${receiptId}`,
      }),
      providesTags: (_r, _e, id) => [{ type: 'Receipt', id }],
    }),

    // real 2026-10-22: POST /api/inventory/receipts/{receiptId}/submit
    submitReceipt: build.mutation<ApiResponse<SupplierReceipt>, string>({
      query: (receiptId) => ({
        url: `/api/inventory/receipts/${receiptId}/submit`,
        method: 'post',
      }),
      invalidatesTags: ['Receipt'],
    }),

    // real 2026-10-22: POST /api/inventory/receipts/{receiptId}/approve
    approveReceipt: build.mutation<ApiResponse<SupplierReceipt>, string>({
      query: (receiptId) => ({
        url: `/api/inventory/receipts/${receiptId}/approve`,
        method: 'post',
      }),
      invalidatesTags: ['Receipt'],
    }),

    // real 2026-10-22: POST /api/inventory/receipts/{receiptId}/post
    postReceipt: build.mutation<ApiResponse<SupplierReceipt>, string>({
      query: (receiptId) => ({
        url: `/api/inventory/receipts/${receiptId}/post`,
        method: 'post',
      }),
      invalidatesTags: ['Receipt', 'Inventory'],
    }),
  }),
});

export const {
  useListWarehouseReceiptsQuery,
  useCreateWarehouseReceiptMutation,
  useUpdateReceiptActualsMutation,
  useGetInventoryByIdQuery,
  useListInventoriesQuery,
  useGetInventoryBySkuQuery,
  useGetInventoryByWarehouseQuery,
  useGetInventoryByLocationQuery,
  useGetAvailableStockQuery,
  useListMovementsQuery,
  useCreateInventoryMutation,
  useCreateInventoryBatchMutation,
  useReceiveStockMutation,
  useReceiveReturnMutation,
  useInspectReturnMutation,
  useReceiveInventoryMutation,
  useReserveInventoryMutation,
  useReleaseInventoryMutation,
  useIssueInventoryMutation,
  useAdjustInventoryMutation,
  useTransferInventoryMutation,
  useCreateTransferMutation,
  useDispatchTransferMutation,
  useReceiveTransferMutation,
  useCreateWarehouseMutation,
  useListWarehousesQuery,
  useGetWarehouseByIdQuery,
  useUpdateWarehouseMutation,
  useUpdateWarehouseStatusMutation,
  useCreateLocationMutation,
  useListLocationsQuery,
  useGetLocationByIdQuery,
  useUpdateLocationMutation,
  useUpdateLocationStatusMutation,
  useGetReceiptByIdQuery,
  useSubmitReceiptMutation,
  useApproveReceiptMutation,
  usePostReceiptMutation,
} = inventoryApi;
