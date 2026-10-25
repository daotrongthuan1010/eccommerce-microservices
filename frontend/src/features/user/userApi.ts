import { createApi } from '@reduxjs/toolkit/query/react';
import { axiosBaseQuery } from '../../app/api';

// FE_API_CONTRACT §2: GET /api/auth/me (Bearer, enriched: firstName/
// lastName/picture/emailVerified tu Keycloak Admin API) — source that
// duy nhat cho ProfilePage + AppLayout. /api/user/me (user-service MeController,
// JWT claims only) khong dung nua vi thieu enrichment.
export interface UserMe {
  subject: string;
  username: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  roles: string[];
  admin: boolean;
  enabled?: boolean;
  emailVerified?: boolean;
  picture?: string | null;
}

export const userApi = createApi({
  reducerPath: 'userApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Me'],
  endpoints: (build) => ({
    // real 2026-10-23: GET /api/auth/me — server tra raw Map enriched
    // (firstName/lastName/picture/emailVerified), khong envelope
    getMe: build.query<UserMe, void>({
      query: () => ({ url: '/auth/me', method: 'get' }),
      providesTags: ['Me'],
    }),
    // PUT /api/auth/me/profile {firstName,lastName,email} — tu sua ho so
    updateProfile: build.mutation<UserMe, { firstName?: string; lastName?: string; email?: string }>({
      query: (data) => ({ url: '/auth/me/profile', method: 'put', data }),
      invalidatesTags: ['Me'],
    }),
    // PUT /api/auth/me/avatar {fileKey} — luu key anh (upload truoc qua file-service);
    // fileKey null/blank = xoa avatar (doi xung admin PATCH /{id}/avatar)
    updateAvatar: build.mutation<UserMe, { fileKey?: string | null }>({
      query: (data) => ({ url: '/auth/me/avatar', method: 'put', data }),
      invalidatesTags: ['Me'],
    }),
  }),
});

export const { useGetMeQuery, useUpdateProfileMutation, useUpdateAvatarMutation } = userApi;

// Profile view-model: map raw /me Map → typed, defensive defaults (prod
// tra thieu field optional thi render "—", khong bao gio trang).
export interface Profile {
  subject: string;
  username: string;
  email: string;
  fullName: string;
  firstName: string | null;
  lastName: string | null;
  roles: string[];
  admin: boolean;
  enabled: boolean;
  emailVerified: boolean | null;
  picture: string | null;
}

const FALLBACK = '—';

export function toProfile(me: Partial<UserMe & { firstName?: string; lastName?: string; enabled?: boolean; emailVerified?: boolean; picture?: string | null }> | null | undefined): Profile | null {
  if (!me || (!me.username && !me.subject)) return null;
  const firstName = me.firstName?.trim() || null;
  const lastName = me.lastName?.trim() || null;
  const fullName = [firstName, lastName].filter(Boolean).join(' ').trim();
  return {
    subject: me.subject ?? FALLBACK,
    username: me.username ?? FALLBACK,
    email: me.email ?? FALLBACK,
    fullName: fullName || FALLBACK,
    firstName,
    lastName,
    roles: me.roles ?? [],
    admin: me.admin ?? false,
    enabled: me.enabled ?? true,
    emailVerified: me.emailVerified ?? null,
    picture: me.picture ?? null,
  };
}
