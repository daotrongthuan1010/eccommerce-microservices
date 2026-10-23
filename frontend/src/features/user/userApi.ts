import { createApi } from '@reduxjs/toolkit/query/react';
import { axiosBaseQuery } from '../../app/api';

// FE_API_CONTRACT §2: GET /api/user/me (Bearer) — cung shape voi GET /api/auth/me.
// Ca hai tra raw Map (KHONG envelope ApiResponse), giong authSlice.fetchMe.
export interface UserMe {
  subject: string;
  username: string;
  email?: string;
  roles: string[];
  admin: boolean;
}

export const userApi = createApi({
  reducerPath: 'userApi',
  baseQuery: axiosBaseQuery(),
  endpoints: (build) => ({
    // real 2026-10-22: GET /api/user/me — server tra raw Map, khong envelope
    getMe: build.query<UserMe, void>({
      query: () => ({ url: '/user/me', method: 'get' }),
    }),
  }),
});

export const { useGetMeQuery } = userApi;

// Profile view-model: map raw /me Map → typed, defensive defaults (prod
// tra thieu field optional thi render "—", khong bao gio trang).
export interface Profile {
  subject: string;
  username: string;
  email: string;
  fullName: string;
  roles: string[];
  admin: boolean;
  enabled: boolean;
  emailVerified: boolean | null;
}

const FALLBACK = '—';

export function toProfile(me: Partial<UserMe & { firstName?: string; lastName?: string; enabled?: boolean; emailVerified?: boolean }> | null | undefined): Profile | null {
  if (!me || (!me.username && !me.subject)) return null;
  const fullName = [me.firstName, me.lastName].filter(Boolean).join(' ').trim();
  return {
    subject: me.subject ?? FALLBACK,
    username: me.username ?? FALLBACK,
    email: me.email ?? FALLBACK,
    fullName: fullName || FALLBACK,
    roles: me.roles ?? [],
    admin: me.admin ?? false,
    enabled: me.enabled ?? true,
    emailVerified: me.emailVerified ?? null,
  };
}
