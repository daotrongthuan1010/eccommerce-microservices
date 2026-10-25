// MOCK — dashboard KPI series (mock data only).
// Unlock condition: thay bang API that khi
// `services/analytics-service` co `*Controller.java` voi @RequestMapping duoc
// ghi nhan trong FE_API_CONTRACT (hien tai Phu luc A: chua co controller nao).
// KHONG goi /api/analytics/** (BE tra 404). Khong gan nhan thuc te.

import { mockDelay } from './db';

export interface DashboardKpi {
  totalRevenue7d: number;
  totalOrders7d: number;
  pendingReceipts: number;
  lowStockSkus: number;
}

export interface RevenuePoint {
  day: string;
  revenue: number;
}

export interface OrderStatusSlice {
  status: string;
  count: number;
}

const revenueSeries: RevenuePoint[] = [
  { day: 'T2', revenue: 12500000 },
  { day: 'T3', revenue: 18200000 },
  { day: 'T4', revenue: 14900000 },
  { day: 'T5', revenue: 21400000 },
  { day: 'T6', revenue: 19800000 },
  { day: 'T7', revenue: 25600000 },
  { day: 'CN', revenue: 16100000 },
];

const orderStatusBreakdown: OrderStatusSlice[] = [
  { status: 'PENDING', count: 4 },
  { status: 'CONFIRMED', count: 9 },
  { status: 'SHIPPED', count: 6 },
  { status: 'DELIVERED', count: 21 },
];

export async function getDashboardKpi(): Promise<DashboardKpi> {
  await mockDelay();
  return {
    totalRevenue7d: revenueSeries.reduce((s, p) => s + p.revenue, 0),
    totalOrders7d: orderStatusBreakdown.reduce((s, p) => s + p.count, 0),
    pendingReceipts: 3,
    lowStockSkus: 5,
  };
}

export async function getRevenueSeries(): Promise<RevenuePoint[]> {
  await mockDelay();
  return [...revenueSeries];
}

export async function getOrderStatusBreakdown(): Promise<OrderStatusSlice[]> {
  await mockDelay();
  return [...orderStatusBreakdown];
}
