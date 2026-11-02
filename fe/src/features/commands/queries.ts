import type { PageParams } from 'src/types/api';

import { queryOptions } from '@tanstack/react-query';

import { LIVE_REFETCH_MS } from 'src/lib/query-client';

import { commandApi } from './api';

export const commandQueries = {
  key: {
    all: ['commands'] as const,
    byDevice: (deviceId: string) => [...commandQueries.key.all, deviceId] as const,
    list: (deviceId: string, params: PageParams) =>
      [...commandQueries.key.byDevice(deviceId), params] as const,
  },
  // Status advances as the device claims and reports, so the list polls.
  list: (deviceId: string, params: PageParams) =>
    queryOptions({
      queryKey: commandQueries.key.list(deviceId, params),
      queryFn: () => commandApi.list(deviceId, params),
      refetchInterval: LIVE_REFETCH_MS,
    }),
};
