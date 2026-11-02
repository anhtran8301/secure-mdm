import type { Policy, PolicyConfig } from 'src/types/api';

import { z } from 'zod';

const MAX_BLOCKED_PACKAGES = 200;

export function policyFormSchema(isNewPolicy: boolean) {
  return z.object({
    name: isNewPolicy
      ? z.string().trim().min(1, 'Nhập tên policy.').max(120, 'Tối đa 120 ký tự.')
      : z.string(),
    requireScreenLock: z.boolean(),
    minAndroidApiLevel: z
      .number('Nhập số từ 1 đến 100.')
      .int('Nhập số nguyên.')
      .min(1, 'Nhập số từ 1 đến 100.')
      .max(100, 'Nhập số từ 1 đến 100.'),
    blockedPackages: z
      .string()
      .refine(
        (value) => toPackages(value).length <= MAX_BLOCKED_PACKAGES,
        `Tối đa ${MAX_BLOCKED_PACKAGES} package.`
      )
      .refine(
        (value) => toPackages(value).every((name) => name.length <= 180),
        'Mỗi package tối đa 180 ký tự.'
      ),
  });
}

export type PolicyFormValues = z.infer<ReturnType<typeof policyFormSchema>>;

export function toPackages(value: string) {
  return value
    .split('\n')
    .map((name) => name.trim())
    .filter(Boolean);
}

export function policyFormDefaults(policy: Policy | null): PolicyFormValues {
  return {
    name: policy?.name ?? '',
    requireScreenLock: policy?.config.requireScreenLock ?? true,
    minAndroidApiLevel: policy?.config.minAndroidApiLevel ?? 34,
    blockedPackages: policy?.config.blockedPackages.join('\n') ?? '',
  };
}

export function toPolicyConfig(values: PolicyFormValues): PolicyConfig {
  return {
    requireScreenLock: values.requireScreenLock,
    minAndroidApiLevel: values.minAndroidApiLevel,
    blockedPackages: toPackages(values.blockedPackages),
  };
}
