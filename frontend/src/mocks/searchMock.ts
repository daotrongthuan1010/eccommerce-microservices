// MOCK — search client-side filter tren mock catalog.
// Unlock condition: thay bang API that khi
// `services/search-service` co `*Controller.java` voi @RequestMapping duoc
// ghi nhan trong FE_API_CONTRACT (hien tai Phu luc A: chua co controller nao).
// KHONG goi /api/search/** (BE tra 404). Khong gan nhan thuc te.

import { mockDelay } from './db';

export interface MockProduct {
  id: string;
  name: string;
  sku: string;
  brand?: string;
  price: number;
}

export const mockCatalog: MockProduct[] = [
  { id: 'p-1', name: 'Áo thun cotton', sku: 'SKU-001', brand: 'Local', price: 199000 },
  { id: 'p-2', name: 'Giày sneaker', sku: 'SKU-002', brand: 'Local', price: 899000 },
  { id: 'p-3', name: 'Ba lô du lịch', sku: 'SKU-003', brand: 'Travel', price: 549000 },
  { id: 'p-4', name: 'Mũ lưỡi trai', sku: 'SKU-004', brand: 'Local', price: 129000 },
];

export async function searchProducts(keyword: string): Promise<MockProduct[]> {
  await mockDelay();
  const kw = keyword.trim().toLowerCase();
  if (!kw) return [...mockCatalog];
  return mockCatalog.filter(
    (p) =>
      p.name.toLowerCase().includes(kw) ||
      p.sku.toLowerCase().includes(kw) ||
      (p.brand ?? '').toLowerCase().includes(kw),
  );
}
