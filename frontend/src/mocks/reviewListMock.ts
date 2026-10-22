// MOCK — backend chưa có GET list/search review công khai
// (FE_API_CONTRACT §6 + Phụ lục B: mock nằm trong src/mocks, KHÔNG gắn
// nhãn real). Mở khóa khi search-service có API list review.
export interface ReviewListItem {
  id: string;
  productId: string;
  productName: string;
  author: string;
  rating: number;
  title: string;
  content: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'HIDDEN';
  helpfulCount: number;
  createdAt: string;
}

export const reviewListMock: ReviewListItem[] = [
  {
    id: 'rev-001',
    productId: 'prod-001',
    productName: 'Ao thun cotton',
    author: 'nguyen.van.a',
    rating: 5,
    title: 'Chat lieu tot',
    content: 'Vai mem, form dep, giao hang nhanh.',
    status: 'PENDING',
    helpfulCount: 12,
    createdAt: '2026-10-20T08:00:00Z',
  },
  {
    id: 'rev-002',
    productId: 'prod-001',
    productName: 'Ao thun cotton',
    author: 'tran.thi.b',
    rating: 2,
    title: 'Size nho hon mo ta',
    content: 'Dat size L nhung chat, can doi size truoc khi mua.',
    status: 'PENDING',
    helpfulCount: 3,
    createdAt: '2026-10-21T02:30:00Z',
  },
  {
    id: 'rev-003',
    productId: 'prod-002',
    productName: 'Giay the thao',
    author: 'le.van.c',
    rating: 4,
    title: 'On trong tam gia',
    content: 'De em, di thoai mai. Mau sac dung anh.',
    status: 'APPROVED',
    helpfulCount: 7,
    createdAt: '2026-10-19T14:15:00Z',
  },
];
