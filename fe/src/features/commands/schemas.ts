import type { CommandType } from 'src/types/api';

import { z } from 'zod';

export const COMMAND_TYPES: CommandType[] = [
  'REPORT_STATUS',
  'SYNC_DEVICE',
  'SHOW_MESSAGE',
  'LOCK_DEVICE_SIMULATED',
];

export const commandSchema = z
  .object({
    type: z.enum(COMMAND_TYPES),
    message: z.string().max(500, 'Tối đa 500 ký tự.'),
  })
  .refine((value) => value.type !== 'SHOW_MESSAGE' || value.message.trim().length > 0, {
    path: ['message'],
    message: 'Nhập nội dung thông báo.',
  });

export type CommandFormValues = z.infer<typeof commandSchema>;

export function toCommandBody(values: CommandFormValues) {
  return {
    type: values.type,
    payload: values.type === 'SHOW_MESSAGE' ? { message: values.message } : {},
  };
}
