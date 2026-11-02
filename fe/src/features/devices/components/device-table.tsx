import type { Device } from 'src/types/api';

import { Link as RouterLink } from 'react-router-dom';

import Link from '@mui/material/Link';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';

import { paths } from 'src/routes/paths';

import { formatRelative, formatTimestamp } from 'src/lib/format';

import { Status, DataTable } from 'src/components/console';

export function DeviceTable({ rows }: { rows: Device[] }) {
  return (
    <DataTable
      rows={rows}
      empty="Chưa có thiết bị. Tạo enrollment token rồi đăng ký simulator."
      columns={[
        {
          title: 'Thiết bị',
          render: (d) => (
            <>
              <Link component={RouterLink} to={paths.devices.detail(d.id)}>
                {d.name}
              </Link>
              <Typography variant="body2" color="text.secondary">
                Android mô phỏng
              </Typography>
            </>
          ),
        },
        { title: 'Kết nối', render: (d) => <Status value={d.connectivity} /> },
        { title: 'Compliance', render: (d) => <Status value={d.compliance} /> },
        {
          title: 'Heartbeat gần nhất',
          render: (d) => (
            <Tooltip title={formatTimestamp(d.lastSeenAt)}>
              <span>{formatRelative(d.lastSeenAt)}</span>
            </Tooltip>
          ),
        },
      ]}
    />
  );
}
