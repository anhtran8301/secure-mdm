import type { Overview } from 'src/types/api';

import Card from '@mui/material/Card';
import Divider from '@mui/material/Divider';
import { useTheme } from '@mui/material/styles';
import Typography from '@mui/material/Typography';
import CardHeader from '@mui/material/CardHeader';

import { fNumber } from 'src/utils/format-number';

import { statusText, statusMainColor } from 'src/components/console';
import { Chart, useChart, ChartLegends } from 'src/components/chart';

const STATES = ['ONLINE', 'OFFLINE', 'REVOKED'];

export function DeviceStatusChart({ devices }: { devices: Overview['devices'] }) {
  const theme = useTheme();
  // The API counts total, online and revoked; the rest are active but silent.
  const offline = Math.max(0, devices.total - devices.online - devices.revoked);
  const series = [devices.online, offline, devices.revoked];
  const labels = STATES.map(statusText);
  const colors = STATES.map((state) => statusMainColor(theme, state));

  const options = useChart({
    chart: { sparkline: { enabled: true } },
    colors,
    labels,
    stroke: { width: 0 },
    plotOptions: { pie: { donut: { size: '74%', labels: { total: { label: 'Thiết bị' } } } } },
    tooltip: { y: { formatter: (value: number) => fNumber(value) } },
  });

  return (
    <Card>
      <CardHeader
        title="Kết nối thiết bị"
        subheader="Online: có heartbeat trong 90 giây gần nhất"
      />
      {devices.total ? (
        <>
          <Chart
            type="donut"
            series={series}
            options={options}
            role="img"
            aria-label={labels.map((label, i) => `${label}: ${series[i]}`).join(', ')}
            sx={{ my: 4, mx: 'auto', width: 240, height: 240 }}
          />
          <Divider sx={{ borderStyle: 'dashed' }} />
          <ChartLegends
            labels={labels}
            colors={colors}
            values={series.map(String)}
            sx={{ p: 3, justifyContent: 'center' }}
          />
        </>
      ) : (
        <Typography color="text.secondary" sx={{ p: 3 }}>
          Chưa có thiết bị. Tạo enrollment token rồi đăng ký simulator.
        </Typography>
      )}
    </Card>
  );
}
