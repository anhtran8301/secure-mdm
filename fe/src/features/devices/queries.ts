import { queryOptions } from '@tanstack/react-query';

import { LIVE_REFETCH_MS } from 'src/lib/query-client';

import { deviceApi } from './api';

import type { DeviceListParams } from './api';

export const deviceQueries = {
  key: {
    all: ['devices'] as const,
    lists: () => [...deviceQueries.key.all, 'list'] as const,
    list: (params: DeviceListParams) => [...deviceQueries.key.lists(), params] as const,
    detail: (id: string) => [...deviceQueries.key.all, 'detail', id] as const,
  },
  // Connectivity and compliance change without user action, so device data polls.
  list: (params: DeviceListParams) =>
    queryOptions({
      queryKey: deviceQueries.key.list(params),
      queryFn: () => deviceApi.list(params),
      refetchInterval: LIVE_REFETCH_MS,
    }),
  detail: (id: string) =>
    queryOptions({
      queryKey: deviceQueries.key.detail(id),
      queryFn: () => deviceApi.detail(id),
      refetchInterval: LIVE_REFETCH_MS,
    }),
};
