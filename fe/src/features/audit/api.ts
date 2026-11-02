import type { Page, AuditEntry, PageParams } from 'src/types/api';

import { http } from 'src/lib/axios';
import { PAGE_SIZE, pageQuery } from 'src/lib/page';

export type AuditListParams = PageParams & { action?: string };

export const auditApi = {
  list: ({ page, size = PAGE_SIZE, action }: AuditListParams) =>
    http
      .get<Page<AuditEntry>>('/audit-logs', { params: pageQuery({ page, size, action }) })
      .then((r) => r.data),
};
