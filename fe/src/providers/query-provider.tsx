import type { ReactNode } from 'react';

import { useEffect } from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';

import { refreshSession } from 'src/lib/session';
import { queryClient } from 'src/lib/query-client';
import { useAuthStore } from 'src/stores/auth-store';

/** Restores the session from the refresh cookie and drops cached data when the user changes. */
export function QueryProvider({ children }: { children: ReactNode }) {
  useEffect(() => {
    const unsubscribe = useAuthStore.subscribe((state, previous) => {
      if (state.user?.id !== previous.user?.id) queryClient.clear();
    });
    refreshSession().catch(() => {});
    return unsubscribe;
  }, []);

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      {import.meta.env.DEV && <ReactQueryDevtools buttonPosition="bottom-left" />}
    </QueryClientProvider>
  );
}
