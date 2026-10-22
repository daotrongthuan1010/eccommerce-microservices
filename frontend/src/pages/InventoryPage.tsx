import { useState } from 'react';
import { Button, Input } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  useGetAvailableStockQuery,
  useListMovementsQuery,
  type InventoryMovement,
  type InventoryRecord,
} from '../features/inventory/inventoryApi';
import {
  ApiErrorAlert,
  DataTable,
  FilterBar,
  ListSkeleton,
  PageHeader,
} from '../components/ui';

// Wave 3.2 — tra cuu ton kha dung qua doubled path verbatim
// GET /api/inventory/inventory/available + lich su dich chuyen
// GET /api/inventory/inventory/movements.
export default function InventoryPage() {
  const [sku, setSku] = useState('');
  const [warehouseId, setWarehouseId] = useState('');
  const [applied, setApplied] = useState<{ sku?: string; warehouseId?: string }>({});
  const [movePage, setMovePage] = useState(0);

  const available = useGetAvailableStockQuery(applied);
  const movements = useListMovementsQuery({ page: movePage, size: 20 });

  const stockColumns: ColumnsType<InventoryRecord> = [
    { title: 'SKU', dataIndex: 'sku' },
    { title: 'Kho', dataIndex: 'warehouseId' },
    { title: 'Vị trí', dataIndex: 'locationId' },
    { title: 'Khả dụng', dataIndex: 'availableQuantity' },
    { title: 'Đã giữ', dataIndex: 'reservedQuantity' },
  ];

  const moveColumns: ColumnsType<InventoryMovement> = [
    { title: 'ID', dataIndex: 'id' },
    { title: 'SKU', dataIndex: 'sku' },
    { title: 'Loại', dataIndex: 'type' },
    { title: 'Số lượng', dataIndex: 'quantity' },
    { title: 'Thời gian', dataIndex: 'createdAt' },
  ];

  const rows: InventoryRecord[] = available.data?.data ?? [];
  const movePageData = movements.data?.data;

  return (
    <div>
      <PageHeader title="Tồn kho" subTitle="Tra cứu khả dụng + lịch sử dịch chuyển" />
      <FilterBar>
        <Input
          placeholder="SKU"
          value={sku}
          onChange={(e) => setSku(e.target.value)}
          style={{ width: 200 }}
        />
        <Input
          placeholder="Warehouse ID"
          value={warehouseId}
          onChange={(e) => setWarehouseId(e.target.value)}
          style={{ width: 240 }}
        />
        <Button
          type="primary"
          onClick={() =>
            setApplied({
              ...(sku.trim() ? { sku: sku.trim() } : {}),
              ...(warehouseId.trim() ? { warehouseId: warehouseId.trim() } : {}),
            })
          }
        >
          Tra cứu
        </Button>
      </FilterBar>
      <ApiErrorAlert
        message={
          (available.error as { message?: string } | undefined)?.message ??
          (movements.error as { message?: string } | undefined)?.message
        }
      />
      {available.isLoading ? (
        <ListSkeleton />
      ) : (
        <DataTable<InventoryRecord> columns={stockColumns} data={rows} rowKey="id" />
      )}
      <PageHeader title="Lịch sử dịch chuyển" />
      {movements.isLoading ? (
        <ListSkeleton />
      ) : (
        <DataTable<InventoryMovement>
          columns={moveColumns}
          data={movePageData?.content ?? []}
          total={movePageData?.totalElements}
          page={movePage}
          onPageChange={setMovePage}
          rowKey="id"
        />
      )}
    </div>
  );
}
