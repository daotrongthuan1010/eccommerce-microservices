import { useMemo, useState } from 'react';
import { Button, Card, Descriptions, Drawer, Empty, Space, Tag, Transfer, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  useListRolesQuery,
  useListRoleMembersQuery,
  useListUsersQuery,
  useAssignRoleMutation,
  useRemoveRoleMutation,
  type RealmRole,
} from '../features/admin/adminApi';
import { useAppSelector } from '../app/hooks';
import {
  ApiErrorAlert,
  DataTable,
  ListSkeleton,
  PageHeader,
  SummaryRow,
  TableEmpty,
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
      title: 'Quyền',
      dataIndex: 'name',
      key: 'name',
      render: (v: string) => (
        <Tag color={v === 'ADMIN' ? 'red' : 'blue'}>{v}</Tag>
      ),
    },
    { title: 'Mô tả', dataIndex: 'description', key: 'description', render: (v?: string) => v ?? '—' },
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
      width: 160,
      render: (_: unknown, r: RealmRole) => (
        <Button size="small" type="link" onClick={() => setSelectedRole(r.name)}>
          Thành viên
        </Button>
      ),
    },
  ];

  const queryError = (rolesQuery.error as { message?: string } | undefined)?.message;

  return (
    <div>
      <PageHeader title="Vai trò" subTitle="Realm roles Keycloak (ADMIN)" />
      <ApiErrorAlert message={mutationError ?? queryError ?? null} />
      <SummaryRow
        items={[
          { label: 'Số vai trò', value: roles.length },
          { label: 'Tổng lượt gán', value: <span style={{ fontVariantNumeric: 'tabular-nums' }}>{totalAssignments}</span> },
        ]}
      />

      {rolesQuery.isLoading ? (
        <ListSkeleton />
      ) : rolesQuery.isError ? (
        <Card>
          <Empty description="Không tải được danh sách vai trò">
            <Button type="primary" onClick={() => rolesQuery.refetch()}>
              Thử lại
            </Button>
          </Empty>
        </Card>
      ) : roles.length === 0 ? (
        <Card>
          <TableEmpty description="Chưa có vai trò" />
        </Card>
      ) : (
        <DataTable<RealmRole> columns={columns} data={roles} loading={rolesQuery.isFetching} rowKey="name" />
      )}

      <Drawer
        title={selectedRole ? `Thành viên — ${selectedRole}` : 'Thành viên'}
        width={560}
        open={!!selectedRole}
        onClose={() => setSelectedRole(null)}
        destroyOnClose
      >
        {membersQuery.isLoading || poolQuery.isLoading ? (
          <ListSkeleton />
        ) : membersQuery.isError || poolQuery.isError ? (
          <Empty description="Không tải được danh sách người dùng">
            <Button onClick={() => { membersQuery.refetch(); poolQuery.refetch(); }}>Thử lại</Button>
          </Empty>
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
              render={(item) => item.title ?? item.key}
              listStyle={{ width: '45%', height: 320 }}
              showSearch
              filterOption={(input, option) =>
                (option.title ?? '').toLowerCase().includes(input.toLowerCase())
              }
              titles={['Ngoài vai trò', 'Trong vai trò']}
              locale={{ searchPlaceholder: 'Tìm người dùng' }}
            />
            <p style={{ color: '#888' }}>
              Gán/gỡ tại đây dùng chung endpoints Users — bảng Users và Roles cùng refresh sau mutation.
            </p>
          </Space>
        )}
      </Drawer>
    </div>
  );
}
