import { queryOptions } from '@tanstack/react-query';

import { LIVE_REFETCH_MS } from 'src/lib/query-client';

import { dashboardApi } from './api';

export const dashboardQueries = {
  key: { all: ['dashboard'] as const },
  overview: () =>
    queryOptions({
      queryKey: [...dashboardQueries.key.all, 'overview'] as const,
      queryFn: dashboardApi.overview,
      refetchInterval: LIVE_REFETCH_MS,
    }),
};
