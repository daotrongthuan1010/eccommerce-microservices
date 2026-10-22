import { createApi } from '@reduxjs/toolkit/query/react';
import { axiosBaseQuery, type ApiResponse } from '../../app/api';

// FE_API_CONTRACT §2: GET /api/user/me (Bearer) — cung shape voi GET /api/auth/me.
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
    // real 2026-10-22: GET /api/user/me
    getMe: build.query<ApiResponse<UserMe>, void>({
      query: () => ({ url: '/user/me', method: 'get' }),
    }),
  }),
});

export const { useGetMeQuery } = userApi;
