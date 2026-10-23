import { configureStore } from '@reduxjs/toolkit';
import authReducer from '../features/auth/authSlice';
import { catalogApi } from '../features/catalog/catalogApi';
import { productApi } from '../features/product/productApi';
import { inventoryApi } from '../features/inventory/inventoryApi';
import { reviewApi } from '../features/review/reviewApi';
import { fileApi } from '../features/file/fileApi';
import { userApi } from '../features/user/userApi';
import { adminApi } from '../features/admin/adminApi';

// Store: auth (memory tokens) + RTK Query data slices. Mock slices
// (order/payment/search/notification/analytics) la du lieu local trong
// src/mocks — khong dang ky reducer bat dong bo o day.
export const store = configureStore({
  reducer: {
    auth: authReducer,
    [catalogApi.reducerPath]: catalogApi.reducer,
    [productApi.reducerPath]: productApi.reducer,
    [inventoryApi.reducerPath]: inventoryApi.reducer,
    [reviewApi.reducerPath]: reviewApi.reducer,
    [fileApi.reducerPath]: fileApi.reducer,
    [userApi.reducerPath]: userApi.reducer,
    [adminApi.reducerPath]: adminApi.reducer,
  },
  middleware: (getDefault) =>
    getDefault().concat(
      catalogApi.middleware,
      productApi.middleware,
      inventoryApi.middleware,
      reviewApi.middleware,
      fileApi.middleware,
      userApi.middleware,
      adminApi.middleware,
    ),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
