import { Navigate } from 'react-router-dom';
import { useAppSelector } from '../app/hooks';

// FE_AUTH_GATEWAY §5 — guards. Roles lay tu GET /api/auth/me
// (roles[] + admin===true → ADMIN), khong dung IAM-DB permission paths.
export function RequireAuth({ children }: { children: JSX.Element }) {
  const token = useAppSelector((s) => s.auth.accessToken);
  return token ? children : <Navigate to="/login" replace />;
}

export function RequireRole({ roles, children }: { roles: string[]; children: JSX.Element }) {
  const { accessToken, roles: mine } = useAppSelector((s) => s.auth);
  if (!accessToken) return <Navigate to="/login" replace />;
  return roles.some((r) => mine.includes(r)) ? children : <Navigate to="/403" replace />;
}
