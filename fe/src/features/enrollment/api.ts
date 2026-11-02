import type { EnrollmentToken } from 'src/types/api';

import { http } from 'src/lib/axios';

export const enrollmentApi = {
  issue: () => http.post<EnrollmentToken>('/enrollment-tokens').then((r) => r.data),
  revoke: (id: string) => http.post<void>(`/enrollment-tokens/${id}/revoke`).then(() => undefined),
};
