import { queryOptions } from '@tanstack/react-query';

import { auditApi } from './api';

import type { AuditListParams } from './api';

export const auditQueries = {
  key: {
    all: ['audit'] as const,
    list: (params: AuditListParams) => [...auditQueries.key.all, 'list', params] as const,
  },
  list: (params: AuditListParams) =>
    queryOptions({
      queryKey: auditQueries.key.list(params),
      queryFn: () => auditApi.list(params),
    }),
};
