import type { ReactNode } from 'react';

import { Navigate } from 'react-router-dom';

import LinearProgress from '@mui/material/LinearProgress';

import { useAuthStore } from 'src/stores/auth-store';

import { paths } from '../paths';

/** Waits for the initial silent refresh, then requires a signed-in user. */
export function AuthGuard({ children }: { children: ReactNode }) {
  const ready = useAuthStore((state) => state.ready);
  const user = useAuthStore((state) => state.user);
  if (!ready) return <LinearProgress aria-label="Đang khôi phục phiên" />;
  if (!user) return <Navigate to={paths.signIn} replace />;
  return <>{children}</>;
}
