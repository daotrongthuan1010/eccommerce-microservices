// Mock shared helpers — KHONG gan nhan thuc te (real).
// Unlock condition: chi thay bang API that khi backend co controller tuong ung
// (xem tung file mock; FE_API_CONTRACT Phu luc A).

export const MOCK_DELAY_MS = 300;

export function mockDelay(ms: number = MOCK_DELAY_MS): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

export interface MockPage<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export function mockPage<T>(items: T[], page = 0, size = 20): MockPage<T> {
  const totalElements = items.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / size));
  return {
    content: items.slice(page * size, page * size + size),
    page,
    size,
    totalElements,
    totalPages,
  };
}

export function mockId(prefix: string): string {
  return `${prefix}-${Date.now().toString(36)}-${Math.floor(Math.random() * 1e6).toString(36)}`;
}
