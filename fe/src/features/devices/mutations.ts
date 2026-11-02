import { useMutation, useQueryClient } from '@tanstack/react-query';

import { commandQueries } from 'src/features/commands/queries';
import { dashboardQueries } from 'src/features/dashboard/queries';

import { toast } from 'src/components/snackbar';

import { deviceApi } from './api';
import { deviceQueries } from './queries';

export function useRevokeDevice(deviceId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => deviceApi.revoke(deviceId),
    onSuccess: async () => {
      toast.success('Đã thu hồi credential của thiết bị.');
      await Promise.all(
        [deviceQueries.key.all, commandQueries.key.all, dashboardQueries.key.all].map((queryKey) =>
          queryClient.invalidateQueries({ queryKey })
        )
      );
    },
  });
}
