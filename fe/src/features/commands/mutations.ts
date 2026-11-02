import { useMutation, useQueryClient } from '@tanstack/react-query';

import { dashboardQueries } from 'src/features/dashboard/queries';

import { toast } from 'src/components/snackbar';

import { commandApi } from './api';
import { commandQueries } from './queries';

import type { CreateCommandBody } from './api';

type Variables = { body: CreateCommandBody; idempotencyKey: string };

export function useCreateCommand(deviceId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ body, idempotencyKey }: Variables) =>
      commandApi.create(deviceId, body, idempotencyKey),
    onSuccess: async () => {
      toast.success('Command đã được đưa vào hàng đợi.');
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: commandQueries.key.byDevice(deviceId) }),
        queryClient.invalidateQueries({ queryKey: dashboardQueries.key.all }),
      ]);
    },
  });
}
