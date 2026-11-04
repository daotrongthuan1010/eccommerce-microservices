import { createSlice, type PayloadAction } from '@reduxjs/toolkit';

// Marketplace guest cart (2026-11-05 fe-marketplace D5) — kieu Tiki:
// khach chua login van them duoc gio hang; du lieu nam localStorage;
// sau login merge vao cart server (POST tung item /api/order/cart/items).
// Gia unitPrice chi la gia TAM TINH luc them — tong that do BE tinh khi
// checkout (AGENTS.md luat 3: FE khong tu ket luan gia cuoi).

export interface GuestCartItem {
  skuId: string;
  productName: string;
  unitPrice: number; // VND long, tam tinh
  quantity: number;
  imageKey?: string | null;
}

export interface GuestCartState {
  items: GuestCartItem[];
  /** Dang merge len server sau login (chong double merge). */
  merging: boolean;
  mergeError: string | null;
}

const STORAGE_KEY = 'ecommerce.guest-cart.v1';

function loadFromStorage(): GuestCartItem[] {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as GuestCartItem[];
    if (!Array.isArray(parsed)) return [];
    return parsed.filter(
      (i) => i && typeof i.skuId === 'string' && typeof i.quantity === 'number' && i.quantity > 0,
    );
  } catch {
    return [];
  }
}

function persist(items: GuestCartItem[]) {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  } catch {
    // localStorage full/bi chan: bo qua, gio chi con trong memory phien nay.
  }
}

const initialState: GuestCartState = {
  items: loadFromStorage(),
  merging: false,
  mergeError: null,
};

const guestCartSlice = createSlice({
  name: 'guestCart',
  initialState,
  reducers: {
    addItem(state, action: PayloadAction<GuestCartItem>) {
      const item = action.payload;
      if (!item.skuId || item.quantity < 1) return;
      const existing = state.items.find((i) => i.skuId === item.skuId);
      if (existing) {
        existing.quantity = Math.min(existing.quantity + item.quantity, 999);
        existing.unitPrice = item.unitPrice; // cap nhat gia tam tinh moi nhat
        existing.productName = item.productName;
        if (item.imageKey !== undefined) existing.imageKey = item.imageKey;
      } else {
        state.items.push({ ...item, quantity: Math.min(item.quantity, 999) });
      }
      persist(state.items);
    },
    updateQuantity(state, action: PayloadAction<{ skuId: string; quantity: number }>) {
      const { skuId, quantity } = action.payload;
      const item = state.items.find((i) => i.skuId === skuId);
      if (!item) return;
      if (quantity < 1) {
        state.items = state.items.filter((i) => i.skuId !== skuId);
      } else {
        item.quantity = Math.min(quantity, 999);
      }
      persist(state.items);
    },
    removeItem(state, action: PayloadAction<string>) {
      state.items = state.items.filter((i) => i.skuId !== action.payload);
      persist(state.items);
    },
    clearCart(state) {
      state.items = [];
      persist(state.items);
    },
    // Merge flow (goi tu component sau khi co token):
    mergeStarted(state) {
      state.merging = true;
      state.mergeError = null;
    },
    mergeDone(state) {
      state.merging = false;
      state.items = [];
      persist(state.items);
    },
    mergeFailed(state, action: PayloadAction<string>) {
      state.merging = false;
      state.mergeError = action.payload;
      // Giu nguyen items local de lan sau thu lai (khong mat du lieu khach).
    },
  },
});

export const {
  addItem,
  updateQuantity,
  removeItem,
  clearCart,
  mergeStarted,
  mergeDone,
  mergeFailed,
} = guestCartSlice.actions;

export function guestCartCount(items: GuestCartItem[]): number {
  return items.reduce((sum, i) => sum + i.quantity, 0);
}

export function guestCartAmount(items: GuestCartItem[]): number {
  return items.reduce((sum, i) => sum + i.unitPrice * i.quantity, 0);
}

export default guestCartSlice.reducer;
