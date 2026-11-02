import type { Page, Command, PageParams, CommandType } from 'src/types/api';

import { http } from 'src/lib/axios';
import { PAGE_SIZE, pageQuery } from 'src/lib/page';

export type CreateCommandBody = { type: CommandType; payload: Record<string, unknown> };

export const commandApi = {
  list: (deviceId: string, { page, size = PAGE_SIZE }: PageParams) =>
    http
      .get<Page<Command>>(`/devices/${deviceId}/commands`, { params: pageQuery({ page, size }) })
      .then((r) => r.data),
  /** The same Idempotency-Key with the same body returns the original command. */
  create: (deviceId: string, body: CreateCommandBody, idempotencyKey: string) =>
    http
      .post<Command>(`/devices/${deviceId}/commands`, body, {
        headers: { 'Idempotency-Key': idempotencyKey },
      })
      .then((r) => r.data),
};
