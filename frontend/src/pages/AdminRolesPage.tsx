import { useMemo, useState } from 'react';
import { Button, Card, Descriptions, Drawer, Form, Input, Modal, Popconfirm, Space, Transfer, Typography, message } from 'antd';
import { KeyOutlined, TeamOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import {
  useListRolesQuery,
  useListRoleMembersQuery,
  useListUsersQuery,
  useAssignRoleMutation,
  useRemoveRoleMutation,
  useCreateRoleMutation,
  useDeleteRoleMutation,
  type RealmRole,
} from '../features/admin/adminApi';
import { useAppSelector } from '../app/hooks';
import {
  ApiErrorAlert,
  DataTable,
  ListSkeleton,
  PageHeader,
  RoleTag,
  SummaryRow,
  TableEmpty,
  UserAvatar,
} from '../components/ui';

// /admin/roles — RequireRole ADMIN. Table (role, mo ta, members) → Drawer
// 560 voi Transfer gan/go user, tai su dung Users assign/remove endpoints.
// Roles page chi doc + dieu huong (design D5); khong co membership API rieng.
export default function AdminRolesPage() {
  const myUsername = useAppSelector((s) => s.auth.username);
  const [selectedRole, setSelectedRole] = useState<string | null>(null);

  const rolesQuery = useListRolesQuery();
  // Members chinh xac tu GET /{role}/users; pool users cho Transfer lay tu list.
  const membersQuery = useListRoleMembersQuery(selectedRole ?? '', { skip: !selectedRole });
  const poolQuery = useListUsersQuery({ size: 50 }, { skip: !selectedRole });

  const [assignRole, assignState] = useAssignRoleMutation();
  const [removeRole, removeState] = useRemoveRoleMutation();
  const [createRole, createState] = useCreateRoleMutation();
  const [deleteRole] = useDeleteRoleMutation();
  const [createOpen, setCreateOpen] = useState(false);
  const [createForm] = Form.useForm();

  const roles: RealmRole[] = rolesQuery.data?.items ?? [];
  const members = membersQuery.data?.items ?? [];
  const pool = poolQuery.data?.items ?? [];
  // Pool = hop nhat members + list (list co the thieu user khi vuot size).
  const allUsers = useMemo(() => {
    const map = new Map(pool.map((u) => [u.id, u]));
    for (const m of members) map.set(m.id, m);
    return [...map.values()];
  }, [pool, members]);

  const memberIds = useMemo(() => members.map((u) => u.id), [members]);

  const totalAssignments = roles.reduce((s, r) => s + (r.memberCount ?? 0), 0);
  const mutationError =
    (assignState.error as { message?: string } | undefined)?.message ??
    (removeState.error as { message?: string } | undefined)?.message;

  const onTransferChange = async (_next: React.Key[], direction: string, moveKeys: React.Key[]) => {
    if (!selectedRole) return;
    try {
      let done = 0;
      if (direction === 'right') {
        for (const key of moveKeys) {
          const u = allUsers.find((x) => x.id === String(key));
          if (selectedRole === 'ADMIN' && u?.username === myUsername) {
            message.warning(`Bỏ qua ${u.username}: không tự gán/gỡ ADMIN của chính mình tại đây`);
            continue;
          }
          await assignRole({ id: String(key), role: selectedRole }).unwrap();
          done += 1;
        }
        message.success(`Đã gán quyền ${selectedRole} cho ${done} người dùng`);
      } else {
        for (const key of moveKeys) {
          const u = allUsers.find((x) => x.id === String(key));
          if (selectedRole === 'ADMIN' && u?.username === myUsername) {
            message.warning(`Bỏ qua ${u.username}: không tự gỡ ADMIN của chính mình`);
            continue;
          }
          await removeRole({ id: String(key), role: selectedRole }).unwrap();
          done += 1;
        }
        message.success(`Đã gỡ quyền ${selectedRole} khỏi ${done} người dùng`);
      }
    } catch {
      message.error('Cập nhật thành viên thất bại');
    }
  };

  const columns: ColumnsType<RealmRole> = [
    {
      title: 'Vai trò',
      dataIndex: 'name',
      key: 'name',
      render: (v: string, r: RealmRole) => (
        <Space>
          <RoleTag role={v} />
          {v === 'ADMIN' && (
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>toàn quyền hệ thống</Typography.Text>
          )}
          {r.description ? (
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>{r.description}</Typography.Text>
          ) : null}
        </Space>
      ),
    },
    {
      title: 'Thành viên',
      dataIndex: 'memberCount',
      key: 'memberCount',
      width: 130,
      render: (v: number) => <span style={{ fontVariantNumeric: 'tabular-nums' }}>{v ?? 0}</span>,
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 200,
      render: (_: unknown, r: RealmRole) => (
        <Space>
          <Button size="small" type="link" onClick={() => setSelectedRole(r.name)}>
            Thành viên
          </Button>
          <Popconfirm
            title={`Xóa vai trò ${r.name}?`}
            description={
              r.memberCount > 0
                ? `Vai trò ${r.name} đang có ${r.memberCount} thành viên. Keycloak cho phép xóa role đang gán — hãy chắc chắn.`
                : `Vai trò ${r.name} sẽ bị xóa khỏi realm Keycloak.`
            }
            okText="Xóa"
            okType="danger"
            cancelText="Hủy"
            onConfirm={async () => {
              try {
                await deleteRole(r.name).unwrap();
                message.success(`Đã xóa vai trò ${r.name}`);
              } catch {
                message.error('Xóa vai trò thất bại');
              }
            }}
          >
            <Button
              size="small"
              type="link"
              danger
              disabled={['ADMIN', 'USER', 'offline_access', 'uma_authorization'].includes(r.name)}
            >
              Xóa
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const queryError = (rolesQuery.error as { message?: string } | undefined)?.message;

  return (
    <div>
      <PageHeader
        title="Vai trò"
        subTitle="Realm roles Keycloak (ADMIN)"
        extra={<Button type="primary" onClick={() => setCreateOpen(true)}>Tạo vai trò</Button>}
      />
      <ApiErrorAlert message={mutationError ?? queryError ?? null} />
      <SummaryRow
        items={[
          { label: 'Số vai trò', value: roles.length, icon: <KeyOutlined />, hint: 'realm roles Keycloak' },
          { label: 'Tổng lượt gán', value: totalAssignments, icon: <TeamOutlined />, color: '#0F4C81', hint: 'người dùng × vai trò · xem người dùng', link: '/admin/users' },
        ]}
      />

      {rolesQuery.isLoading ? (
        <ListSkeleton />
      ) : rolesQuery.isError ? (
        <Card>
          <TableEmpty
            illustration="roles"
            description="Không tải được danh sách vai trò"
            actionText="Thử lại"
            onAction={() => rolesQuery.refetch()}
          />
        </Card>
      ) : roles.length === 0 ? (
        <Card>
          <TableEmpty
            illustration="roles"
            description="Chưa có vai trò"
            actionText="Tạo vai trò đầu tiên"
            onAction={() => setCreateOpen(true)}
          />
        </Card>
      ) : (
        <DataTable<RealmRole> columns={columns} data={roles} loading={rolesQuery.isFetching} rowKey="name" />
      )}

      <Drawer
        title={
          selectedRole ? (
            <Space>
              <RoleTag role={selectedRole} />
              <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                {members.length} thành viên
              </Typography.Text>
            </Space>
          ) : (
            'Thành viên'
          )
        }
        width={560}
        open={!!selectedRole}
        onClose={() => setSelectedRole(null)}
        destroyOnClose
      >
        {membersQuery.isLoading || poolQuery.isLoading ? (
          <ListSkeleton />
        ) : membersQuery.isError || poolQuery.isError ? (
          <TableEmpty
            illustration="users"
            description="Không tải được danh sách người dùng"
            actionText="Thử lại"
            onAction={() => { membersQuery.refetch(); poolQuery.refetch(); }}
          />
        ) : (
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="Vai trò">{selectedRole}</Descriptions.Item>
              <Descriptions.Item label="Thành viên">{memberIds.length}</Descriptions.Item>
            </Descriptions>
            <Transfer
              dataSource={allUsers.map((u) => ({
                key: u.id,
                title: `${u.username}${u.email ? ` (${u.email})` : ''}`,
                disabled: selectedRole === 'ADMIN' && u.username === myUsername,
              }))}
              targetKeys={memberIds}
              onChange={onTransferChange}
              render={(item) => {
                const u = allUsers.find((x) => x.id === String(item.key));
                return (
                  <Space size={6}>
                    <UserAvatar name={u?.username ?? ''} size={20} />
                    <span>{item.title ?? item.key}</span>
                  </Space>
                );
              }}
              listStyle={{ width: '45%', height: 320 }}
              showSearch
              filterOption={(input, option) =>
                (option.title ?? '').toLowerCase().includes(input.toLowerCase())
              }
              titles={['Ngoài vai trò', 'Trong vai trò']}
              locale={{ searchPlaceholder: 'Tìm người dùng' }}
            />
            <Typography.Text type="secondary">
              Gán/gỡ tại đây dùng chung endpoints Users — bảng Users và Roles cùng refresh sau mutation.
            </Typography.Text>
          </Space>
        )}
      </Drawer>

      <Modal
        title="Tạo vai trò"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        confirmLoading={createState.isLoading}
        okText="Tạo"
        cancelText="Hủy"
        destroyOnClose
        onOk={async () => {
          try {
            const v = await createForm.validateFields();
            await createRole({
              name: v.name.trim(),
              description: v.description?.trim() || undefined,
            }).unwrap();
            message.success(`Đã tạo vai trò ${v.name.trim()}`);
            setCreateOpen(false);
            createForm.resetFields();
          } catch {
            if (createState.error) message.error('Tạo vai trò thất bại');
          }
        }}
      >
        <ApiErrorAlert message={(createState.error as { message?: string } | undefined)?.message ?? null} />
        <Form form={createForm} layout="vertical" preserve={false}>
          <Form.Item
            name="name"
            label="Tên vai trò"
            rules={[
              { required: true, message: 'Nhập tên vai trò' },
              { min: 2, message: 'Tối thiểu 2 ký tự' },
              { max: 64, message: 'Tối đa 64 ký tự' },
              { pattern: /^[A-Za-z0-9._-]+$/, message: 'Chỉ chữ/số và . _ -' },
            ]}
          >
            <Input placeholder="VD: MODERATOR" />
          </Form.Item>
          <Form.Item name="description" label="Mô tả" rules={[{ max: 255, message: 'Tối đa 255 ký tự' }]}>
            <Input.TextArea rows={2} placeholder="Mô tả vai trò (không bắt buộc)" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
