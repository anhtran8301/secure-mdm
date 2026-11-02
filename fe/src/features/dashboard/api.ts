import type { Overview } from 'src/types/api';

import { http } from 'src/lib/axios';

export const dashboardApi = {
  overview: () => http.get<Overview>('/dashboard/overview').then((r) => r.data),
};
