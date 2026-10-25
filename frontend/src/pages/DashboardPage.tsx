import { useEffect, useState } from 'react';
import { Card, Typography } from 'antd';
import {
  getDashboardKpi,
  getOrderStatusBreakdown,
  getRevenueSeries,
  type DashboardKpi,
  type OrderStatusSlice,
  type RevenuePoint,
} from '../mocks/analyticsMock';
import { ApiErrorAlert, ListSkeleton, PageHeader, SummaryRow } from '../components/ui';
import { themeToken } from '../app/theme';

// Wave 4.2 + DESIGN_GUIDE §14 — dashboard mock: KPI bam duoc (drill-down),
// sparkline + donut SVG inline. Mock data only — KHONG goi /api/analytics/**.

function Sparkline({ points }: { points: RevenuePoint[] }) {
  const w = 260;
  const h = 64;
  const max = Math.max(...points.map((p) => p.revenue), 1);
  const step = points.length > 1 ? w / (points.length - 1) : w;
  const d = points
    .map((p, i) => `${i === 0 ? 'M' : 'L'}${(i * step).toFixed(1)},${(h - (p.revenue / max) * (h - 8) - 4).toFixed(1)}`)
    .join(' ');
  return (
    <svg width={w} height={h} role="img" aria-label="Doanh thu 7 ngay">
      <path d={d} fill="none" stroke={themeToken.colorPrimary} strokeWidth={2} />
    </svg>
  );
}

function Donut({ slices }: { slices: OrderStatusSlice[] }) {
  const total = slices.reduce((s, x) => s + x.count, 0) || 1;
  const colors = [themeToken.colorPrimary, themeToken.colorSuccess, themeToken.colorWarning, themeToken.colorError];
  const r = 44;
  const c = 2 * Math.PI * r;
  let acc = 0;
  return (
    <svg width={120} height={120} role="img" aria-label="Tỉ lệ trạng thái đơn">
      <circle cx={60} cy={60} r={r} fill="none" stroke="#f0f0f0" strokeWidth={16} />
      {slices.map((s, i) => {
        const frac = s.count / total;
        const el = (
          <circle
            key={s.status}
            cx={60}
            cy={60}
            r={r}
            fill="none"
            stroke={colors[i % colors.length]}
            strokeWidth={16}
            strokeDasharray={`${(frac * c).toFixed(1)} ${c.toFixed(1)}`}
            strokeDashoffset={(-acc * c).toFixed(1)}
            transform="rotate(-90 60 60)"
          />
        );
        acc += frac;
        return el;
      })}
    </svg>
  );
}

export default function DashboardPage() {
  const [kpi, setKpi] = useState<DashboardKpi | null>(null);
  const [series, setSeries] = useState<RevenuePoint[]>([]);
  const [breakdown, setBreakdown] = useState<OrderStatusSlice[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([getDashboardKpi(), getRevenueSeries(), getOrderStatusBreakdown()])
      .then(([k, s, b]) => {
        setKpi(k);
        setSeries(s);
        setBreakdown(b);
      })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : 'Không tải được dashboard'));
  }, []);

  if (!kpi) return (
    <div>
      <PageHeader title="Dashboard (mock)" />
      {error ? <ApiErrorAlert message={error} /> : <ListSkeleton />}
    </div>
  );

  return (
    <div>
      <PageHeader title="Dashboard (mock)" subTitle="Số liệu giả — thay bằng analytics-service khi có BE" />
      <ApiErrorAlert message={error} />
      <SummaryRow
        items={[
          {
            label: 'Doanh thu 7 ngày',
            value: `${kpi.totalRevenue7d.toLocaleString('vi-VN')} ₫`,
            link: '/orders',
          },
          { label: 'Đơn hàng 7 ngày', value: kpi.totalOrders7d, link: '/orders' },
          { label: 'Phiếu nhập chờ duyệt', value: kpi.pendingReceipts, link: '/receipts' },
          { label: 'SKU sắp hết hàng', value: kpi.lowStockSkus, link: '/inventory' },
        ]}
      />
      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
        <Card size="small" title="Doanh thu 7 ngày (mock sparkline)">
          <Sparkline points={series} />
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            Tổng 7 ngày:{' '}
            <span style={{ fontVariantNumeric: 'tabular-nums' }}>
              {kpi.totalRevenue7d.toLocaleString('vi-VN')} ₫
            </span>
            {' · '}Bấm KPI để drill-down
          </Typography.Text>
        </Card>
        <Card size="small" title="Trạng thái đơn (mock donut)">
          <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
            <Donut slices={breakdown} />
            <div>
              {breakdown.map((s) => (
                <div key={s.status}>
                  {s.status}: <strong>{s.count}</strong>
                </div>
              ))}
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
