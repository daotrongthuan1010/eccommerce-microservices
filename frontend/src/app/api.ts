import axios, { type AxiosRequestConfig } from 'axios';
import { store } from './store';
import keycloak from '../features/auth/keycloak';

// FE_AUTH_GATEWAY §4 — axios instance duy nhat, memory tokens, single-flight refresh.
export const apiBaseUrl: string =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://162.4.177.91/api';

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
        window.location.assign('/login');
        return Promise.reject(err);
      }
    }
    if (err.response?.status === 401) {
      store.dispatch({ type: 'auth/loggedOut' });
      if (window.location.pathname !== '/login') window.location.assign('/login');
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
