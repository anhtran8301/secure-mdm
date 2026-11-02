import type { ReactNode } from 'react';

import { Navigate } from 'react-router-dom';

import { useAuthStore } from 'src/stores/auth-store';

import { paths } from '../paths';

export function GuestGuard({ children }: { children: ReactNode }) {
  const user = useAuthStore((state) => state.user);
  if (user) return <Navigate to={paths.root} replace />;
  return <>{children}</>;
}
