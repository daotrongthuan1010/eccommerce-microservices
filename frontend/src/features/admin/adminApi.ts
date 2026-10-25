import { createApi } from '@reduxjs/toolkit/query/react';
import { axiosBaseQuery } from '../../app/api';

// rbac-admin-screens — admin data slice, bounded context rieng (FE_HOC).
// userApi.ts giu self-service /me. Mọi path copy tu AdminUserController /
// AdminRoleController (@RequestMapping "/auth/admin/...") qua gateway
// StripPrefix → baseURL /api + /auth/admin/... ADMIN-only.
// LUU Y: auth-service tra raw Map, KHONG envelope ApiResponse (giong /me).
export interface AdminUser {
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
}

export interface AdminUserPage {
  items: AdminUser[];
  total: number;
  page: number;
  size: number;
}

export interface RealmRole {
  name: string;
  description?: string;
  memberCount: number;
}

export interface RoleListResponse {
  items: RealmRole[];
  total: number;
}

export interface RoleMembersResponse {
  role: string;
  items: AdminUser[];
  total: number;
}

export interface CreateUserBody {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  roles?: string[];
  sendVerifyEmail?: boolean;
}

export interface CreatedUser {
  id: string;
  userId: string;
  username: string;
  roles: string[];
}

export interface ListUsersParams {
  search?: string;
  page?: number;
  size?: number;
}

export const adminApi = createApi({
  reducerPath: 'adminApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['AdminUser', 'AdminRole'],
  endpoints: (build) => ({
    // GET /auth/admin/users?search=&page=&size= → {items,total,page,size}
    listUsers: build.query<AdminUserPage, ListUsersParams | void>({
      query: (params) => ({
        url: '/auth/admin/users',
        params: { page: 0, size: 20, ...(params ?? {}) },
      }),
      providesTags: ['AdminUser'],
    }),
    // GET /auth/admin/users/{id} → AdminUser raw (404 user_not_found)
    getUser: build.query<AdminUser, string>({
      query: (id) => ({ url: `/auth/admin/users/${id}` }),
      providesTags: (_r, _e, id) => [{ type: 'AdminUser', id }],
    }),
    // GET /auth/admin/roles → {items,total}
    listRoles: build.query<RoleListResponse, void>({
      query: () => ({ url: '/auth/admin/roles' }),
      providesTags: ['AdminRole'],
    }),
    // GET /auth/admin/roles/{role}/users → {role,items,total}
    listRoleMembers: build.query<RoleMembersResponse, string>({
      query: (role) => ({ url: `/auth/admin/roles/${role}/users` }),
      providesTags: (_r, _e, role) => [{ type: 'AdminRole', id: role }],
    }),
    // POST /auth/admin/users → 201 {id,userId,username,roles}
    createUser: build.mutation<CreatedUser, CreateUserBody>({
      query: (data) => ({ url: '/auth/admin/users', method: 'post', data }),
      invalidatesTags: ['AdminUser', 'AdminRole'],
    }),
    // POST /auth/admin/roles {name, description} → 201 {name, created}
    createRole: build.mutation<{ name: string; created: boolean }, { name: string; description?: string }>({
      query: (data) => ({ url: '/auth/admin/roles', method: 'post', data }),
      invalidatesTags: ['AdminRole'],
    }),
    // DELETE /auth/admin/roles/{role} → {role, deleted}
    deleteRole: build.mutation<{ role: string; deleted: boolean }, string>({
      query: (role) => ({ url: `/auth/admin/roles/${encodeURIComponent(role)}`, method: 'delete' }),
      invalidatesTags: ['AdminRole'],
    }),
    // POST /auth/admin/users/{id}/roles {role}
    assignRole: build.mutation<{ userId: string; role: string }, { id: string; role: string }>({
      query: ({ id, role }) => ({
        url: `/auth/admin/users/${id}/roles`,
        method: 'post',
        data: { role },
      }),
      invalidatesTags: (_r, _e, { id, role }) => [
        'AdminUser',
        'AdminRole',
        { type: 'AdminUser', id },
        { type: 'AdminRole', id: role },
      ],
    }),
    // DELETE /auth/admin/users/{id}/roles/{role}
    removeRole: build.mutation<{ userId: string; role: string }, { id: string; role: string }>({
      query: ({ id, role }) => ({
        url: `/auth/admin/users/${id}/roles/${role}`,
        method: 'delete',
      }),
      invalidatesTags: (_r, _e, { id, role }) => [
        'AdminUser',
        'AdminRole',
        { type: 'AdminUser', id },
        { type: 'AdminRole', id: role },
      ],
    }),
    // PATCH /auth/admin/users/{id}/profile {firstName?,lastName?,email?} → AdminUser raw
    updateAnyProfile: build.mutation<
      AdminUser,
      { id: string; firstName?: string; lastName?: string; email?: string }
    >({
      query: ({ id, ...profile }) => ({
        url: `/auth/admin/users/${id}/profile`,
        method: 'patch',
        data: profile,
      }),
      invalidatesTags: (_r, _e, { id }) => [
        'AdminUser',
        'AdminRole',
        { type: 'AdminUser', id },
      ],
    }),
    // PATCH /auth/admin/users/{id}/avatar {fileKey?} → AdminUser raw (blank = xoa avatar)
    updateAnyAvatar: build.mutation<AdminUser, { id: string; fileKey?: string }>({
      query: ({ id, fileKey }) => ({
        url: `/auth/admin/users/${id}/avatar`,
        method: 'patch',
        data: { fileKey: fileKey ?? '' },
      }),
      invalidatesTags: (_r, _e, { id }) => [
        'AdminUser',
        { type: 'AdminUser', id },
      ],
    }),
    // PATCH /auth/admin/users/{id}/enabled {enabled}
    setEnabled: build.mutation<{ userId: string; enabled: boolean }, { id: string; enabled: boolean }>({
      query: ({ id, enabled }) => ({
        url: `/auth/admin/users/${id}/enabled`,
        method: 'patch',
        data: { enabled },
      }),
      invalidatesTags: (_r, _e, { id }) => [
        'AdminUser',
        'AdminRole',
        { type: 'AdminUser', id },
      ],
    }),
    // POST /auth/admin/users/{id}/reset-password {newPassword, temporary}
    resetPassword: build.mutation<{ userId: string }, { id: string; newPassword: string; temporary?: boolean }>({
      query: ({ id, newPassword, temporary }) => ({
        url: `/auth/admin/users/${id}/reset-password`,
        method: 'post',
        data: { newPassword, temporary: temporary ?? true },
      }),
      invalidatesTags: (_r, _e, { id }) => [{ type: 'AdminUser', id }],
    }),
  }),
});

export const {
  useListUsersQuery,
  useGetUserQuery,
  useListRolesQuery,
  useListRoleMembersQuery,
  useCreateUserMutation,
  useAssignRoleMutation,
  useRemoveRoleMutation,
  useUpdateAnyProfileMutation,
  useUpdateAnyAvatarMutation,
  useSetEnabledMutation,
  useResetPasswordMutation,
  useCreateRoleMutation,
  useDeleteRoleMutation,
} = adminApi;
