import { useState } from 'react';
import { Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Switch, Tabs, Typography, message } from 'antd';
import { AppstoreOutlined, ThunderboltOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import {
  useListMenusQuery,
  useCreateMenuMutation,
  useUpdateMenuMutation,
  useDeleteMenuMutation,
  useListActionsQuery,
  useCreateActionMutation,
  useUpdateActionMutation,
  useToggleActionMutation,
  type IamMenu,
  type IamAction,
} from '../features/admin/iamApi';
import {
  ApiErrorAlert,
  DataTable,
  ListSkeleton,
  PageHeader,
  StatusTag,
  SummaryRow,
  TableEmpty,
} from '../components/ui';

// /admin/menus — RequireRole ADMIN. Quan tri Menu (CRUD) + Action (CRUD +
// toggle active). Pattern AdminRolesPage: PageHeader → SummaryRow →
// DataTable → Modal 560. Popconfirm dat ten object cu the (§15).
function MenuForm({ form, menus, excludeId, isEdit }: {
  form: ReturnType<typeof Form.useForm>[0];
  menus: IamMenu[];
  excludeId?: string | null;
  isEdit?: boolean;
}) {
  return (
    <Form form={form} layout="vertical" preserve={false}>
      <Form.Item name="code" label="Mã menu" rules={[{ required: true, message: 'Nhập mã menu' }, { pattern: /^[A-Z0-9_]+$/, message: 'VD: USER, PRODUCT, ORDER' }]}>
        <Input placeholder="VD: PRODUCT" disabled={isEdit} />
      </Form.Item>
      <Form.Item name="name" label="Tên hiển thị" rules={[{ required: true, message: 'Nhập tên hiển thị' }]}>
        <Input placeholder="VD: Sản phẩm" />
      </Form.Item>
      <Form.Item name="parentId" label="Menu cha">
        <Select
          allowClear
          placeholder="Không chọn = menu gốc"
          options={menus
            .filter((m) => m.id !== excludeId)
            .map((m) => ({ value: m.id, label: `${m.code} — ${m.name}` }))}
        />
      </Form.Item>
      <Form.Item name="path" label="Đường dẫn sider">
        <Input placeholder="VD: /products (trống = không hiện sider)" />
      </Form.Item>
      <Form.Item name="icon" label="Icon">
        <Input placeholder="Tên icon AntD (tùy chọn)" />
      </Form.Item>
      <Form.Item name="sortOrder" label="Thứ tự">
        <InputNumber min={0} style={{ width: '100%' }} placeholder="0" />
      </Form.Item>
    </Form>
  );
}

function MenusTab() {
  const menusQuery = useListMenusQuery();
  const [createMenu, createState] = useCreateMenuMutation();
  const [updateMenu, updateState] = useUpdateMenuMutation();
  const [deleteMenu] = useDeleteMenuMutation();
  const [editing, setEditing] = useState<IamMenu | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();

  const menus = menusQuery.data?.items ?? [];
  const mutationError =
    (createState.error as { message?: string } | undefined)?.message ??
    (updateState.error as { message?: string } | undefined)?.message;

  const columns: ColumnsType<IamMenu> = [
    { title: 'Mã', dataIndex: 'code', key: 'code', render: (v: string) => <Typography.Text code>{v}</Typography.Text> },
    { title: 'Tên hiển thị', dataIndex: 'name', key: 'name' },
    { title: 'Đường dẫn', dataIndex: 'path', key: 'path', render: (v: string | null) => v ?? '—' },
    {
      title: 'Trạng thái', dataIndex: 'active', key: 'active', width: 140,
      render: (v: boolean) => <StatusTag status={v ? 'ACTIVE' : 'INACTIVE'} />,
    },
    {
      title: 'Thao tác', key: 'action', width: 200,
      render: (_: unknown, r: IamMenu) => {
        const isDefault = !!r.default;
        return (
          <Space>
            <Button size="small" type="link" onClick={() => { setEditing(r); editForm.setFieldsValue({ ...r, parentId: r.parentId || null }); }}>Sửa</Button>
            <Popconfirm
              title={`Xóa menu ${r.name}?`}
              description="Menu bị xóa mềm; các permission MENU.* thuộc menu này cũng bị xóa mềm. Menu còn menu con thì bị chặn — hãy xóa hoặc di chuyển menu con trước."
              okText="Xóa" okType="danger" cancelText="Hủy"
              disabled={isDefault}
              onConfirm={async () => {
                try { await deleteMenu(r.id).unwrap(); message.success(`Đã xóa menu ${r.name}`); }
                catch { message.error('Xóa menu thất bại'); }
              }}
            >
              <Button size="small" type="link" danger disabled={isDefault} title={isDefault ? 'Menu mặc định hệ thống không được xóa' : undefined}>Xóa</Button>
            </Popconfirm>
          </Space>
        );
      },
    },
  ];

  return (
    <div>
      <ApiErrorAlert message={mutationError ?? (menusQuery.error as { message?: string } | undefined)?.message ?? null} />
      {menusQuery.isLoading ? <ListSkeleton /> : menus.length === 0 ? (
        <Card><TableEmpty description="Chưa có menu" actionText="Tạo menu đầu tiên" onAction={() => setCreateOpen(true)} /></Card>
      ) : (
        <DataTable<IamMenu> columns={columns} data={menus} loading={menusQuery.isFetching} rowKey="id" />
      )}
      <Modal title="Tạo menu" open={createOpen} onCancel={() => setCreateOpen(false)} confirmLoading={createState.isLoading}
        okText="Tạo" cancelText="Hủy" destroyOnClose
        onOk={async () => {
          try {
            const v = await createForm.validateFields();
            await createMenu({ ...v, active: true }).unwrap();
            message.success(`Đã tạo menu ${v.code}`); setCreateOpen(false); createForm.resetFields();
          } catch { if (createState.error) message.error('Tạo menu thất bại'); }
        }}>
        <MenuForm form={createForm} menus={menus} />
      </Modal>
      <Modal title={`Sửa menu ${editing?.code ?? ''}`} open={!!editing} onCancel={() => setEditing(null)} confirmLoading={updateState.isLoading}
        okText="Lưu" cancelText="Hủy" destroyOnClose
        onOk={async () => {
          if (!editing) return;
          try {
            const v = await editForm.validateFields();
            await updateMenu({ id: editing.id, data: v }).unwrap();
            message.success(`Đã lưu menu ${editing.code}`); setEditing(null);
          } catch { if (updateState.error) message.error('Lưu menu thất bại'); }
        }}>
        <MenuForm form={editForm} menus={menus} excludeId={editing?.id ?? null} isEdit />
      </Modal>
    </div>
  );
}

function ActionsTab() {
  const actionsQuery = useListActionsQuery();
  const [createAction, createState] = useCreateActionMutation();
  const [updateAction, updateState] = useUpdateActionMutation();
  const [toggleAction] = useToggleActionMutation();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<IamAction | null>(null);
  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();

  const actions = actionsQuery.data?.items ?? [];

  const columns: ColumnsType<IamAction> = [
    { title: 'Mã', dataIndex: 'code', key: 'code', render: (v: string) => <Typography.Text code>{v}</Typography.Text> },
    { title: 'Tên', dataIndex: 'name', key: 'name' },
    {
      title: 'Kích hoạt', dataIndex: 'active', key: 'active', width: 130,
      render: (v: boolean, r: IamAction) => (
        <Switch checked={v} onChange={async () => {
          try { await toggleAction(r.id).unwrap(); }
          catch { message.error(`Bật/tắt action ${r.code} thất bại`); }
        }} />
      ),
    },
    {
      title: 'Thao tác', key: 'action', width: 160,
      render: (_: unknown, r: IamAction) => (
        <Button size="small" type="link" onClick={() => { setEditing(r); editForm.setFieldsValue(r); }}>Sửa</Button>
      ),
    },
  ];

  const actionForm = (form: ReturnType<typeof Form.useForm>[0]) => (
    <Form form={form} layout="vertical" preserve={false}>
      <Form.Item name="code" label="Mã action" rules={[{ required: true, message: 'Nhập mã action' }, { pattern: /^[A-Z_]+$/, message: 'VD: VIEW, CREATE, EDIT, DELETE, APPROVE' }]}>
        <Input placeholder="VD: APPROVE" />
      </Form.Item>
      <Form.Item name="name" label="Tên" rules={[{ required: true, message: 'Nhập tên action' }]}>
        <Input placeholder="VD: Duyệt" />
      </Form.Item>
    </Form>
  );

  return (
    <div>
      <ApiErrorAlert message={
        ((createState.error ?? updateState.error) as { message?: string } | undefined)?.message ??
        (actionsQuery.error as { message?: string } | undefined)?.message ?? null
      } />
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={() => setCreateOpen(true)}>Tạo action</Button>
      </Space>
      {actionsQuery.isLoading ? <ListSkeleton /> : actions.length === 0 ? (
        <Card><TableEmpty description="Chưa có action" actionText="Tạo action đầu tiên" onAction={() => setCreateOpen(true)} /></Card>
      ) : (
        <DataTable<IamAction> columns={columns} data={actions} loading={actionsQuery.isFetching} rowKey="id" />
      )}
      <Modal title="Tạo action" open={createOpen} onCancel={() => setCreateOpen(false)} confirmLoading={createState.isLoading}
        okText="Tạo" cancelText="Hủy" destroyOnClose
        onOk={async () => {
          try {
            const v = await createForm.validateFields();
            await createAction(v).unwrap();
            message.success(`Đã tạo action ${v.code}`); setCreateOpen(false); createForm.resetFields();
          } catch { if (createState.error) message.error('Tạo action thất bại'); }
        }}>
        {actionForm(createForm)}
      </Modal>
      <Modal title={`Sửa action ${editing?.code ?? ''}`} open={!!editing} onCancel={() => setEditing(null)} confirmLoading={updateState.isLoading}
        okText="Lưu" cancelText="Hủy" destroyOnClose
        onOk={async () => {
          if (!editing) return;
          try {
            const v = await editForm.validateFields();
            await updateAction({ id: editing.id, data: v }).unwrap();
            message.success(`Đã lưu action ${editing.code}`); setEditing(null);
          } catch { if (updateState.error) message.error('Lưu action thất bại'); }
        }}>
        {actionForm(editForm)}
      </Modal>
    </div>
  );
}

export default function AdminMenusPage() {
  const menusQuery = useListMenusQuery();
  const actionsQuery = useListActionsQuery();
  const menus = menusQuery.data?.items ?? [];
  const actions = actionsQuery.data?.items ?? [];
  return (
    <div>
      <PageHeader
        title="Menus & Actions"
        subTitle="Danh mục chức năng và hành động — nền của chuỗi Menu → Action → Permission"
        extra={<Typography.Text type="secondary">Permission code = MENU.ACTION</Typography.Text>}
      />
      <SummaryRow
        items={[
          { label: 'Số menu', value: menus.length, icon: <AppstoreOutlined /> },
          { label: 'Số action', value: actions.length, icon: <ThunderboltOutlined />, color: '#0F4C81', hint: 'VIEW/CREATE/EDIT/DELETE/APPROVE', link: '/admin/permissions' },
        ]}
      />
      <Tabs
        items={[
          { key: 'menus', label: 'Menus', children: <MenusTab /> },
          { key: 'actions', label: 'Actions', children: <ActionsTab /> },
        ]}
      />
    </div>
  );
}
