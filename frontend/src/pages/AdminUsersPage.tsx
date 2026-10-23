import { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Switch,
  Tag,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  useListUsersQuery,
  useGetUserQuery,
  useCreateUserMutation,
  useAssignRoleMutation,
  useRemoveRoleMutation,
  useSetEnabledMutation,
  useResetPasswordMutation,
  useListRolesQuery,
  type AdminUser,
} from '../features/admin/adminApi';
import { useAppSelector } from '../app/hooks';
import {
  ApiErrorAlert,
  DataTable,
  FilterBar,
  ListSkeleton,
  PageHeader,
  StatusTag,
  SummaryRow,
  TableEmpty,
} from '../components/ui';

// /admin/users — RequireRole ADMIN. PageHeader → SummaryRow → FilterBar →
// Table → Drawer 560 → Create modal. 5 states + mutation feedback.
function useDebounced(value: string, delay = 300): string {
  const [v, setV] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setV(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return v;
}

const KNOWN_ROLES = ['ADMIN', 'USER', 'MODERATOR'];

export default function AdminUsersPage() {
  const myUsername = useAppSelector((s) => s.auth.username);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState<string | undefined>(undefined);
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [createForm] = Form.useForm();

  const debounced = useDebounced(search);
  // BE guard: search min 2 chars — chi gui khi rong hoac >= 2 ky tu.
  const appliedSearch = debounced.trim().length >= 2 ? debounced.trim() : undefined;

  const query = useListUsersQuery({ search: appliedSearch, page, size: 20 });
  const rolesQuery = useListRolesQuery();
  const detailQuery = useGetUserQuery(selectedId ?? '', { skip: !selectedId });

  const [createUser, createState] = useCreateUserMutation();
  const [assignRole, assignState] = useAssignRoleMutation();
  const [removeRole, removeState] = useRemoveRoleMutation();
  const [setEnabled, enabledState] = useSetEnabledMutation();
  const [resetPassword, resetState] = useResetPasswordMutation();

  // Reset ve trang 0 khi filter doi.
  useEffect(() => {
    setPage(0);
  }, [appliedSearch, roleFilter, statusFilter]);

  const items: AdminUser[] = query.data?.items ?? [];
  const filtered = useMemo(
    () =>
      items.filter(
        (u) =>
          (!roleFilter || u.roles.includes(roleFilter)) &&
          (!statusFilter || (statusFilter === 'ENABLED' ? u.enabled : !u.enabled)),
      ),
    [items, roleFilter, statusFilter],
  );

  const total = query.data?.total ?? 0;
  const disabledCount = items.filter((u) => !u.enabled).length;
  const adminCount = items.filter((u) => u.roles.includes('ADMIN')).length;

  const mutationError =
    (createState.error as { message?: string } | undefined)?.message ??
    (assignState.error as { message?: string } | undefined)?.message ??
    (removeState.error as { message?: string } | undefined)?.message ??
    (enabledState.error as { message?: string } | undefined)?.message ??
    (resetState.error as { message?: string } | undefined)?.message;

  const runMutation = async (fn: () => Promise<unknown>, ok: string, fail: string): Promise<boolean> => {
    try {
      await fn();
      message.success(ok);
      return true;
    } catch {
      message.error(fail);
      return false;
    }
  };

  const detail: AdminUser | undefined = detailQuery.data;
  const isSelf = !!detail && !!myUsername && detail.username === myUsername;
  const [newPassword, setNewPassword] = useState('');
  const [roleToAdd, setRoleToAdd] = useState<string | undefined>(undefined);

  const columns: ColumnsType<AdminUser> = [
    {
      title: 'Tên đăng nhập',
      dataIndex: 'username',
      key: 'username',
      render: (v: string) => <span style={{ fontVariantNumeric: 'tabular-nums' }}>{v}</span>,
    },
    { title: 'Email', dataIndex: 'email', key: 'email', render: (v?: string) => v ?? '—' },
    {
      title: 'Trạng thái',
      key: 'status',
      width: 130,
      render: (_: unknown, r: AdminUser) => (
        <StatusTag status={r.enabled ? 'ENABLED' : 'DISABLED'} />
      ),
    },
    {
      title: 'Quyền',
      key: 'roles',
      render: (_: unknown, r: AdminUser) =>
        r.roles.length === 0
          ? '—'
          : r.roles.map((role) => (
              <Tag key={role} color={role === 'ADMIN' ? 'red' : 'blue'}>
                {role}
              </Tag>
            )),
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 120,
      render: (_: unknown, r: AdminUser) => (
        <Button size="small" type="link" onClick={() => { setSelectedId(r.id); setNewPassword(''); setRoleToAdd(undefined); }}>
          Chi tiết
        </Button>
      ),
    },
  ];

  const queryError = (query.error as { message?: string } | undefined)?.message;

  return (
    <div>
      <PageHeader
        title="Người dùng"
        subTitle="Quản trị người dùng Keycloak (ADMIN)"
        extra={
          <Button type="primary" onClick={() => setCreateOpen(true)}>
            Tạo người dùng
          </Button>
        }
      />
      <ApiErrorAlert message={mutationError ?? queryError ?? null} />
      <SummaryRow
        items={[
          { label: 'Tổng số', value: <span style={{ fontVariantNumeric: 'tabular-nums' }}>{total}</span> },
          { label: 'Vô hiệu hóa (trang này)', value: disabledCount },
          { label: 'ADMIN (trang này)', value: adminCount },
        ]}
      />
      <FilterBar>
        <Input
          placeholder="Tìm theo username/email (≥ 2 ký tự)"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          allowClear
          style={{ width: 280 }}
        />
        <Select
          placeholder="Quyền"
          allowClear
          value={roleFilter}
          onChange={setRoleFilter}
          options={KNOWN_ROLES.map((r) => ({ value: r, label: r }))}
          style={{ width: 160 }}
        />
        <Select
          placeholder="Trạng thái"
          allowClear
          value={statusFilter}
          onChange={setStatusFilter}
          options={[
            { value: 'ENABLED', label: 'Đang hoạt động' },
            { value: 'DISABLED', label: 'Vô hiệu hóa' },
          ]}
          style={{ width: 170 }}
        />
      </FilterBar>

      {query.isLoading ? (
        <ListSkeleton />
      ) : query.isError ? (
        <Card>
          <Empty description="Không tải được danh sách người dùng">
            <Button type="primary" onClick={() => query.refetch()}>
              Thử lại
            </Button>
          </Empty>
        </Card>
      ) : filtered.length === 0 ? (
        <Card>
          <TableEmpty description={appliedSearch ? `Không tìm thấy "${appliedSearch}"` : 'Chưa có người dùng'} />
        </Card>
      ) : (
        <DataTable<AdminUser>
          columns={columns}
          data={filtered}
          loading={query.isFetching}
          total={total}
          page={page}
          onPageChange={setPage}
          rowKey="id"
        />
      )}

      <Drawer
        title="Chi tiết người dùng"
        width={560}
        open={!!selectedId}
        onClose={() => setSelectedId(null)}
        destroyOnClose
      >
        {detailQuery.isLoading ? (
          <ListSkeleton />
        ) : detailQuery.isError || !detail ? (
          <Empty description="Không tải được chi tiết">
            <Button onClick={() => detailQuery.refetch()}>Thử lại</Button>
          </Empty>
        ) : (
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="Tên đăng nhập">{detail.username}</Descriptions.Item>
              <Descriptions.Item label="Email">{detail.email ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="Họ tên">
                {[detail.firstName, detail.lastName].filter(Boolean).join(' ') || '—'}
              </Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                <StatusTag status={detail.enabled ? 'ENABLED' : 'DISABLED'} />
              </Descriptions.Item>
            </Descriptions>
            <Card size="small" title="Quyền (realm roles)">
              <Space wrap>
                {detail.roles.map((r) => (
                  <Tag
                    key={r}
                    color={r === 'ADMIN' ? 'red' : 'blue'}
                    closable={!(isSelf && r === 'ADMIN')}
                    onClose={() =>
                      runMutation(
                        () => removeRole({ id: detail.id, role: r }).unwrap(),
                        `Đã gỡ quyền ${r}`,
                        `Gỡ quyền ${r} thất bại`,
                      )
                    }
                  >
                    {r}
                  </Tag>
                ))}
                {detail.roles.length === 0 && '—'}
              </Space>
              <Space style={{ marginTop: 12 }}>
                <Select
                  placeholder="Thêm quyền"
                  value={roleToAdd}
                  onChange={setRoleToAdd}
                  options={(rolesQuery.data?.items.map((r) => r.name) ?? KNOWN_ROLES)
                    .filter((r) => !detail.roles.includes(r))
                    .map((r) => ({ value: r, label: r }))}
                  style={{ width: 180 }}
                />
                <Button
                  type="primary"
                  disabled={!roleToAdd}
                  loading={assignState.isLoading}
                  onClick={() =>
                    roleToAdd &&
                    runMutation(
                      () => assignRole({ id: detail.id, role: roleToAdd }).unwrap(),
                      `Đã gán quyền ${roleToAdd}`,
                      `Gán quyền thất bại`,
                    )
                  }
                >
                  Gán
                </Button>
              </Space>
              {isSelf && <p style={{ color: '#888' }}>Không thể tự gỡ quyền ADMIN của chính mình.</p>}
            </Card>
            <Card size="small" title="Trạng thái tài khoản">
              <Space>
                <Switch
                  checked={detail.enabled}
                  disabled={isSelf}
                  loading={enabledState.isLoading}
                  onChange={(checked) =>
                    runMutation(
                      () => setEnabled({ id: detail.id, enabled: checked }).unwrap(),
                      checked ? 'Đã kích hoạt tài khoản' : 'Đã vô hiệu hóa tài khoản',
                      'Đổi trạng thái thất bại',
                    )
                  }
                />
                <span>{detail.enabled ? 'Đang hoạt động' : 'Vô hiệu hóa'}</span>
              </Space>
              {isSelf && <p style={{ color: '#888' }}>Không thể tự vô hiệu hóa tài khoản của mình.</p>}
            </Card>
            <Card size="small" title="Đặt lại mật khẩu">
              <Space>
                <Input.Password
                  placeholder="Mật khẩu mới (≥ 8 ký tự)"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  style={{ width: 260 }}
                />
                <Button
                  loading={resetState.isLoading}
                  disabled={newPassword.length < 8}
                  onClick={() =>
                    runMutation(
                      () => resetPassword({ id: detail.id, newPassword, temporary: true }).unwrap(),
                      'Đã đặt lại mật khẩu — user đổi ở lần đăng nhập tới',
                      'Đặt lại mật khẩu thất bại',
                    )
                  }
                >
                  Đặt lại
                </Button>
              </Space>
            </Card>
          </Space>
        )}
      </Drawer>

      <Modal
        title="Tạo người dùng"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={() =>
          createForm.validateFields().then((v) =>
            runMutation(
              () => createUser(v as Parameters<typeof createUser>[0]).unwrap(),
              'Đã tạo người dùng',
              'Tạo người dùng thất bại',
            ).then((ok) => {
              if (ok) {
                setCreateOpen(false);
                createForm.resetFields();
              }
            }),
          )
        }
        confirmLoading={createState.isLoading}
        okText="Tạo"
        cancelText="Hủy"
      >
        <Form form={createForm} layout="vertical">
          <Form.Item name="username" label="Tên đăng nhập" rules={[{ required: true, message: 'Nhập username' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="email" label="Email" rules={[{ required: true, message: 'Nhập email' }, { type: 'email', message: 'Email không hợp lệ' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="firstName" label="Tên">
            <Input />
          </Form.Item>
          <Form.Item name="lastName" label="Họ">
            <Input />
          </Form.Item>
          <Form.Item
            name="password"
            label="Mật khẩu tạm thời"
            rules={[{ required: true, message: 'Nhập mật khẩu' }, { min: 8, message: 'Tối thiểu 8 ký tự' }]}
          >
            <Input.Password />
          </Form.Item>
          <Form.Item name="roles" label="Quyền">
            <Select mode="multiple" options={KNOWN_ROLES.map((r) => ({ value: r, label: r }))} />
          </Form.Item>
          <Form.Item name="sendVerifyEmail" label="Gửi email xác thực" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
