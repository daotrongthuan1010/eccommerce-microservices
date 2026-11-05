import { useState } from 'react';
import {
  App,
  Button,
  Descriptions,
  Drawer,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Switch,
  Typography,
} from 'antd';
import { ReloadOutlined, StopOutlined, TeamOutlined, UserAddOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import {
  useListMyStaffQuery,
  useGetStaffQuery,
  useCreateStaffMutation,
  useSetStaffEnabledMutation,
  useResetStaffPasswordMutation,
  type StaffUser,
} from '../features/user/staffApi';
import { useAppSelector } from '../app/hooks';
import {
  ApiErrorAlert,
  DataTable,
  FilterBar,
  ListSkeleton,
  PageHeader,
  RoleTags,
  StatusTag,
  SummaryRow,
  UserAvatar,
} from '../components/ui';

// /staff/users — cap tren (ADMIN/MODERATOR) quan ly CAP DUOI TRUC TIEP cua
// minh. Khac /admin/users (ADMIN toan he thong): BE loc theo managerId,
// moi nguoi chi thay nguoi minh tao. FE-STYLE §8: PageHeader → SummaryRow →
// FilterBar → DataTable → Drawer 560 → Create modal.

export default function StaffUsersPage() {
  const { message } = App.useApp();
  const myRoles = useAppSelector((s) => s.auth.roles) ?? [];
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState<string | undefined>(undefined);
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [createForm] = Form.useForm();

  // ADMIN duoc tao MODERATOR + USER; MODERATOR chi tao USER (BE role matrix
  // StaffUserController.GRANTABLE_ROLES — FE chi goi y, BE la nguon chan ly).
  const isAdmin = myRoles.includes('ADMIN');
  const grantableRoles = isAdmin ? ['USER', 'MODERATOR'] : ['USER'];

  const query = useListMyStaffQuery();
  const detailQuery = useGetStaffQuery(selectedId ?? '', { skip: !selectedId });

  const [createStaff, createState] = useCreateStaffMutation();
  const [setStaffEnabled, enabledState] = useSetStaffEnabledMutation();
  const [resetStaffPassword, resetState] = useResetStaffPasswordMutation();

  const items: StaffUser[] = query.data?.items ?? [];
  const filtered = items.filter(
    (u) =>
      (!search.trim() ||
        u.username.toLowerCase().includes(search.trim().toLowerCase()) ||
        (u.email ?? '').toLowerCase().includes(search.trim().toLowerCase())) &&
      (!roleFilter || u.roles.includes(roleFilter)) &&
      (!statusFilter || (statusFilter === 'ENABLED' ? u.enabled : !u.enabled)),
  );

  const total = query.data?.total ?? 0;
  const enabledCount = items.filter((u) => u.enabled).length;
  const moderatorCount = items.filter((u) => u.roles.includes('MODERATOR')).length;

  const mutationError =
    (createState.error as { message?: string } | undefined)?.message ??
    (enabledState.error as { message?: string } | undefined)?.message ??
    (resetState.error as { message?: string } | undefined)?.message;

  const runMutation = async (
    fn: () => Promise<unknown>,
    successMsg: string,
    errorMsg: string,
  ): Promise<boolean> => {
    try {
      await fn();
      message.success(successMsg);
      return true;
    } catch (e) {
      const err = e as { data?: { message?: string; error?: string } };
      message.error(err?.data?.message ?? errorMsg);
      return false;
    }
  };

  const columns: ColumnsType<StaffUser> = [
    {
      title: 'Người dùng',
      dataIndex: 'username',
      key: 'username',
      render: (_: string, u: StaffUser) => (
        <Space>
          <UserAvatar picture={u.picture} name={u.username} />
          <div>
            <div>{u.username}</div>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {u.email}
            </Typography.Text>
          </div>
        </Space>
      ),
    },
    {
      title: 'Họ tên',
      key: 'name',
      render: (_: unknown, u: StaffUser) =>
        [u.lastName, u.firstName].filter(Boolean).join(' ') || '—',
    },
    {
      title: 'Quyền',
      dataIndex: 'roles',
      key: 'roles',
      render: (roles: string[]) => <RoleTags roles={roles} />,
    },
    {
      title: 'Email',
      dataIndex: 'emailVerified',
      key: 'emailVerified',
      render: (v: boolean) => (
        <StatusTag status={v ? 'ACTIVE' : 'PENDING'} />
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'enabled',
      key: 'enabled',
      render: (v: boolean) => <StatusTag status={v ? 'ACTIVE' : 'DISABLED'} />,
    },
    {
      title: 'Hành động',
      key: 'actions',
      fixed: 'right',
      render: (_: unknown, u: StaffUser) => (
        <Space>
          <Button size="small" type="text" onClick={() => setSelectedId(u.id)}>
            Chi tiết
          </Button>
          <Button
            size="small"
            type="text"
            danger={!u.enabled ? false : true}
            icon={<StopOutlined />}
            onClick={() =>
              runMutation(
                () => setStaffEnabled({ id: u.id, enabled: !u.enabled }).unwrap(),
                u.enabled ? 'Đã khóa tài khoản' : 'Đã mở khóa tài khoản',
                'Thao tác thất bại',
              )
            }
          >
            {u.enabled ? 'Khóa' : 'Mở khóa'}
          </Button>
        </Space>
      ),
    },
  ];

  if (query.isLoading) {
    return (
      <div>
        <PageHeader
          title="Quản lý cấp dưới"
          subTitle="Danh sách tài khoản bạn tạo cho cấp dưới"
        />
        <ListSkeleton />
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="Quản lý cấp dưới"
        subTitle="Tài khoản doanh nghiệp do bạn tạo — mỗi cấp chỉ quản lý cấp dưới trực tiếp"
        extra={
          <Button type="primary" icon={<UserAddOutlined />} onClick={() => setCreateOpen(true)}>
            Tạo tài khoản cho cấp dưới
          </Button>
        }
      />
      <SummaryRow
        items={[
          { label: 'Cấp dưới trực tiếp', value: total, icon: <TeamOutlined /> },
          { label: 'Đang hoạt động', value: enabledCount, icon: <TeamOutlined /> },
          {
            label: 'Điều phối viên',
            value: moderatorCount,
            icon: <TeamOutlined />,
            hint: 'Chỉ ADMIN có thể tạo MODERATOR',
          },
        ]}
      />
      <FilterBar>
        <Input.Search
          placeholder="Tìm theo tên đăng nhập / email"
          allowClear
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{ width: 280 }}
        />
        <Select
          placeholder="Lọc quyền"
          allowClear
          value={roleFilter}
          onChange={setRoleFilter}
          style={{ width: 160 }}
          options={grantableRoles.map((r) => ({ value: r, label: r }))}
        />
        <Select
          placeholder="Lọc trạng thái"
          allowClear
          value={statusFilter}
          onChange={setStatusFilter}
          style={{ width: 160 }}
          options={[
            { value: 'ENABLED', label: 'Đang hoạt động' },
            { value: 'DISABLED', label: 'Đã khóa' },
          ]}
        />
        <Button icon={<ReloadOutlined />} onClick={() => query.refetch()}>
          Tải lại
        </Button>
      </FilterBar>
      {mutationError && <ApiErrorAlert message={mutationError} />}
      <DataTable<StaffUser>
        columns={columns}
        data={filtered}
        rowKey="id"
        loading={query.isFetching}
      />

      {/* Drawer 560 — chi tiet 1 cap duoi (FE-STYLE §8). */}
      <Drawer
        title={detailQuery.data?.username ?? 'Chi tiết'}
        width={560}
        open={!!selectedId}
        onClose={() => setSelectedId(null)}
      >
        {detailQuery.data && (
          <>
            <UserAvatar
              picture={detailQuery.data.picture}
              name={detailQuery.data.username}
              size={64}
            />
            <Descriptions column={1} style={{ marginTop: 16 }}>
              <Descriptions.Item label="Tên đăng nhập">
                {detailQuery.data.username}
              </Descriptions.Item>
              <Descriptions.Item label="Email">{detailQuery.data.email ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="Họ tên">
                {[detailQuery.data.lastName, detailQuery.data.firstName]
                  .filter(Boolean)
                  .join(' ') || '—'}
              </Descriptions.Item>
              <Descriptions.Item label="Quyền">
                <RoleTags roles={detailQuery.data.roles} />
              </Descriptions.Item>
              <Descriptions.Item label="Loại tài khoản">
                {detailQuery.data.accountType === 'ENTERPRISE_STAFF'
                  ? 'Doanh nghiệp (do cấp trên tạo)'
                  : detailQuery.data.accountType === 'SELF'
                    ? 'Cá nhân (tự đăng ký)'
                    : '—'}
              </Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                <StatusTag
                  status={detailQuery.data.enabled ? 'ACTIVE' : 'DISABLED'}
                />
              </Descriptions.Item>
            </Descriptions>
            <Button
              onClick={() =>
                runMutation(
                  () =>
                    resetStaffPassword({
                      id: detailQuery.data!.id,
                      newPassword: `Staff@${Date.now()}`,
                      temporary: true,
                    }).unwrap(),
                  'Đã đặt lại mật khẩu (tạm thời — user đổi ở lần đăng nhập đầu)',
                  'Đặt lại mật khẩu thất bại',
                )
              }
              loading={resetState.isLoading}
            >
              Đặt lại mật khẩu
            </Button>
          </>
        )}
      </Drawer>

      {/* Create modal — tai khoan doanh nghiep: cap tren tin cuoc email nen
          login duoc ngay; bat "Gửi email xác thực" neu muon user tu xac thuc. */}
      <Modal
        title="Tạo tài khoản cho cấp dưới"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={() =>
          createForm.validateFields().then((v) =>
            runMutation(
              () => createStaff(v as Parameters<typeof createStaff>[0]).unwrap(),
              'Đã tạo tài khoản cho cấp dưới',
              'Tạo tài khoản thất bại',
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
        <Form form={createForm} layout="vertical" initialValues={{ roles: ['USER'] }}>
          <Form.Item
            name="username"
            label="Tên đăng nhập"
            rules={[
              { required: true, message: 'Nhập tên đăng nhập' },
              { min: 3, max: 64, message: '3-64 ký tự' },
              { pattern: /^[a-zA-Z0-9._-]+$/, message: 'Chỉ gồm chữ, số, . _ -' },
            ]}
          >
            <Input placeholder="username" autoComplete="off" />
          </Form.Item>
          <Form.Item
            name="email"
            label="Email"
            rules={[
              { required: true, message: 'Nhập email' },
              { type: 'email', message: 'Email không hợp lệ' },
            ]}
          >
            <Input placeholder="capduoi@congty.vn" autoComplete="off" />
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
            rules={[
              { required: true, message: 'Nhập mật khẩu' },
              { min: 8, message: 'Tối thiểu 8 ký tự' },
            ]}
          >
            <Input.Password placeholder="Tối thiểu 8 ký tự" autoComplete="new-password" />
          </Form.Item>
          <Form.Item
            name="roles"
            label="Quyền"
            tooltip="Bạn chỉ có thể gán quyền thấp hơn hoặc bằng cấp của mình"
          >
            <Select
              mode="multiple"
              options={grantableRoles.map((r) => ({ value: r, label: r }))}
            />
          </Form.Item>
          <Form.Item
            name="sendVerifyEmail"
            label="Gửi email xác thực"
            valuePropName="checked"
            tooltip="Tắt: bạn chịu trách nhiệm xác nhận email đúng — tài khoản đăng nhập được ngay"
          >
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
