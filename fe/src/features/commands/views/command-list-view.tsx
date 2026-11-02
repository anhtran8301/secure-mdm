import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';

import MenuItem from '@mui/material/MenuItem';
import TextField from '@mui/material/TextField';

import { PAGE_SIZE } from 'src/lib/page';
import { deviceQueries } from 'src/features/devices/queries';
import { usePermissions } from 'src/features/auth/hooks/use-permissions';

import { Pager, Heading, QueryState, statusText } from 'src/components/console';

import { CommandPanel } from '../components/command-panel';

export function CommandListView() {
  const [devicePage, setDevicePage] = useState(0);
  const devices = useQuery(deviceQueries.list({ page: devicePage }));
  const [deviceId, setDeviceId] = useState('');
  const { edit } = usePermissions();
  const selected = devices.data?.items.find((d) => d.id === deviceId);

  return (
    <>
      <Heading title="Commands" description="Theo dõi lệnh theo thiết bị và từng lần giao." />
      <QueryState query={devices}>
        {(data) => (
          <TextField
            fullWidth
            select
            label="Thiết bị"
            value={deviceId}
            sx={{ mb: 3 }}
            onChange={(event) => setDeviceId(event.target.value)}
          >
            <MenuItem value="">Chọn thiết bị</MenuItem>
            {data.items.map((d) => (
              <MenuItem key={d.id} value={d.id}>
                {d.name} · {statusText(d.status)}
              </MenuItem>
            ))}
          </TextField>
        )}
      </QueryState>
      {devices.data && devices.data.total > PAGE_SIZE && (
        <Pager
          page={devicePage}
          total={devices.data.total}
          onChange={(p) => {
            setDevicePage(p);
            setDeviceId('');
          }}
        />
      )}
      {deviceId && (
        <CommandPanel
          key={deviceId}
          deviceId={deviceId}
          enabled={edit && selected?.status === 'ACTIVE'}
        />
      )}
    </>
  );
}
