// MOCK — payment shapes + local helpers.
// Unlock condition: thay bang RTK Query that khi
// `services/payment-service` co `*Controller.java` voi @RequestMapping duoc
// ghi nhan trong FE_API_CONTRACT (hien tai Phu luc A: chua co controller nao).
// KHONG goi /api/payment/** (BE tra 404). Khong gan nhan thuc te.

import { mockDelay, mockId } from './db';

export interface PaymentRequest {
  orderId: string;
  amount: number;
  method: 'COD' | 'BANK_TRANSFER' | 'E_WALLET';
}

export interface Payment {
  id: string;
  orderId: string;
  amount: number;
  method: PaymentRequest['method'];
  status: 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';
  createdAt: string;
}

const localPayments: Payment[] = [];

export async function createPayment(req: PaymentRequest): Promise<Payment> {
  await mockDelay(500);
  const payment: Payment = {
    id: mockId('pay'),
    orderId: req.orderId,
    amount: req.amount,
    method: req.method,
    status: req.method === 'COD' ? 'PENDING' : 'PAID',
    createdAt: new Date().toISOString(),
  };
  localPayments.unshift(payment);
  return payment;
}

export async function getPaymentByOrderId(orderId: string): Promise<Payment | undefined> {
  await mockDelay();
  return localPayments.find((p) => p.orderId === orderId);
}

export async function listPayments(): Promise<Payment[]> {
  await mockDelay();
  return [...localPayments];
}
