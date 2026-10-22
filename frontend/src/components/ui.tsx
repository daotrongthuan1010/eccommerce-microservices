import React from 'react';
import { Alert, Button, Card, Divider, Empty, Form, Result, Skeleton, Space, Table, Tag, Typography } from 'antd';
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

export function SummaryRow({ items }: { items: { label: string; value: React.ReactNode; link?: string }[] }) {
  return (
    <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
      {items.map((it) => (
        <Card key={it.label} size="small" style={{ flex: 1 }}>
          <Typography.Text type="secondary">{it.label}</Typography.Text>
          <div style={{ fontSize: 20, fontWeight: 600 }}>
            {it.link ? <Link to={it.link}>{it.value}</Link> : it.value}
          </div>
        </Card>
      ))}
    </div>
  );
}

export function StatusTag({ status }: { status: string }) {
  const colorMap: Record<string, string> = {
    ACTIVE: 'success',
    APPROVED: 'success',
    PUBLISHED: 'success',
    PENDING: 'warning',
    DRAFT: 'default',
    REJECTED: 'error',
    INACTIVE: 'default',
  };
  return <Tag bordered={false} color={colorMap[status] ?? 'default'}>{status}</Tag>;
}

export function TableEmpty({ description = 'Khong co du lieu' }: { description?: string }) {
  return <Empty description={description} />;
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
              showSizeChanger: false,
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
          title="Co loi xay ra"
          subTitle="Vui long tai lai trang."
          extra={<Button type="primary" onClick={() => window.location.reload()}>Tai lai</Button>}
        />
      );
    }
    return this.props.children;
  }
}

export { Form };
