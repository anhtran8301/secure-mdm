import { useQuery } from '@tanstack/react-query';
import { Link as RouterLink } from 'react-router-dom';

import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Alert from '@mui/material/Alert';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';

import { paths } from 'src/routes/paths';

import { deviceQueries } from 'src/features/devices/queries';
import { DeviceTable } from 'src/features/devices/components/device-table';

import { Heading, QueryState } from 'src/components/console';

import { dashboardQueries } from '../queries';
import { DeviceStatusChart } from '../components/device-status-chart';
import { CommandStatusChart } from '../components/command-status-chart';

export function OverviewView() {
  const overview = useQuery(dashboardQueries.overview());
  const latest = useQuery(deviceQueries.list({ page: 0, size: 5 }));

  return (
    <>
      <Heading
        title="Tổng quan thiết bị"
        description="Theo dõi kết nối và kết quả thực thi trong tổ chức."
        action={
          <Button component={RouterLink} to={paths.enrollment} variant="contained" color="inherit">
            Enrollment
          </Button>
        }
      />
      <QueryState query={overview}>
        {(data) => (
          <>
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' },
                gap: 3,
                mb: 3,
              }}
            >
              {[
                ['Tổng thiết bị', data.devices.total],
                ['Đang kết nối', data.devices.online],
                ['Đã thu hồi', data.devices.revoked],
              ].map(([label, value]) => (
                <Card key={label} sx={{ p: 3 }}>
                  <Typography color="text.secondary">{label}</Typography>
                  <Typography
                    variant="h3"
                    component="p"
                    sx={{ fontSize: 32, fontVariantNumeric: 'tabular-nums' }}
                  >
                    {value}
                  </Typography>
                </Card>
              ))}
            </Box>
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', md: '5fr 7fr' },
                gap: 3,
                mb: 3,
              }}
            >
              <DeviceStatusChart devices={data.devices} />
              <CommandStatusChart commands={data.commands} />
            </Box>
            {data.commands.some((item) => item.status === 'UNKNOWN' && item.total > 0) && (
              <Alert severity="warning" sx={{ mb: 3 }}>
                Unknown: thiết bị đã nhận lệnh nhưng chưa xác định được kết quả. Kiểm tra trạng thái
                trước khi gửi lệnh mới.
              </Alert>
            )}
          </>
        )}
      </QueryState>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography component="h2" variant="h6">
          Thiết bị mới nhất
        </Typography>
        <Button component={RouterLink} to={paths.devices.root}>
          Xem tất cả
        </Button>
      </Stack>
      <QueryState query={latest}>{(data) => <DeviceTable rows={data.items} />}</QueryState>
    </>
  );
}
