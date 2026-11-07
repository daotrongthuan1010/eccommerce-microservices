import { Card, Table, Typography } from 'antd';
import {
  useKpisQuery,
  useRevenueQuery,
  useTopProductsQuery,
  type RevenuePoint,
} from '../features/analytics/analyticsApi';
import { ApiErrorAlert, ListSkeleton, PageHeader, SummaryRow } from '../components/ui';
import { themeToken } from '../app/theme';

// Dashboard that: KPI tu /kpis, sparkline doanh thu 7 ngay tu /revenue,
// top 5 san pham tu /top-products (thay donut mock — khong con nguon du lieu).

function toISODate(d: Date): string {
  return d.toISOString().slice(0, 10);
}

function last7DaysRange(): { from: string; to: string } {
  const to = new Date();
  const from = new Date();
  from.setDate(to.getDate() - 6);
  return { from: toISODate(from), to: toISODate(to) };
}

function fmtVnd(v: number): string {
  return `${(v ?? 0).toLocaleString('vi-VN')} ₫`;
}

function Sparkline({ points }: { points: RevenuePoint[] }) {
  const w = 260;
  const h = 64;
  const max = Math.max(...points.map((p) => p.grossRevenue), 1);
  const step = points.length > 1 ? w / (points.length - 1) : w;
  const d = points
    .map(
      (p, i) =>
        `${i === 0 ? 'M' : 'L'}${(i * step).toFixed(1)},${(h - (p.grossRevenue / max) * (h - 8) - 4).toFixed(1)}`,
    )
    .join(' ');
  return (
    <svg width={w} height={h} role="img" aria-label="Doanh thu 7 ngay">
      <path d={d} fill="none" stroke={themeToken.colorPrimary} strokeWidth={2} />
    </svg>
  );
}

export default function DashboardPage() {
  const range = last7DaysRange();
  const kpisQ = useKpisQuery();
  const revenueQ = useRevenueQuery({ ...range, granularity: 'day' });
  const topQ = useTopProductsQuery({ ...range, limit: 5 });

  const kpis = kpisQ.data?.data;
  const series: RevenuePoint[] = revenueQ.data?.data ?? [];
  const top = topQ.data?.data ?? [];
  const loading = kpisQ.isLoading || revenueQ.isLoading || topQ.isLoading;
  const error =
    (kpisQ.error as { message?: string } | undefined)?.message ??
    (revenueQ.error as { message?: string } | undefined)?.message ??
    (topQ.error as { message?: string } | undefined)?.message ??
    null;

  if (loading && !kpis) {
    return (
      <div>
        <PageHeader title="Dashboard" />
        <ListSkeleton />
      </div>
    );
  }

  return (
    <div>
      <PageHeader title="Dashboard" subTitle="Số liệu thật từ analytics-service" />
      <ApiErrorAlert message={error} />
      <SummaryRow
        items={[
          {
            label: 'Doanh thu hôm nay',
            value: kpis ? fmtVnd(kpis.todayRevenue) : '—',
            link: '/admin/orders',
          },
          { label: 'Đơn hàng hôm nay', value: kpis?.todayOrders ?? '—', link: '/admin/orders' },
          {
            label: 'Doanh thu 7 ngày',
            value: kpis ? fmtVnd(kpis.last7DaysRevenue) : '—',
            link: '/admin/orders',
          },
          {
            label: 'Doanh thu 30 ngày',
            value: kpis ? fmtVnd(kpis.last30DaysRevenue) : '—',
            link: '/admin/orders',
          },
          {
            label: 'Giá trị TB/đơn (30d)',
            value: kpis ? fmtVnd(kpis.avgOrderValue30d) : '—',
          },
        ]}
      />
      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
        <Card size="small" title="Doanh thu 7 ngày">
          {revenueQ.isLoading ? (
            <ListSkeleton />
          ) : (
            <>
              <Sparkline points={series} />
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                Tổng 7 ngày:{' '}
                <span style={{ fontVariantNumeric: 'tabular-nums' }}>
                  {fmtVnd(series.reduce((s, p) => s + (p.grossRevenue ?? 0), 0))}
                </span>
                {' · '}Bấm KPI để drill-down
              </Typography.Text>
            </>
          )}
        </Card>
        <Card size="small" title="Top 5 sản phẩm bán chạy (7 ngày)" style={{ minWidth: 480, flex: 1 }}>
          <Table
            size="small"
            rowKey="skuId"
            loading={topQ.isLoading}
            dataSource={top}
            pagination={false}
            columns={[
              { title: 'Sản phẩm', dataIndex: 'productName', key: 'productName' },
              {
                title: 'SL bán',
                dataIndex: 'totalQuantity',
                key: 'totalQuantity',
                width: 90,
                align: 'right' as const,
              },
              {
                title: 'Doanh thu',
                dataIndex: 'totalRevenue',
                key: 'totalRevenue',
                width: 160,
                align: 'right' as const,
                render: (v: number) => (
                  <span style={{ fontVariantNumeric: 'tabular-nums' }}>{fmtVnd(v)}</span>
                ),
              },
            ]}
          />
        </Card>
      </div>
    </div>
  );
}
