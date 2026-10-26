import { useEffect, useMemo, useState } from 'react';
import {
  App,
  Button,
  Card,
  Descriptions,
  Drawer,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Switch,
  Typography,
  Upload,
} from 'antd';
import { ReloadOutlined, StopOutlined, TeamOutlined, UploadOutlined, UserAddOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import {
  useListUsersQuery,
  useGetUserQuery,
  useCreateUserMutation,
  useAssignRoleMutation,
  useRemoveRoleMutation,
  useUpdateAnyProfileMutation,
  useUpdateAnyAvatarMutation,
  useSetEnabledMutation,
  useResetPasswordMutation,
  useListRolesQuery,
  type AdminUser,
} from '../features/admin/adminApi';
import { useLazyFileUrlQuery, useUploadMutation } from '../features/file/fileApi';
import { cropToSquare } from '../utils/cropImage';
import { useAppSelector } from '../app/hooks';
import {
  ApiErrorAlert,
  DataTable,
  FilterBar,
  ListSkeleton,
  PageHeader,
  RoleTag,
  RoleTags,
  StatusTag,
  SummaryRow,
  TableEmpty,
  UserAvatar,
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
  const { message } = App.useApp();
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
  const [updateAnyProfile, updateProfileState] = useUpdateAnyProfileMutation();
  const [updateAnyAvatar, updateAvatarState] = useUpdateAnyAvatarMutation();
  const [setEnabled, enabledState] = useSetEnabledMutation();
  const [resetPassword, resetState] = useResetPasswordMutation();
  const [triggerFileUrl] = useLazyFileUrlQuery();
  const [uploadFile, uploadState] = useUploadMutation();

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
  // Co loc/search dang ap dung khong — quyet dinh CTA cua empty state
  // (Xoa loc khi dang loc, Tao nguoi dung khi that su trong).
  const hasFilter = search.trim().length > 0 || roleFilter !== undefined || statusFilter !== undefined;

  const mutationError =
    (createState.error as { message?: string } | undefined)?.message ??
    (assignState.error as { message?: string } | undefined)?.message ??
    (removeState.error as { message?: string } | undefined)?.message ??
    (updateProfileState.error as { message?: string } | undefined)?.message ??
    (updateAvatarState.error as { message?: string } | undefined)?.message ??
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
  const [editOpen, setEditOpen] = useState(false);
  const [editForm] = Form.useForm();
  const [detailAvatarUrl, setDetailAvatarUrl] = useState<string | null>(null);

  // Resolve file-key picture cua user dang xem → URL tuoi tho ngan (S1).
  useEffect(() => {
    let cancelled = false;
    setDetailAvatarUrl(null);
    if (!detail?.picture) return;
    triggerFileUrl({ key: detail.picture })
      .unwrap()
      .then((res) => {
        if (!cancelled) setDetailAvatarUrl(res.data?.url ?? null);
      })
      .catch(() => {
        if (!cancelled) setDetailAvatarUrl(null);
      });
    return () => {
      cancelled = true;
    };
  }, [detail?.picture, triggerFileUrl]);

  // Reset form sua ho so + avatar khi mo drawer sang user khac.
  useEffect(() => {
    setEditOpen(false);
    setNewPassword('');
    setRoleToAdd(undefined);
  }, [selectedId]);

  const onSaveAnyProfile = async () => {
    if (!detail) return;
    try {
      const values = await editForm.validateFields();
      await runMutation(
        () =>
          updateAnyProfile({
            id: detail.id,
            firstName: values.firstName?.trim() || undefined,
            lastName: values.lastName?.trim() || undefined,
            email: values.email?.trim() || undefined,
          }).unwrap(),
        'Đã lưu hồ sơ người dùng',
        'Lưu hồ sơ thất bại',
      ).then((ok) => {
        if (ok) setEditOpen(false);
      });
    } catch {
      // validateFields loi → form tu hien thi.
    }
  };

  const onAdminAvatarFile = async (file: File) => {
    if (!detail) return false;
    try {
      // Crop vuong o giua nhu ProfilePage — anh chu nhat se meo trong UserAvatar tron.
      const square = await cropToSquare(file);
      const up = await uploadFile({ file: square, folder: 'avatars' }).unwrap();
      const key = up.data?.key;
      if (!key) {
        message.error('Upload ảnh thất bại: không nhận được file key');
        return false;
      }
      await updateAnyAvatar({ id: detail.id, fileKey: key }).unwrap();
      message.success('Đã cập nhật ảnh đại diện');
    } catch {
      message.error('Cập nhật ảnh đại diện thất bại');
    }
    return false;
  };

  const columns: ColumnsType<AdminUser> = [
    {
      title: 'Người dùng',
      dataIndex: 'username',
      key: 'username',
      render: (v: string, r: AdminUser) => (
        <Space>
          <UserAvatar name={v} size={32} />
          <span>
            <Button
              type="link"
              style={{ padding: 0, height: 'auto', fontWeight: 500 }}
              onClick={() => { setSelectedId(r.id); setNewPassword(''); setRoleToAdd(undefined); }}
            >
              {v}
            </Button>
            <br />
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {[r.firstName, r.lastName].filter(Boolean).join(' ') || r.email || '—'}
            </Typography.Text>
          </span>
        </Space>
      ),
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
      render: (_: unknown, r: AdminUser) => <RoleTags roles={r.roles} />,
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
          { label: 'Tổng số', value: total, icon: <TeamOutlined />, hint: 'tài khoản trong hệ thống' },
          { label: 'Vô hiệu hóa', value: disabledCount, icon: <StopOutlined />, color: '#C13515', hint: 'trang hiện tại' },
          { label: 'ADMIN', value: adminCount, icon: <UserAddOutlined />, color: '#0F4C81', hint: 'trang hiện tại · xem vai trò', link: '/admin/roles' },
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
        <Button icon={<ReloadOutlined />} onClick={() => query.refetch()}>Tải lại</Button>
        {(search || roleFilter || statusFilter) && (
          <Button
            onClick={() => {
              setSearch('');
              setRoleFilter(undefined);
              setStatusFilter(undefined);
            }}
          >
            Xóa lọc
          </Button>
        )}
      </FilterBar>

      {query.isLoading ? (
        <ListSkeleton />
      ) : query.isError ? (
        <Card>
          <TableEmpty
            description="Không tải được danh sách người dùng"
            actionText="Thử lại"
            onAction={() => query.refetch()}
          />
        </Card>
      ) : filtered.length === 0 ? (
        <Card>
          <TableEmpty
            illustration="users"
            description={appliedSearch ? `Không tìm thấy "${appliedSearch}"` : 'Chưa có người dùng'}
            actionText={hasFilter ? 'Xóa lọc' : 'Tạo người dùng'}
            onAction={
              hasFilter
                ? () => {
                    setSearch('');
                    setRoleFilter(undefined);
                    setStatusFilter(undefined);
                  }
                : () => setCreateOpen(true)
            }
          />
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
        title={
          detail ? (
            <Space>
              <UserAvatar name={detail.username} picture={detailAvatarUrl} />
              <span>
                {detail.username}
                <br />
                <Typography.Text type="secondary" style={{ fontSize: 12, fontWeight: 400 }}>
                  {detail.email ?? [detail.firstName, detail.lastName].filter(Boolean).join(' ') ?? '—'}
                </Typography.Text>
              </span>
            </Space>
          ) : (
            'Chi tiết người dùng'
          )
        }
        width={560}
        open={!!selectedId}
        onClose={() => setSelectedId(null)}
        destroyOnClose
      >
        {detailQuery.isLoading ? (
          <ListSkeleton />
        ) : detailQuery.isError || !detail ? (
          <TableEmpty
            description="Không tải được chi tiết"
            actionText="Thử lại"
            onAction={() => detailQuery.refetch()}
          />
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
            <Card
              size="small"
              title="Hồ sơ"
              extra={
                <Button
                  size="small"
                  type="link"
                  onClick={() => {
                    editForm.setFieldsValue({
                      firstName: detail.firstName ?? '',
                      lastName: detail.lastName ?? '',
                      email: detail.email ?? '',
                    });
                    setEditOpen(true);
                  }}
                >
                  Sửa
                </Button>
              }
            >
              <Space>
                <UserAvatar name={detail.username} picture={detailAvatarUrl} size={48} />
                <div>
                  <Upload
                    accept="image/*"
                    showUploadList={false}
                    beforeUpload={(file) => {
                      if (!file.type.startsWith('image/')) {
                        message.error('Chỉ chấp nhận file ảnh');
                        return false;
                      }
                      if (file.size > 2 * 1024 * 1024) {
                        message.error('Ảnh đại diện tối đa 2MB');
                        return false;
                      }
                      void onAdminAvatarFile(file as File);
                      return false;
                    }}
                  >
                    <Button size="small" icon={<UploadOutlined />} loading={uploadState.isLoading || updateAvatarState.isLoading}>
                      {detail.picture ? 'Đổi ảnh đại diện' : 'Tải ảnh đại diện'}
                    </Button>
                  </Upload>
                  {detail.picture && (
                    <Button
                      size="small"
                      type="link"
                      danger
                      loading={updateAvatarState.isLoading}
                      onClick={() =>
                        runMutation(
                          () => updateAnyAvatar({ id: detail.id }).unwrap(),
                          'Đã xóa ảnh đại diện',
                          'Xóa ảnh đại diện thất bại',
                        )
                      }
                    >
                      Xóa ảnh
                    </Button>
                  )}
                </div>
              </Space>
              {editOpen && (
                <Form form={editForm} layout="vertical" style={{ marginTop: 12 }}>
                  <Form.Item name="firstName" label="Tên" rules={[{ max: 64, message: 'Tối đa 64 ký tự' }]}>
                    <Input placeholder="Tên" />
                  </Form.Item>
                  <Form.Item name="lastName" label="Họ" rules={[{ max: 64, message: 'Tối đa 64 ký tự' }]}>
                    <Input placeholder="Họ" />
                  </Form.Item>
                  <Form.Item name="email" label="Email" rules={[{ type: 'email', message: 'Email không hợp lệ' }]}>
                    <Input placeholder="Email" />
                  </Form.Item>
                  <Space>
                    <Button type="primary" size="small" loading={updateProfileState.isLoading} onClick={onSaveAnyProfile}>
                      Lưu
                    </Button>
                    <Button size="small" onClick={() => setEditOpen(false)}>
                      Hủy
                    </Button>
                  </Space>
                </Form>
              )}
            </Card>
            <Card size="small" title="Quyền (realm roles)">
              <Space wrap>
                {detail.roles.map((r) => (
                  <RoleTag
                    key={r}
                    role={r}
                    closable={!(isSelf && r === 'ADMIN')}
                    onClose={(e) => {
                      e.preventDefault();
                      runMutation(
                        () => removeRole({ id: detail.id, role: r }).unwrap(),
                        `Đã gỡ quyền ${r}`,
                        `Gỡ quyền ${r} thất bại`,
                      )
                    }}
                  />
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
              {isSelf && <Typography.Text type="secondary">Không thể tự gỡ quyền ADMIN của chính mình.</Typography.Text>}
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
              {isSelf && <Typography.Text type="secondary">Không thể tự vô hiệu hóa tài khoản của mình.</Typography.Text>}
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
