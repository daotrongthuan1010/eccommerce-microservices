import axios, { type AxiosRequestConfig } from 'axios';
import { store } from './store';
import keycloak from '../features/auth/keycloak';

// FE_AUTH_GATEWAY §4 — axios instance duy nhat, memory tokens, single-flight refresh.
export const apiBaseUrl: string =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'https://shop-hatd.online/api';

export const api = axios.create({ baseURL: apiBaseUrl });

api.interceptors.request.use((cfg) => {
  const t = (store.getState() as { auth?: { accessToken?: string | null } }).auth?.accessToken;
  if (t) cfg.headers.Authorization = `Bearer ${t}`;
  return cfg;
});

let refreshing: Promise<string> | null = null;

api.interceptors.response.use(
  (r) => r,
  async (err) => {
    const orig = err.config as AxiosRequestConfig & { _retried?: boolean };
    if (err.response?.status === 401 && !orig._retried) {
      orig._retried = true;
      refreshing ??= (async () => {
        // SSO Keycloak: silent refresh qua keycloak.updateToken truoc,
        // fallback sang POST /auth/refresh (password-grant cu).
        if (keycloak.authenticated) {
          try {
            await keycloak.updateToken(30);
            if (keycloak.token && keycloak.refreshToken) {
              const tokens = {
                access_token: keycloak.token,
                refresh_token: keycloak.refreshToken,
              };
              store.dispatch({ type: 'auth/refreshed', payload: tokens });
              return tokens.access_token as string;
            }
          } catch {
            // roi xuong fallback /auth/refresh
          }
        }
        const rt = (store.getState() as { auth?: { refreshToken?: string | null } }).auth
          ?.refreshToken;
        const { data } = await axios.post(`${apiBaseUrl}/auth/refresh`, { refreshToken: rt });
        store.dispatch({ type: 'auth/refreshed', payload: data });
        return data.access_token as string;
      })().finally(() => {
        refreshing = null;
      });
      try {
        orig.headers = { ...orig.headers, Authorization: `Bearer ${await refreshing}` };
        return api(orig);
      } catch {
        store.dispatch({ type: 'auth/loggedOut' });
        // Refresh fail: dang xuat lang le. Neu dang o trang public marketplace
        // → RETRY request KHONG kem Authorization (guest mode): API public
        // (browse/categories) tra 200 cho guest, data van hien. Token chet trong
        // header lam Spring Security tu choi ca endpoint public (401).
        const PUBLIC_PATHS = ['/', '/products', '/login', '/register'];
        const isPublic =
          PUBLIC_PATHS.includes(window.location.pathname) ||
          /^\/products\/[^/]+$/.test(window.location.pathname);
        if (isPublic) {
          const guestConfig = { ...orig, headers: { ...orig.headers } };
          delete (guestConfig.headers as Record<string, unknown>).Authorization;
          return api(guestConfig);
        }
        const isAdminArea =
          window.location.pathname.startsWith('/admin') || window.location.pathname.startsWith('/staff');
        if (window.location.pathname !== '/login') {
          window.location.assign(isAdminArea ? '/admin/login' : '/login');
        }
        return Promise.reject(err);
      }
    }
    if (err.response?.status === 401) {
      store.dispatch({ type: 'auth/loggedOut' });
      // Token chet lam Spring Security tu choi ca endpoint public khi header
      // Authorization con kem theo. Trang public marketplace (/, /products,
      // /products/:id): retry request KHONG token (guest) de data van hien;
      // khu can login: redirect dung login theo khu vuc.
      const PUBLIC_PATHS = ['/', '/products', '/login', '/register'];
      const isPublic =
        PUBLIC_PATHS.includes(window.location.pathname) ||
        /^\/products\/[^/]+$/.test(window.location.pathname);
      if (isPublic) {
        const guestConfig = { ...orig, headers: { ...orig.headers } };
        delete (guestConfig.headers as Record<string, unknown>).Authorization;
        return api(guestConfig);
      }
      if (window.location.pathname !== '/login' && window.location.pathname !== '/admin/login') {
        // Khu backoffice -> login quan tri; khu shopper can login -> /login.
        const isAdminArea = window.location.pathname.startsWith('/admin') || window.location.pathname.startsWith('/staff');
        window.location.assign(isAdminArea ? '/admin/login' : '/login');
      }
    }
    return Promise.reject(err);
  },
);

// Envelope chuan ApiResponse (FE_API_CONTRACT §0.3). Auth login/refresh tra
// Keycloak Map tho — khong feed qua kieu nay.
export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
  traceId?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// axiosBaseQuery cho RTK Query data slices: giu interceptor + envelope parsing.
import type { BaseQueryFn } from '@reduxjs/toolkit/query';
import type { AxiosError } from 'axios';

export const axiosBaseQuery =
  (): BaseQueryFn<
    { url: string; method?: string; data?: unknown; params?: unknown; headers?: Record<string, string> },
    unknown,
    { status?: number; message: string }
  > =>
  async ({ url, method = 'get', data, params, headers }) => {
    try {
      const res = await api({ url, method, data, params, headers });
      return { data: res.data };
    } catch (e) {
      const axiosErr = e as AxiosError<{ message?: string; error?: string }>;
      return {
        error: {
          status: axiosErr.response?.status,
          message:
            axiosErr.response?.data?.message ??
            axiosErr.response?.data?.error ??
            axiosErr.message,
        },
      };
    }
  };
