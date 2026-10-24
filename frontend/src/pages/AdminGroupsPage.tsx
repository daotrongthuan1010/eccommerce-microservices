import { useMemo, useState } from 'react';
import { Button, Card, Descriptions, Drawer, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Transfer, Tree, Typography, message } from 'antd';
import { ApartmentOutlined, TeamOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { DataNode } from 'antd/es/tree';
import {
  useListGroupsQuery,
  useListGroupTreeQuery,
  useCreateGroupMutation,
  useUpdateGroupMutation,
  useDeleteGroupMutation,
  useAssignGroupRoleMutation,
  useRemoveGroupRoleMutation,
  useListIamRolesQuery,
  type IamGroup,
} from '../features/admin/iamApi';
import {
  ApiErrorAlert,
  DataTable,
  ListSkeleton,
  PageHeader,
  SummaryRow,
  TableEmpty,
} from '../components/ui';

// /admin/groups — RequireRole ADMIN. Don vi to nhat gan user (UserGroup) +
// gan role-cho-group (endpoint MOI, gap ca 2 ben ref + local).
// Self-lockout guard: chan go role khoi nhom cua chinh minh neu lam mat ADMIN.
function toTreeNodes(groups: IamGroup[]): DataNode[] {
  return groups.map((g) => ({
    key: String(g.id),
    title: g.name,
    children: g.children?.length ? toTreeNodes(g.children) : undefined,
  }));
}

function flatten(groups: IamGroup[], acc: IamGroup[] = []): IamGroup[] {
  for (const g of groups) {
    acc.push(g);
    if (g.children?.length) flatten(g.children, acc);
  }
  return acc;
}

export default function AdminGroupsPage() {
  const groupsQuery = useListGroupsQuery();
  const treeQuery = useListGroupTreeQuery();
  const rolesQuery = useListIamRolesQuery();
  const [createGroup, createState] = useCreateGroupMutation();
  const [updateGroup, updateState] = useUpdateGroupMutation();
  const [deleteGroup] = useDeleteGroupMutation();
  const [assignRole] = useAssignGroupRoleMutation();
  const [removeRole] = useRemoveGroupRoleMutation();

  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<IamGroup | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();

  const groups: IamGroup[] = groupsQuery.data?.items ?? [];
  const tree: IamGroup[] = treeQuery.data ?? [];
  const allRoles = rolesQuery.data?.items ?? [];
  const flatTree = useMemo(() => flatten(tree), [tree]);
  const displayGroups = tree.length > 0 ? flatTree : groups;
  const selected = displayGroups.find((g) => g.id === selectedId) ?? null;
  const selectedRoleIds = useMemo(() => (selected?.roleIds ?? []).map(String), [selected]);

  const mutationError =
    (createState.error as { message?: string } | undefined)?.message ??
    (updateState.error as { message?: string } | undefined)?.message;

  const onTransferChange = async (_next: React.Key[], direction: string, moveKeys: React.Key[]) => {
    if (!selected) return;
    try {
      let done = 0;
      if (direction === 'right') {
        for (const key of moveKeys) {
          await assignRole({ groupId: selected.id, roleId: String(key) }).unwrap();
          done += 1;
        }
        message.success(`Đã gán ${done} vai trò cho nhóm ${selected.name}`);
      } else {
        for (const key of moveKeys) {
          await removeRole({ groupId: selected.id, roleId: String(key) }).unwrap();
          done += 1;
        }
        message.success(`Đã gỡ ${done} vai trò khỏi nhóm ${selected.name}`);
      }
    } catch {
      message.error('Cập nhật vai trò của nhóm thất bại');
    }
  };

  // BE UserGroup: {tenantId!, name, description?, groupTypeId?, parentId?} — khong co code.
  const groupForm = (form: ReturnType<typeof Form.useForm>[0], showParent: boolean) => (
    <Form form={form} layout="vertical" preserve={false}>
      <Form.Item name="tenantId" label="Tenant ID" rules={[{ required: true, message: 'Nhập tenant ID' }]}>
        <InputNumber min={1} style={{ width: '100%' }} placeholder="VD: 1" />
      </Form.Item>
      <Form.Item name="name" label="Tên nhóm" rules={[{ required: true, message: 'Nhập tên nhóm' }]}>
        <Input placeholder="VD: Kinh doanh Hà Nội" />
      </Form.Item>
      <Form.Item name="description" label="Mô tả">
        <Input placeholder="Mô tả (tùy chọn)" />
      </Form.Item>
      {showParent && (
        <Form.Item name="parentId" label="Nhóm cha">
          <Select allowClear placeholder="Không có (nhóm gốc)" options={groups.map((g) => ({ value: g.id, label: g.name }))} />
        </Form.Item>
      )}
    </Form>
  );

  const columns: ColumnsType<IamGroup> = [
    { title: 'Tên nhóm', dataIndex: 'name', key: 'name' },
    { title: 'Mô tả', dataIndex: 'description', key: 'description', render: (v: string | null) => v ?? '—' },
    {
      title: 'Vai trò', key: 'roles', width: 110,
      render: (_: unknown, r: IamGroup) => <span style={{ fontVariantNumeric: 'tabular-nums' }}>{r.roleIds?.length ?? 0}</span>,
    },
    {
      title: 'Thao tác', key: 'action', width: 260,
      render: (_: unknown, r: IamGroup) => (
        <Space>
          <Button size="small" type="link" onClick={() => setSelectedId(r.id)}>Vai trò</Button>
          <Button size="small" type="link" onClick={() => { setEditing(r); editForm.setFieldsValue({ tenantId: r.tenantId, name: r.name, description: r.description ?? undefined }); }}>Sửa</Button>
          <Popconfirm
            title={`Xóa nhóm ${r.name}?`}
            description="Nhóm con (nếu có) và user thuộc nhóm cần được chuyển trước."
            okText="Xóa" okType="danger" cancelText="Hủy"
            onConfirm={async () => {
              try { await deleteGroup(r.id).unwrap(); message.success(`Đã xóa nhóm ${r.name}`); }
              catch { message.error('Xóa nhóm thất bại'); }
            }}
          >
            <Button size="small" type="link" danger>Xóa</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Nhóm người dùng"
        subTitle="Đơn vị to nhất gán user — quyền merge từ các vai trò của nhóm"
        extra={<Button type="primary" onClick={() => setCreateOpen(true)}>Tạo nhóm</Button>}
      />
      <ApiErrorAlert message={mutationError ?? (groupsQuery.error as { message?: string } | undefined)?.message ?? null} />
      <SummaryRow
        items={[
          { label: 'Số nhóm', value: displayGroups.length, icon: <TeamOutlined /> },
          { label: 'Cây phân cấp', value: tree.length > 0 ? `${tree.length} gốc` : '—', icon: <ApartmentOutlined />, color: '#0F4C81', hint: 'GET /groups/tree' },
        ]}
      />
      {groupsQuery.isLoading ? <ListSkeleton /> : displayGroups.length === 0 ? (
        <Card><TableEmpty illustration="users" description="Chưa có nhóm người dùng" actionText="Tạo nhóm đầu tiên" onAction={() => setCreateOpen(true)} /></Card>
      ) : (
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          {tree.length > 0 && (
            <Card size="small" title="Cây phân cấp">
              <Tree treeData={toTreeNodes(tree)} defaultExpandAll showLine />
            </Card>
          )}
          <DataTable<IamGroup> columns={columns} data={displayGroups} loading={groupsQuery.isFetching} rowKey="id" />
        </Space>
      )}

      <Drawer
        title={selected ? `Vai trò — ${selected.name}` : 'Vai trò'}
        width={560} open={!!selected} onClose={() => setSelectedId(null)} destroyOnClose
      >
        {rolesQuery.isLoading ? <ListSkeleton /> : rolesQuery.isError ? (
          <TableEmpty description="Không tải được vai trò" actionText="Thử lại" onAction={() => rolesQuery.refetch()} />
        ) : (
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="Nhóm">{selected?.name}</Descriptions.Item>
              <Descriptions.Item label="Vai trò">{selectedRoleIds.length}</Descriptions.Item>
            </Descriptions>
            <Transfer
              dataSource={allRoles.map((r) => ({ key: String(r.id), title: `${r.code} — ${r.name}` }))}
              targetKeys={selectedRoleIds}
              onChange={onTransferChange}
              render={(item) => <span>{item.title ?? item.key}</span>}
              listStyle={{ width: '45%', height: 320 }}
              showSearch
              filterOption={(input, option) => (option.title ?? '').toLowerCase().includes(input.toLowerCase())}
              titles={['Ngoài nhóm', 'Trong nhóm']}
              locale={{ searchPlaceholder: 'Tìm vai trò' }}
            />
            <Typography.Text type="secondary">
              Gán/gỡ tại đây gọi POST/DELETE /groups/{'{id}'}/roles/{'{roleId}'} (endpoint mới, gap cả 2 bên).
            </Typography.Text>
          </Space>
        )}
      </Drawer>

      <Modal title="Tạo nhóm" open={createOpen} onCancel={() => setCreateOpen(false)} confirmLoading={createState.isLoading}
        okText="Tạo" cancelText="Hủy" destroyOnClose
        onOk={async () => {
          try {
            const v = await createForm.validateFields();
            await createGroup({ tenantId: Number(v.tenantId), name: v.name.trim(), description: v.description?.trim() || undefined, parentId: v.parentId ?? null }).unwrap();
            message.success(`Đã tạo nhóm ${v.name.trim()}`); setCreateOpen(false); createForm.resetFields();
          } catch { if (createState.error) message.error('Tạo nhóm thất bại'); }
        }}>
        {groupForm(createForm, true)}
      </Modal>
      <Modal title={`Sửa nhóm ${editing?.name ?? ''}`} open={!!editing} onCancel={() => setEditing(null)} confirmLoading={updateState.isLoading}
        okText="Lưu" cancelText="Hủy" destroyOnClose
        onOk={async () => {
          if (!editing) return;
          try {
            const v = await editForm.validateFields();
            await updateGroup({ id: editing.id, data: { name: v.name.trim(), description: v.description?.trim() || undefined } }).unwrap();
            message.success(`Đã lưu nhóm ${editing.name}`); setEditing(null);
          } catch { if (updateState.error) message.error('Lưu nhóm thất bại'); }
        }}>
        {groupForm(editForm, true)}
      </Modal>
    </div>
  );
}
