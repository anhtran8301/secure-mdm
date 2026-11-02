import type { Policy } from 'src/types/api';

import { useMutation, useQueryClient } from '@tanstack/react-query';

import { deviceQueries } from 'src/features/devices/queries';

import { toast } from 'src/components/snackbar';

import { policyApi } from './api';
import { policyQueries } from './queries';
import { toPolicyConfig } from './schemas';

import type { PolicyFormValues } from './schemas';

/** Creates a policy, or the next immutable version of an existing one. */
export function useSavePolicy(policy: Policy | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (values: PolicyFormValues) =>
      policy
        ? policyApi.createVersion(policy.id, toPolicyConfig(values))
        : policyApi.create({ name: values.name.trim(), config: toPolicyConfig(values) }),
    onSuccess: async (version) => {
      toast.success(`Đã lưu version ${version.version}.`);
      await queryClient.invalidateQueries({ queryKey: policyQueries.key.all });
    },
  });
}

/** Assigns a version (or unassigns with null); the device then reports compliance. */
export function useAssignPolicy(deviceId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (versionId: string | null) =>
      versionId ? policyApi.assign(deviceId, versionId) : policyApi.unassign(deviceId),
    onSuccess: async (assignment) => {
      queryClient.setQueryData(policyQueries.key.assignment(deviceId), assignment);
      toast.success('Đã cập nhật assignment. Đang chờ báo cáo từ thiết bị.');
      await queryClient.invalidateQueries({ queryKey: deviceQueries.key.detail(deviceId) });
    },
  });
}
