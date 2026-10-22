// MOCK — order/cart/checkout shapes + local helpers.
// Unlock condition: thay bang RTK Query that khi
// `services/order-service` co `*Controller.java` voi @RequestMapping duoc
// ghi nhan trong FE_API_CONTRACT (hien tai Phu luc A: chua co controller nao).
// KHONG goi /api/order/** (BE tra 404). Khong gan nhan thuc te.

import { mockDelay, mockId, mockPage, type MockPage } from './db';

export interface CartItem {
  skuId: string;
  sku: string;
  productName: string;
  price: number;
  quantity: number;
}

export interface Cart {
  items: CartItem[];
  totalQuantity: number;
  totalAmount: number;
}

export interface CheckoutRequest {
  items: CartItem[];
  shippingAddress: string;
  paymentMethod: string;
}

export interface Order {
  id: string;
  code: string;
  status: 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
  items: CartItem[];
  totalAmount: number;
  createdAt: string;
}

const seedOrders: Order[] = [
  {
    id: 'order-seed-1',
    code: 'ORD-0001',
    status: 'CONFIRMED',
    items: [
      { skuId: 'sku-1', sku: 'SKU-001', productName: 'Áo thun cotton', price: 199000, quantity: 2 },
    ],
    totalAmount: 398000,
    createdAt: new Date(Date.now() - 86400000 * 2).toISOString(),
  },
  {
    id: 'order-seed-2',
    code: 'ORD-0002',
    status: 'PENDING',
    items: [
      { skuId: 'sku-2', sku: 'SKU-002', productName: 'Giày sneaker', price: 899000, quantity: 1 },
    ],
    totalAmount: 899000,
    createdAt: new Date(Date.now() - 86400000).toISOString(),
  },
];

let localOrders: Order[] = [...seedOrders];
let localCart: CartItem[] = [];

function totals(items: CartItem[]): Pick<Cart, 'totalQuantity' | 'totalAmount'> {
  return {
    totalQuantity: items.reduce((s, i) => s + i.quantity, 0),
    totalAmount: items.reduce((s, i) => s + i.price * i.quantity, 0),
  };
}

export async function getCart(): Promise<Cart> {
  await mockDelay();
  return { items: [...localCart], ...totals(localCart) };
}

export async function addToCart(item: CartItem): Promise<Cart> {
  await mockDelay();
  const found = localCart.find((i) => i.skuId === item.skuId);
  if (found) {
    found.quantity += item.quantity;
  } else {
    localCart.push({ ...item });
  }
  return { items: [...localCart], ...totals(localCart) };
}

export async function removeFromCart(skuId: string): Promise<Cart> {
  await mockDelay();
  localCart = localCart.filter((i) => i.skuId !== skuId);
  return { items: [...localCart], ...totals(localCart) };
}

export async function clearCart(): Promise<Cart> {
  await mockDelay();
  localCart = [];
  return { items: [], totalQuantity: 0, totalAmount: 0 };
}

export async function checkout(req: CheckoutRequest): Promise<Order> {
  await mockDelay(500);
  const order: Order = {
    id: mockId('order'),
    code: `ORD-${String(localOrders.length + 1).padStart(4, '0')}`,
    status: 'PENDING',
    items: [...req.items],
    totalAmount: req.items.reduce((s, i) => s + i.price * i.quantity, 0),
    createdAt: new Date().toISOString(),
  };
  localOrders = [order, ...localOrders];
  localCart = [];
  return order;
}

export async function listOrders(page = 0, size = 20): Promise<MockPage<Order>> {
  await mockDelay();
  return mockPage(localOrders, page, size);
}

export async function getOrderById(id: string): Promise<Order | undefined> {
  await mockDelay();
  return localOrders.find((o) => o.id === id);
}
