import { createApi } from '@reduxjs/toolkit/query/react';
import { axiosBaseQuery } from '../../app/api';

// staff-hierarchy — cap tren (ADMIN/MODERATOR) quan ly cap duoi truc tiep.
// Khac adminApi (ADMIN toan he thong): moi nguoi chi thay nguoi minh tao
// (BE loc theo attribute managerId = subject cua toi). Path copy tu
// StaffUserController @RequestMapping "/auth/staff/users" qua gateway.
// auth-service tra raw Map, KHONG envelope ApiResponse.
export interface StaffUser {
  id: string;
  username: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  enabled: boolean;
  emailVerified?: boolean;
  roles: string[];
  requiredActions?: string[];
  picture?: string | null;
  accountType?: string | null;
  managerId?: string | null;
}

export interface StaffListResponse {
  items: StaffUser[];
  total: number;
}

export interface CreateStaffBody {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  roles?: string[];
  /** true = gui email xac thuc; bo qua = cap tren tin cuoc, login duoc ngay. */
  sendVerifyEmail?: boolean;
}

export interface CreatedStaff {
  id: string;
  userId: string;
  username: string;
  roles: string[];
  managerId: string;
}

export const staffApi = createApi({
  reducerPath: 'staffApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['StaffUser'],
  endpoints: (build) => ({
    // GET /auth/staff/users → {items,total} — cap duoi truc tiep cua toi.
    listMyStaff: build.query<StaffListResponse, void>({
      query: () => ({ url: '/auth/staff/users' }),
      providesTags: ['StaffUser'],
    }),
    // GET /auth/staff/users/{id} → StaffUser (403 not_your_subordinate khi khong phai cua minh).
    getStaff: build.query<StaffUser, string>({
      query: (id) => ({ url: `/auth/staff/users/${id}` }),
      providesTags: (_r, _e, id) => [{ type: 'StaffUser', id }],
    }),
    // POST /auth/staff/users → 201 {id,userId,username,roles,managerId}
    // 403 role_not_grantable khi gan role ngang/cao hon cap cua minh.
    createStaff: build.mutation<CreatedStaff, CreateStaffBody>({
      query: (data) => ({ url: '/auth/staff/users', method: 'post', data }),
      invalidatesTags: ['StaffUser'],
    }),
    // POST /auth/staff/users/{id}/enabled {enabled} — khoa/mo cap duoi.
    setStaffEnabled: build.mutation<{ userId: string; enabled: boolean }, { id: string; enabled: boolean }>({
      query: ({ id, enabled }) => ({
        url: `/auth/staff/users/${id}/enabled`,
        method: 'post',
        data: { enabled },
      }),
      invalidatesTags: (_r, _e, { id }) => [{ type: 'StaffUser', id }, 'StaffUser'],
    }),
    // POST /auth/staff/users/{id}/reset-password {newPassword,temporary}
    resetStaffPassword: build.mutation<
      { userId: string; reset: boolean },
      { id: string; newPassword: string; temporary: boolean }
    >({
      query: ({ id, ...data }) => ({
        url: `/auth/staff/users/${id}/reset-password`,
        method: 'post',
        data,
      }),
      invalidatesTags: ['StaffUser'],
    }),
  }),
});

export const {
  useListMyStaffQuery,
  useGetStaffQuery,
  useCreateStaffMutation,
  useSetStaffEnabledMutation,
  useResetStaffPasswordMutation,
} = staffApi;
