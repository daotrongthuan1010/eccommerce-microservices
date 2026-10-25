import { createAsyncThunk, createSlice, type PayloadAction } from '@reduxjs/toolkit';
import axios from 'axios';
import { api } from '../../app/api';
import keycloak from './keycloak';

// FE_API_CONTRACT §1 + FE_AUTH_GATEWAY §2-§3: memory-only tokens, login
// proxy password-grant sang Keycloak (Map tho, KHONG envelope).

export interface TokenMap {
  access_token: string;
  refresh_token: string;
  expires_in?: number;
  refresh_expires_in?: number;
  token_type?: string;
  scope?: string;
}

export interface MeResponse {
  subject: string;
  username: string;
  email?: string;
  roles: string[];
  admin: boolean;
}

export interface RegisterPayload {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
}

export interface AuthError {
  message: string;
  code?: string;
  fields?: Record<string, string>;
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  roles: string[];
  username: string | null;
  status: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
}

const initialState: AuthState = {
  accessToken: null,
  refreshToken: null,
  roles: [],
  username: null,
  status: 'idle',
  error: null,
};

function extractAuthError(err: unknown, fallback: string): AuthError {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as
      | { error?: string; error_description?: string; message?: string; fields?: Record<string, string> }
      | undefined;
    const code = data?.error;
    const fields = data?.fields;
    switch (code) {
      // real 2026-10-22: POST /api/auth/login — FE_API_CONTRACT §1.5
      case 'invalid_grant':
        return { message: 'Sai tên đăng nhập hoặc mật khẩu', code, fields };
      case 'validation_failed':
        return { message: 'Dữ liệu không hợp lệ', code, fields };
      case 'user_exists':
        return { message: 'Tài khoản/email đã tồn tại', code, fields };
      case 'registration_not_configured':
        return {
          message: 'Chức năng đăng ký chưa được cấu hình. Vui lòng liên hệ quản trị viên.',
          code,
          fields,
        };
      case 'mail_not_configured':
        return {
          message: 'Hệ thống mail chưa được cấu hình. Vui lòng thử lại sau.',
          code,
          fields,
        };
      case 'keycloak_unavailable':
        return {
          message: 'Hệ thống xác thực tạm thời không khả dụng. Vui lòng thử lại sau.',
          code,
          fields,
        };
      case 'wrong_password':
        return { message: 'Mật khẩu hiện tại không đúng', code, fields };
      default:
        return {
          message: data?.error_description ?? data?.message ?? code ?? fallback,
          code,
          fields,
        };
    }
  }
  return { message: fallback };
}

function normalizeRoles(me: MeResponse): string[] {
  const roles = [...(me.roles ?? [])];
  if (me.admin && !roles.includes('ADMIN')) roles.push('ADMIN');
  return roles;
}

// real 2026-10-22: POST /api/auth/login (Keycloak Map tho, KHONG envelope)
export const login = createAsyncThunk<
  { tokens: TokenMap; me: MeResponse },
  { username: string; password: string },
  { rejectValue: AuthError }
>('auth/login', async (payload, { rejectWithValue }) => {
  try {
    const { data: tokens } = await api.post<TokenMap>('/auth/login', payload);
    const { data: me } = await api.get<MeResponse>('/auth/me', {
      headers: { Authorization: `Bearer ${tokens.access_token}` },
    });
    return { tokens, me };
  } catch (err) {
    return rejectWithValue(extractAuthError(err, 'Đăng nhập thất bại'));
  }
});

// real 2026-10-22: POST /api/auth/refresh
export const refreshNow = createAsyncThunk<
  TokenMap,
  void,
  { rejectValue: AuthError; state: { auth: AuthState } }
>('auth/refreshNow', async (_, { getState, rejectWithValue }) => {
  try {
    const rt = getState().auth.refreshToken;
    const { data } = await axios.post<TokenMap>(
      `${api.defaults.baseURL}/auth/refresh`,
      { refreshToken: rt },
    );
    return data;
  } catch (err) {
    return rejectWithValue(extractAuthError(err, 'Phiên đăng nhập đã hết hạn'));
  }
});

// real 2026-10-22: GET /api/auth/me
export const fetchMe = createAsyncThunk<MeResponse, void, { rejectValue: AuthError }>(
  'auth/fetchMe',
  async (_, { rejectWithValue }) => {
    try {
      const { data } = await api.get<MeResponse>('/auth/me');
      return data;
    } catch (err) {
      return rejectWithValue(extractAuthError(err, 'Không tải được thông tin người dùng'));
    }
  },
);

// real 2026-10-22: POST /api/auth/register
export const register = createAsyncThunk<
  { userId: string; username: string; email: string },
  RegisterPayload,
  { rejectValue: AuthError }
>('auth/register', async (payload, { rejectWithValue }) => {
  try {
    const { data } = await api.post('/auth/register', payload);
    return data as { userId: string; username: string; email: string };
  } catch (err) {
    return rejectWithValue(extractAuthError(err, 'Đăng ký thất bại'));
  }
});

// real 2026-10-22: POST /api/auth/forgot-password
export const forgotPassword = createAsyncThunk<
  { sent: boolean },
  string,
  { rejectValue: AuthError }
>('auth/forgotPassword', async (username, { rejectWithValue }) => {
  try {
    const { data } = await api.post('/auth/forgot-password', { username });
    return data as { sent: boolean };
  } catch (err) {
    return rejectWithValue(extractAuthError(err, 'Gửi yêu cầu thất bại'));
  }
});

// real 2026-10-22: POST /api/auth/change-password (Bearer)
export const changePassword = createAsyncThunk<
  void,
  { currentPassword: string; newPassword: string },
  { rejectValue: AuthError }
>('auth/changePassword', async (payload, { rejectWithValue }) => {
  try {
    await api.post('/auth/change-password', payload);
  } catch (err) {
    return rejectWithValue(extractAuthError(err, 'Đổi mật khẩu thất bại'));
  }
});

// real 2026-10-22: POST /api/auth/logout (best-effort, luon xoa token local)
// + keycloak.logout() de thoat SSO Keycloak.
export const logout = createAsyncThunk<void, void, { state: { auth: AuthState } }>(
  'auth/logout',
  async (_, { getState }) => {
    try {
      const rt = getState().auth.refreshToken;
      await api.post('/auth/logout', { refreshToken: rt });
    } catch {
      // best-effort: van clear token local
    }
    try {
      if (keycloak.authenticated) {
        await keycloak.logout({ redirectUri: `${window.location.origin}/login` });
      }
    } catch {
      // best-effort: Keycloak logout that bai van clear token local
    }
  },
);

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    loggedOut(state) {
      state.accessToken = null;
      state.refreshToken = null;
      state.roles = [];
      state.username = null;
      state.status = 'idle';
      state.error = null;
    },
    // Keycloak SSO login thanh cong: dua tokens vao memory, me se lay sau
    // qua fetchMe (GET /api/auth/me Bearer, giu normalizeRoles).
    keycloakAuthenticated(state, action: PayloadAction<TokenMap>) {
      state.accessToken = action.payload.access_token;
      state.refreshToken = action.payload.refresh_token;
      state.status = 'loading';
      state.error = null;
    },
    refreshed(state, action: PayloadAction<TokenMap>) {
      state.accessToken = action.payload.access_token;
      state.refreshToken = action.payload.refresh_token;
      state.status = 'succeeded';
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(login.fulfilled, (state, action) => {
        state.accessToken = action.payload.tokens.access_token;
        state.refreshToken = action.payload.tokens.refresh_token;
        state.roles = normalizeRoles(action.payload.me);
        state.username = action.payload.me.username;
        state.status = 'succeeded';
        state.error = null;
      })
      .addCase(login.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload?.message ?? 'Đăng nhập thất bại';
      })
      .addCase(refreshNow.fulfilled, (state, action) => {
        state.accessToken = action.payload.access_token;
        state.refreshToken = action.payload.refresh_token;
        state.status = 'succeeded';
        state.error = null;
      })
      .addCase(refreshNow.rejected, (state, action) => {
        state.accessToken = null;
        state.refreshToken = null;
        state.roles = [];
        state.username = null;
        state.status = 'failed';
        state.error = action.payload?.message ?? 'Phiên đăng nhập đã hết hạn';
      })
      .addCase(fetchMe.fulfilled, (state, action) => {
        state.roles = normalizeRoles(action.payload);
        state.username = action.payload.username;
        state.status = 'succeeded';
        state.error = null;
      })
      .addCase(fetchMe.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload?.message ?? 'Không tải được thông tin người dùng';
      })
      .addCase(logout.fulfilled, (state) => {
        state.accessToken = null;
        state.refreshToken = null;
        state.roles = [];
        state.username = null;
        state.status = 'idle';
        state.error = null;
      });
  },
});

export const { loggedOut, refreshed, keycloakAuthenticated } = authSlice.actions;
export default authSlice.reducer;
