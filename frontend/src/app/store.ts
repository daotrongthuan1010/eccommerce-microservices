import { configureStore } from '@reduxjs/toolkit';
import authReducer from '../features/auth/authSlice';
import guestCartReducer from '../features/marketplace/guestCartSlice';
import { catalogApi } from '../features/catalog/catalogApi';
import { productApi } from '../features/product/productApi';
import { inventoryApi } from '../features/inventory/inventoryApi';
import { reviewApi } from '../features/review/reviewApi';
import { fileApi } from '../features/file/fileApi';
import { userApi } from '../features/user/userApi';
import { staffApi } from '../features/user/staffApi';
import { adminApi } from '../features/admin/adminApi';
import { iamApi } from '../features/admin/iamApi';
import { browseApi } from '../features/marketplace/browseApi';
import { orderApi } from '../features/order/orderApi';
import { searchApi } from '../features/search/searchApi';
import { adminOrderApi } from '../features/order/adminOrderApi';
import { paymentApi } from '../features/payment/paymentApi';
import { analyticsApi } from '../features/analytics/analyticsApi';

// Store: auth (memory tokens) + guest cart (localStorage, marketplace) + RTK
// Query data slices. Mock slices con lai (payment/search/notification/analytics)
// la du lieu local trong src/mocks — khong dang ky reducer.
export const store = configureStore({
  reducer: {
    auth: authReducer,
    guestCart: guestCartReducer,
    [catalogApi.reducerPath]: catalogApi.reducer,
    [productApi.reducerPath]: productApi.reducer,
    [inventoryApi.reducerPath]: inventoryApi.reducer,
    [reviewApi.reducerPath]: reviewApi.reducer,
    [fileApi.reducerPath]: fileApi.reducer,
    [userApi.reducerPath]: userApi.reducer,
    [staffApi.reducerPath]: staffApi.reducer,
    [adminApi.reducerPath]: adminApi.reducer,
    [iamApi.reducerPath]: iamApi.reducer,
    [browseApi.reducerPath]: browseApi.reducer,
    [orderApi.reducerPath]: orderApi.reducer,
    [searchApi.reducerPath]: searchApi.reducer,
    [adminOrderApi.reducerPath]: adminOrderApi.reducer,
    [paymentApi.reducerPath]: paymentApi.reducer,
    [analyticsApi.reducerPath]: analyticsApi.reducer,
  },
  middleware: (getDefault) =>
    getDefault().concat(
      catalogApi.middleware,
      productApi.middleware,
      inventoryApi.middleware,
      reviewApi.middleware,
      fileApi.middleware,
      userApi.middleware,
      staffApi.middleware,
      adminApi.middleware,
      iamApi.middleware,
      browseApi.middleware,
      orderApi.middleware,
      searchApi.middleware,
      adminOrderApi.middleware,
      paymentApi.middleware,
      analyticsApi.middleware,
    ),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
