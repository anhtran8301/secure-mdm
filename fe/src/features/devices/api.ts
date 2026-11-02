import type { Page, Device, PageParams } from 'src/types/api';

import { http } from 'src/lib/axios';
import { PAGE_SIZE, pageQuery } from 'src/lib/page';

export type DeviceListParams = PageParams & { search?: string };

export const deviceApi = {
  list: ({ page, size = PAGE_SIZE, search }: DeviceListParams) =>
    http
      .get<Page<Device>>('/devices', { params: pageQuery({ page, size, search }) })
      .then((r) => r.data),
  detail: (id: string) => http.get<Device>(`/devices/${id}`).then((r) => r.data),
  revoke: (id: string) => http.post<void>(`/devices/${id}/revoke`).then(() => undefined),
};
