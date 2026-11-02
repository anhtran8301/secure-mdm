import type { PageParams } from 'src/types/api';

import { queryOptions } from '@tanstack/react-query';

import { policyApi } from './api';

export const policyQueries = {
  key: {
    all: ['policies'] as const,
    list: (params: PageParams) => [...policyQueries.key.all, 'list', params] as const,
    versions: (policyId: string, params: PageParams) =>
      [...policyQueries.key.all, 'versions', policyId, params] as const,
    assignment: (deviceId: string) => [...policyQueries.key.all, 'assignment', deviceId] as const,
  },
  list: (params: PageParams) =>
    queryOptions({
      queryKey: policyQueries.key.list(params),
      queryFn: () => policyApi.list(params),
    }),
  versions: (policyId: string, params: PageParams) =>
    queryOptions({
      queryKey: policyQueries.key.versions(policyId, params),
      queryFn: () => policyApi.versions(policyId, params),
      enabled: !!policyId,
    }),
  assignment: (deviceId: string) =>
    queryOptions({
      queryKey: policyQueries.key.assignment(deviceId),
      queryFn: () => policyApi.assignment(deviceId),
    }),
};
