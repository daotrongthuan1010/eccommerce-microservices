import { useState } from 'react';
import { Button, Input, Space, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  useApproveReceiptMutation,
  useCreateWarehouseReceiptMutation,
  useListWarehouseReceiptsQuery,
  usePostReceiptMutation,
  useSubmitReceiptMutation,
  useUpdateReceiptActualsMutation,
  type SupplierReceipt,
} from '../features/inventory/inventoryApi';
import {
  ApiErrorAlert,
  DataTable,
  FilterBar,
  ListSkeleton,
  PageHeader,
  StatusTag,
} from '../components/ui';

// Wave 3.1 — phieu nhap qua special routes /api/v1/warehouses/{id}/receipts
// (GET list, POST tao), PATCH /api/v1/receipts/{id}/actuals + lifecycle
// POST /api/inventory/receipts/{id}/submit|approve|post.
export default function ReceiptsPage() {
  const [warehouseId, setWarehouseId] = useState('');
  const [appliedWarehouseId, setAppliedWarehouseId] = useState('');
  const [newReceipt, setNewReceipt] = useState('');

  const list = useListWarehouseReceiptsQuery(
    { warehouseId: appliedWarehouseId },
    { skip: !appliedWarehouseId },
  );
  const [createReceipt, createState] = useCreateWarehouseReceiptMutation();
  const [updateActuals] = useUpdateReceiptActualsMutation();
  const [submitReceipt] = useSubmitReceiptMutation();
  const [approveReceipt] = useApproveReceiptMutation();
  const [postReceipt] = usePostReceiptMutation();

  const run = async (fn: () => Promise<unknown>, ok: string) => {
    try {
      await fn();
      message.success(ok);
    } catch {
      message.error('Thao tác thất bại');
    }
  };

  const columns: ColumnsType<SupplierReceipt> = [
    { title: 'ID', dataIndex: 'id' },
    { title: 'Kho', dataIndex: 'warehouseId' },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      render: (s: string) => <StatusTag status={s ?? 'DRAFT'} />,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, r) => (
        <Space>
          <Button
            size="small"
            onClick={() => run(() => submitReceipt(r.id).unwrap(), 'Đã submit')}
          >
            Submit
          </Button>
          <Button
            size="small"
            onClick={() => run(() => approveReceipt(r.id).unwrap(), 'Đã approve')}
          >
            Approve
          </Button>
          <Button
            size="small"
            onClick={() => run(() => postReceipt(r.id).unwrap(), 'Đã post')}
          >
            Post
          </Button>
          <Button
            size="small"
            onClick={() =>
              run(
                () => updateActuals({ receiptId: r.id, body: {} }).unwrap(),
                'Đã cập nhật thực nhận',
              )
            }
          >
            Actuals
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <PageHeader title="Phiếu nhập" subTitle="Tạo + duyệt + post phiếu nhập kho" />
      <FilterBar>
        <Input
          placeholder="Warehouse ID"
          value={warehouseId}
          onChange={(e) => setWarehouseId(e.target.value)}
          style={{ width: 240 }}
        />
        <Button type="primary" onClick={() => setAppliedWarehouseId(warehouseId.trim())}>
          Xem phiếu
        </Button>
        <Input
          placeholder='JSON tạo phiếu, VD {"supplier":"NCC-A"}'
          value={newReceipt}
          onChange={(e) => setNewReceipt(e.target.value)}
          style={{ width: 320 }}
        />
        <Button
          loading={createState.isLoading}
          disabled={!appliedWarehouseId}
          onClick={() =>
            run(async () => {
              let body: unknown = {};
              try {
                body = newReceipt.trim() ? JSON.parse(newReceipt) : {};
              } catch {
                throw new Error('JSON không hợp lệ');
              }
              await createReceipt({ warehouseId: appliedWarehouseId, body }).unwrap();
              setNewReceipt('');
            }, 'Đã tạo phiếu nhập')
          }
        >
          Tạo phiếu
        </Button>
      </FilterBar>
      <ApiErrorAlert
        message={(list.error as { message?: string } | undefined)?.message}
      />
      {!appliedWarehouseId ? (
        <p>Nhập Warehouse ID để xem danh sách phiếu nhập.</p>
      ) : list.isLoading ? (
        <ListSkeleton />
      ) : (
        <DataTable<SupplierReceipt>
          columns={columns}
          data={list.data?.data ?? []}
          rowKey="id"
        />
      )}
    </div>
  );
}
