import { useMemo, useState } from 'react';
import { App, Button, Card, Descriptions, Drawer, Form, Input, Modal, Popconfirm, Space, Transfer, Typography } from 'antd';
import { KeyOutlined, SafetyOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import {
  useListIamRolesQuery,
  useCreateIamRoleMutation,
  useDeleteIamRoleMutation,
  useSetRolePermissionsMutation,
  useListPermissionsQuery,
  type IamRole,
} from '../features/admin/iamApi';
import {
  ApiErrorAlert,
  DataTable,
  ListSkeleton,
  PageHeader,
  SummaryRow,
  TableEmpty,
} from '../components/ui';

// /admin/iam-roles — RequireRole ADMIN. Vai tro DB (khac realm roles Keycloak
// o /admin/roles): CRUD + gan permission qua Transfer (PUT .../permissions).
// Vai tro TENANT_ADMIN la goc he thong — chan xoa.
const PROTECTED_ROLES = ['TENANT_ADMIN'];

export default function AdminIamRolesPage() {
  const { message } = App.useApp();
  const rolesQuery = useListIamRolesQuery();
  const permsQuery = useListPermissionsQuery(undefined);
  const [createRole, createState] = useCreateIamRoleMutation();
  const [deleteRole] = useDeleteIamRoleMutation();
  const [setPermissions, setPermState] = useSetRolePermissionsMutation();
  const [createOpen, setCreateOpen] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [createForm] = Form.useForm();

  const roles: IamRole[] = rolesQuery.data?.items ?? [];
  const allPerms = permsQuery.data?.items ?? [];
  const selected = roles.find((r) => r.id === selectedId) ?? null;
  const selectedPermIds = useMemo(() => (selected?.permissionIds ?? []).map(String), [selected]);

  const totalGrants = roles.reduce((s, r) => s + (r.permissionIds?.length ?? 0), 0);
  const mutationError =
    (createState.error as { message?: string } | undefined)?.message ??
    (setPermState.error as { message?: string } | undefined)?.message;

  const onTransferChange = async (targetKeys: React.Key[]) => {
    if (!selected) return;
    try {
      await setPermissions({ id: selected.id, permissionIds: targetKeys.map(String) }).unwrap();
      message.success(`Đã cập nhật ${targetKeys.length} permission cho vai trò ${selected.code}`);
    } catch {
      message.error('Cập nhật permission thất bại');
    }
  };

  const columns: ColumnsType<IamRole> = [
    {
      title: 'Mã', dataIndex: 'code', key: 'code',
      render: (v: string) => <Typography.Text code>{v}</Typography.Text>,
    },
    { title: 'Tên', dataIndex: 'name', key: 'name' },
    {
      title: 'Permissions', key: 'perms', width: 130,
      render: (_: unknown, r: IamRole) => (
        <span style={{ fontVariantNumeric: 'tabular-nums' }}>{r.permissionIds?.length ?? 0}</span>
      ),
    },
    {
      title: 'Thao tác', key: 'action', width: 220,
      render: (_: unknown, r: IamRole) => (
        <Space>
          <Button size="small" type="link" onClick={() => setSelectedId(r.id)}>Phân quyền</Button>
          <Popconfirm
            title={`Xóa vai trò ${r.code}?`}
            description="Các nhóm đang dùng vai trò này sẽ mất quyền tương ứng."
            okText="Xóa" okType="danger" cancelText="Hủy"
            onConfirm={async () => {
              try { await deleteRole(r.id).unwrap(); message.success(`Đã xóa vai trò ${r.code}`); }
              catch { message.error('Xóa vai trò thất bại'); }
            }}
          >
            <Button size="small" type="link" danger disabled={PROTECTED_ROLES.includes(r.code)}>Xóa</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Vai trò (DB)"
        subTitle="Vai trò nội bộ Menu→Action→Permission — khác realm roles Keycloak"
        extra={<Button type="primary" onClick={() => setCreateOpen(true)}>Tạo vai trò</Button>}
      />
      <ApiErrorAlert message={mutationError ?? (rolesQuery.error as { message?: string } | undefined)?.message ?? null} />
      <SummaryRow
        items={[
          { label: 'Số vai trò', value: roles.length, icon: <KeyOutlined /> },
          { label: 'Tổng lượt cấp permission', value: totalGrants, icon: <SafetyOutlined />, color: '#0F4C81', hint: 'vai trò × permission' },
        ]}
      />
      {rolesQuery.isLoading ? <ListSkeleton /> : roles.length === 0 ? (
        <Card><TableEmpty illustration="roles" description="Chưa có vai trò DB" actionText="Tạo vai trò đầu tiên" onAction={() => setCreateOpen(true)} /></Card>
      ) : (
        <DataTable<IamRole> columns={columns} data={roles} loading={rolesQuery.isFetching} rowKey="id" />
      )}

      <Drawer
        title={selected ? `Phân quyền — ${selected.code}` : 'Phân quyền'}
        width={560} open={!!selected} onClose={() => setSelectedId(null)} destroyOnClose
      >
        {permsQuery.isLoading ? <ListSkeleton /> : permsQuery.isError ? (
          <TableEmpty description="Không tải được permissions" actionText="Thử lại" onAction={() => permsQuery.refetch()} />
        ) : (
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="Vai trò">{selected?.code}</Descriptions.Item>
              <Descriptions.Item label="Đã cấp">{selectedPermIds.length}</Descriptions.Item>
            </Descriptions>
            <Transfer
              dataSource={allPerms.map((p) => ({ key: String(p.id), title: p.code }))}
              targetKeys={selectedPermIds}
              onChange={onTransferChange}
              render={(item) => <Typography.Text code style={{ fontSize: 12 }}>{item.title}</Typography.Text>}
              listStyle={{ width: '45%', height: 340 }}
              showSearch
              filterOption={(input, option) => (option.title ?? '').toLowerCase().includes(input.toLowerCase())}
              titles={['Chưa cấp', 'Đã cấp']}
              locale={{ searchPlaceholder: 'Tìm permission (MENU.ACTION)' }}
            />
          </Space>
        )}
      </Drawer>

      <Modal title="Tạo vai trò" open={createOpen} onCancel={() => setCreateOpen(false)} confirmLoading={createState.isLoading}
        okText="Tạo" cancelText="Hủy" destroyOnClose
        onOk={async () => {
          try {
            const v = await createForm.validateFields();
            await createRole({ code: v.code.trim(), name: v.name.trim(), description: v.description?.trim() || undefined }).unwrap();
            message.success(`Đã tạo vai trò ${v.code.trim()}`); setCreateOpen(false); createForm.resetFields();
          } catch { if (createState.error) message.error('Tạo vai trò thất bại'); }
        }}>
        <Form form={createForm} layout="vertical" preserve={false}>
          <Form.Item name="code" label="Mã vai trò" rules={[{ required: true, message: 'Nhập mã vai trò' }, { pattern: /^[A-Z0-9_]+$/, message: 'Chỉ chữ hoa/số/_' }]}>
            <Input placeholder="VD: SHOP_MANAGER" />
          </Form.Item>
          <Form.Item name="name" label="Tên" rules={[{ required: true, message: 'Nhập tên vai trò' }]}>
            <Input placeholder="VD: Quản lý gian hàng" />
          </Form.Item>
          <Form.Item name="description" label="Mô tả" rules={[{ max: 255, message: 'Tối đa 255 ký tự' }]}>
            <Input.TextArea rows={2} placeholder="Mô tả (không bắt buộc)" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
