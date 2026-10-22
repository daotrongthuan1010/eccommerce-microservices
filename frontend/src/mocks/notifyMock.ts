// MOCK — notification shapes + local helpers.
// Unlock condition: thay bang API that khi
// `services/notification-service` co `*Controller.java` voi @RequestMapping duoc
// ghi nhan trong FE_API_CONTRACT (hien tai Phu luc A: chua co controller nao).
// KHONG goi /api/notification/** (BE tra 404). Khong gan nhan thuc te.

import { mockDelay, mockId } from './db';

export interface AppNotification {
  id: string;
  title: string;
  body: string;
  read: boolean;
  createdAt: string;
}

let localNotifications: AppNotification[] = [
  {
    id: 'notif-seed-1',
    title: 'Don hang ORD-0001 da xac nhan',
    body: 'Don hang cua ban dang duoc chuan bi.',
    read: false,
    createdAt: new Date(Date.now() - 3600000 * 5).toISOString(),
  },
];

export async function listNotifications(): Promise<AppNotification[]> {
  await mockDelay();
  return [...localNotifications];
}

export async function pushNotification(title: string, body: string): Promise<AppNotification> {
  await mockDelay();
  const notif: AppNotification = {
    id: mockId('notif'),
    title,
    body,
    read: false,
    createdAt: new Date().toISOString(),
  };
  localNotifications = [notif, ...localNotifications];
  return notif;
}

export async function markAllRead(): Promise<void> {
  await mockDelay();
  localNotifications = localNotifications.map((n) => ({ ...n, read: true }));
}
