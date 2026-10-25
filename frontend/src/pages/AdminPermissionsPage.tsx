import { useMemo, useState } from 'react';
import { App, Button, Card, Checkbox, Col, Popconfirm, Row, Select, Space, Typography } from 'antd';
import { SafetyOutlined } from '@ant-design/icons';
import {
  useListMenusQuery,
  useListActionsQuery,
  useListPermissionsQuery,
  useCreatePermissionMutation,
  useDeletePermissionMutation,
} from '../features/admin/iamApi';
import {
  ApiErrorAlert,
  ListSkeleton,
  PageHeader,
  SummaryRow,
  TableEmpty,
} from '../components/ui';

// /admin/permissions — RequireRole ADMIN. Ma tran cap permission MENU.ACTION:
// chon menu → checklist actions (co = da cap, bo check = thu hoi).
export default function AdminPermissionsPage() {
  const { message } = App.useApp();
  const menusQuery = useListMenusQuery();
  const actionsQuery = useListActionsQuery();
  const [menuId, setMenuId] = useState<string | undefined>(undefined);
  const permsQuery = useListPermissionsQuery(menuId ? { menuId } : undefined);
  const [createPermission, createState] = useCreatePermissionMutation();
  const [deletePermission, deleteState] = useDeletePermissionMutation();

  const menus = menusQuery.data?.items ?? [];
  const actions = useMemo(() => (actionsQuery.data?.items ?? []).filter((a) => a.active), [actionsQuery.data]);
  const granted = useMemo(() => new Map((permsQuery.data?.items ?? []).map((p) => [p.actionId, p])), [permsQuery.data]);

  const activeMenuId = menuId ?? menus[0]?.id;
  const activeMenu = menus.find((m) => m.id === activeMenuId);

  const onToggle = async (actionId: string, checked: boolean) => {
    if (!activeMenuId) return;
    try {
      if (checked) {
        await createPermission({ menuId: activeMenuId, actionId }).unwrap();
        message.success('Đã cấp permission');
      } else {
        const p = granted.get(actionId);
        if (p) {
          await deletePermission(p.id).unwrap();
          message.success('Đã thu hồi permission');
        }
      }
    } catch {
      message.error(checked ? 'Cấp permission thất bại' : 'Thu hồi permission thất bại');
    }
  };

  const loading = menusQuery.isLoading || actionsQuery.isLoading;
  const mutationError =
    (createState.error as { message?: string } | undefined)?.message ??
    (deleteState.error as { message?: string } | undefined)?.message ??
    (permsQuery.error as { message?: string } | undefined)?.message;

  return (
    <div>
      <PageHeader title="Phân quyền" subTitle="Ghép Menu × Action thành Permission (code MENU.ACTION)" />
      <ApiErrorAlert message={mutationError ?? null} />
      <SummaryRow
        items={[
          { label: 'Tổng permission', value: permsQuery.data?.total ?? '—', icon: <SafetyOutlined />, color: '#0F4C81', hint: activeMenu ? `menu ${activeMenu.code}` : undefined },
        ]}
      />
      {loading ? <ListSkeleton /> : menus.length === 0 ? (
        <Card><TableEmpty description="Chưa có menu — tạo menu trước" actionText="Sang Menus & Actions" onAction={() => { window.location.assign('/admin/menus'); }} /></Card>
      ) : (
        <Card>
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            <Space>
              <Typography.Text strong>Menu:</Typography.Text>
              <Select
                style={{ minWidth: 260 }}
                value={activeMenuId}
                onChange={setMenuId}
                options={menus.map((m) => ({ value: m.id, label: `${m.code} — ${m.name}` }))}
              />
            </Space>
            {permsQuery.isLoading ? <ListSkeleton /> : actions.length === 0 ? (
              <TableEmpty description="Chưa có action đang kích hoạt" />
            ) : (
              <Row gutter={[12, 12]}>
                {actions.map((a) => {
                  const p = granted.get(a.id);
                  return (
                    <Col key={a.id} xs={24} sm={12} md={8}>
                      <Card size="small">
                        <Space style={{ width: '100%', justifyContent: 'space-between' }}>
                          <Space>
                            <Checkbox checked={!!p} onChange={(e) => onToggle(a.id, e.target.checked)} />
                            <span>
                              <Typography.Text strong>{a.code}</Typography.Text>
                              <br />
                              <Typography.Text type="secondary" style={{ fontSize: 12 }}>{a.name}</Typography.Text>
                            </span>
                          </Space>
                          {p && (
                            <Popconfirm
                              title={`Thu hồi permission ${p.code}?`}
                              okText="Thu hồi" okType="danger" cancelText="Hủy"
                              onConfirm={() => onToggle(a.id, false)}
                            >
                              <Button size="small" type="link" danger>Thu hồi</Button>
                            </Popconfirm>
                          )}
                        </Space>
                        {p && <Typography.Text code style={{ fontSize: 12 }}>{p.code}</Typography.Text>}
                      </Card>
                    </Col>
                  );
                })}
              </Row>
            )}
          </Space>
        </Card>
      )}
    </div>
  );
}
