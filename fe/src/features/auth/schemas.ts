import { z } from 'zod';

export const loginSchema = z.object({
  organizationSlug: z.string().trim().min(1, 'Nhập slug tổ chức.').max(80),
  email: z.email('Email không hợp lệ.').max(180),
  password: z.string().min(1, 'Nhập mật khẩu.').max(72),
});

export type LoginValues = z.infer<typeof loginSchema>;
