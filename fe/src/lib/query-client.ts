import { QueryClient } from '@tanstack/react-query';

/** Polling is opt-in per query (live device/command state); see features/<domain>/queries.ts. */
export const LIVE_REFETCH_MS = 5_000;

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: false, refetchOnWindowFocus: false },
    mutations: { retry: false },
  },
});
