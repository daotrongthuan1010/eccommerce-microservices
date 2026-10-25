import React from 'react';
import { Alert, Avatar, Button, Card, Divider, Empty, Form, Result, Skeleton, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { Link } from 'react-router-dom';

// DESIGN_GUIDE §8 — shared UI catalog. Moi list page: PageHeader →
// SummaryRow → FilterBar → DataTable (size=middle, scroll x 900, action
// fixed right, server pagination page+1) → Drawer 560.
export function PageHeader({
  title,
  subTitle,
  extra,
}: {
  title: string;
  subTitle?: string;
  extra?: React.ReactNode;
}) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
      <div>
        <Typography.Title level={4} style={{ margin: 0 }}>
          {title}
        </Typography.Title>
        {subTitle && <Typography.Text type="secondary">{subTitle}</Typography.Text>}
      </div>
      <Space>{extra}</Space>
    </div>
  );
}

export function FilterBar({ children }: { children: React.ReactNode }) {
  return (
    <Card size="small" style={{ marginBottom: 16 }}>
      <Space wrap>{children}</Space>
    </Card>
  );
}

export function SummaryRow({ items }: { items: { label: string; value: React.ReactNode; link?: string; icon?: React.ReactNode; color?: string; hint?: string }[] }) {
  return (
    <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
      {items.map((it) => (
        <Card key={it.label} size="small" style={{ flex: 1 }} title={undefined}>
          <Space size={8} align="start">
            {it.icon && <span style={{ color: it.color ?? '#667085', fontSize: 16 }}>{it.icon}</span>}
            <span>
              <Typography.Text type="secondary" style={{ fontSize: 13 }}>{it.label}</Typography.Text>
              <div style={{ fontSize: 20, fontWeight: 600, color: it.color ?? '#101828', fontVariantNumeric: 'tabular-nums' }}>
                {it.link ? <Link to={it.link}>{it.value}</Link> : it.value}
              </div>
              {it.hint && <Typography.Text type="secondary" style={{ fontSize: 12 }}>{it.hint}</Typography.Text>}
            </span>
          </Space>
        </Card>
      ))}
    </div>
  );
}

// Map status code → nhan tieng Viet than thien. Tag luon nhat
// (bordered=false), mau theo vai tro — khong tu <Tag color="blue/red"> dam.
const STATUS_META: Record<string, { color: string; label: string }> = {
  ACTIVE: { color: 'success', label: 'Đang hoạt động' },
  ENABLED: { color: 'success', label: 'Đang hoạt động' },
  APPROVED: { color: 'success', label: 'Đã duyệt' },
  PUBLISHED: { color: 'success', label: 'Đã xuất bản' },
  VERIFIED: { color: 'success', label: 'Đã xác thực' },
  PENDING: { color: 'warning', label: 'Chờ duyệt' },
  DRAFT: { color: 'default', label: 'Nháp' },
  INACTIVE: { color: 'default', label: 'Ngừng hoạt động' },
  DISABLED: { color: 'error', label: 'Vô hiệu hóa' },
  REJECTED: { color: 'error', label: 'Đã từ chối' },
  LOCKED: { color: 'error', label: 'Đã khóa' },
};

export function StatusTag({ status }: { status: string }) {
  const meta = STATUS_META[status];
  return <Tag bordered={false} color={meta?.color ?? 'default'}>{meta?.label ?? status}</Tag>;
}

// Minh hoa empty theo domain — SVG net don gian, muted, khong dung
// Empty mac dinh cua AntD (DESIGN_GUIDE §8.6, §15.6).
export function EmptyIllustration({ variant = 'users' }: { variant?: 'users' | 'roles' | 'generic' }) {
  return (
    <svg width="96" height="72" viewBox="0 0 96 72" fill="none" aria-hidden="true">
      <rect x="8" y="10" width="80" height="52" rx="8" fill="#F2F4F7" stroke="#EAECF0" />
      {variant === 'roles' ? (
        <g stroke="#98A2B3" strokeWidth="2" strokeLinecap="round">
          <circle cx="48" cy="30" r="8" fill="#fff" />
          <path d="M34 52c2-8 8-12 14-12s12 4 14 12" fill="#fff" />
          <circle cx="30" cy="34" r="5" fill="#fff" />
          <circle cx="66" cy="34" r="5" fill="#fff" />
        </g>
      ) : (
        <g stroke="#98A2B3" strokeWidth="2" strokeLinecap="round">
          <circle cx="48" cy="28" r="9" fill="#fff" />
          <path d="M32 54c3-9 9-13 16-13s13 4 16 13" fill="#fff" />
        </g>
      )}
      <circle cx="76" cy="50" r="10" fill="#0F4C81" opacity="0.12" />
      <path d="M72 50h8M76 46v8" stroke="#0F4C81" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

export function TableEmpty({ description = 'Không có dữ liệu', actionText, onAction, illustration = 'generic' }: { description?: string; actionText?: string; onAction?: () => void; illustration?: 'users' | 'roles' | 'generic' }) {
  return (
    <Empty
      description={description}
      image={<EmptyIllustration variant={illustration} />}
    >
      {actionText && onAction && (
        <Button type="primary" onClick={onAction}>
          {actionText}
        </Button>
      )}
    </Empty>
  );
}

// Mau avatar on dinh theo username — cung 1 user luon 1 mau o moi man
// hinh khi chua co anh dai dien that (picture tu PUT /auth/me/avatar,
// resolve URL qua file-service GET /files/url).
const AVATAR_COLORS = [
  '#0F4C81', '#1D6F42', '#7C3AED', '#B42318', '#9A6700',
  '#026AA2', '#6B21A8', '#0E7490', '#4D7C0F', '#BE123C',
];

export function avatarColor(name: string): string {
  let h = 0;
  for (let i = 0; i < name.length; i += 1) h = (h * 31 + name.charCodeAt(i)) >>> 0;
  return AVATAR_COLORS[h % AVATAR_COLORS.length];
}

function initials(name: string): string {
  const clean = name.trim();
  if (!clean) return '?';
  const parts = clean.split(/[\s._-]+/).filter(Boolean);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

// Avatar nguoi dung: anh that neu co (picture), neu khong thi chu cai
// dau voi nen mau on dinh theo username.
export function UserAvatar({
  name,
  picture,
  size = 32,
}: {
  name?: string | null;
  picture?: string | null;
  size?: number | 'small' | 'default' | 'large';
}) {
  const label = name?.trim() || '?';
  if (picture) return <Avatar size={size} src={picture} alt={label} />;
  const px = typeof size === 'number' ? size : undefined;
  return (
    <Avatar
      size={size}
      alt={label}
      style={{
        backgroundColor: avatarColor(label),
        fontWeight: 600,
        fontSize: px ? Math.max(12, Math.round(px * 0.38)) : undefined,
        flexShrink: 0,
      }}
    >
      {initials(label)}
    </Avatar>
  );
}

// Nhan vai tro: ten tieng Viet than thien + tooltip giu ten goc Keycloak.
const ROLE_META: Record<string, { color: string; label: string }> = {
  ADMIN: { color: 'red', label: 'Quản trị viên' },
  MODERATOR: { color: 'orange', label: 'Kiểm duyệt' },
  USER: { color: 'blue', label: 'Người dùng' },
};

export function RoleTag({ role, closable, onClose }: { role: string; closable?: boolean; onClose?: (e: React.MouseEvent<HTMLElement>) => void }) {
  const meta = ROLE_META[role];
  return (
    <Tag bordered={false} color={meta?.color ?? 'default'} title={meta ? `Keycloak role: ${role}` : role} closable={closable} onClose={onClose}>
      {meta?.label ?? role}
    </Tag>
  );
}

export function RoleTags({ roles, admin }: { roles: string[]; admin?: boolean }) {
  if (roles.length === 0 && !admin) return <>—</>;
  return (
    <Space wrap size={[4, 4]}>
      {roles.map((r) => (
        <RoleTag key={r} role={r} />
      ))}
      {admin && !roles.includes('ADMIN') && <RoleTag role="ADMIN" />}
    </Space>
  );
}

export function ListSkeleton() {
  return <Skeleton active paragraph={{ rows: 6 }} />;
}

export function FormSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <>
      <Divider orientation="left">{title}</Divider>
      {children}
    </>
  );
}

export function ActionBar({ children }: { children: React.ReactNode }) {
  return <Space style={{ marginTop: 16 }}>{children}</Space>;
}

export function DataTable<T extends object>(props: {
  columns: ColumnsType<T>;
  data: T[];
  loading?: boolean;
  total?: number;
  page?: number;
  onPageChange?: (page: number) => void;
  rowKey?: string;
}) {
  const { columns, data, loading, total, page = 0, onPageChange, rowKey = 'id' } = props;
  return (
    <Table<T>
      size="middle"
      scroll={{ x: 900 }}
      rowKey={rowKey}
      columns={columns.map((c, i) =>
        i === columns.length - 1 ? { ...c, fixed: 'right' } : c,
      )}
      dataSource={data}
      loading={loading}
      pagination={
        total !== undefined
          ? {
              total,
              current: page + 1,
              onChange: (p) => onPageChange?.(p - 1),
              showSizeChanger: true,
              showTotal: (t) => `Tổng ${t} bản ghi`,
            }
          : false
      }
      locale={{ emptyText: <TableEmpty /> }}
    />
  );
}

export function ApiErrorAlert({ message }: { message?: string | null }) {
  if (!message) return null;
  return <Alert type="error" message={message} showIcon style={{ marginBottom: 16 }} />;
}

export class ErrorBoundary extends React.Component<
  { children: React.ReactNode },
  { hasError: boolean }
> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { hasError: false };
  }
  static getDerivedStateFromError() {
    return { hasError: true };
  }
  render() {
    if (this.state.hasError) {
      return (
        <Result
          status="500"
          title="Có lỗi xảy ra"
          subTitle="Vui lòng tải lại trang."
          extra={<Button type="primary" onClick={() => window.location.reload()}>Tải lại</Button>}
        />
      );
    }
    return this.props.children;
  }
}

export { Form };
