import type { Overview } from 'src/types/api';

import Card from '@mui/material/Card';
import { useTheme } from '@mui/material/styles';
import Typography from '@mui/material/Typography';
import CardHeader from '@mui/material/CardHeader';

import { fNumber } from 'src/utils/format-number';

import { Chart, useChart } from 'src/components/chart';
import { statusText, statusMainColor } from 'src/components/console';

// The command lifecycle, so a status with no commands still shows as zero.
const LIFECYCLE = [
  'PENDING',
  'IN_PROGRESS',
  'SUCCEEDED',
  'FAILED',
  'UNKNOWN',
  'EXPIRED',
  'CANCELLED',
];

export function CommandStatusChart({ commands }: { commands: Overview['commands'] }) {
  const theme = useTheme();
  const counts = LIFECYCLE.map(
    (status) => commands.find((item) => item.status === status)?.total ?? 0
  );
  // One step of headroom keeps the value label of the tallest bar inside the plot.
  const ceiling = Math.max(1, ...counts) + 1;
  const categories = LIFECYCLE.map(statusText);

  const options = useChart({
    colors: LIFECYCLE.map((status) => statusMainColor(theme, status)),
    xaxis: { categories },
    legend: { show: false },
    // Value above each bar, so a zero reads as zero rather than a sliver.
    dataLabels: {
      enabled: true,
      offsetY: -22,
      style: { colors: [theme.palette.text.primary], fontSize: '12px' },
    },
    plotOptions: {
      bar: {
        distributed: true,
        columnWidth: '40%',
        borderRadius: 4,
        dataLabels: { position: 'top' },
      },
    },
    // Counts are whole numbers; hide fractional ticks.
    yaxis: {
      min: 0,
      max: ceiling,
      forceNiceScale: true,
      tickAmount: ceiling <= 5 ? ceiling : undefined,
      labels: { formatter: (value: number) => String(Math.round(value)) },
    },
    tooltip: {
      y: { formatter: (value: number) => fNumber(value), title: { formatter: () => '' } },
    },
  });

  return (
    <Card>
      <CardHeader title="Hàng đợi command" subheader="Số command theo trạng thái" />
      {counts.some(Boolean) ? (
        <Chart
          type="bar"
          series={[{ name: 'Command', data: counts }]}
          options={options}
          role="img"
          aria-label={categories.map((label, i) => `${label}: ${counts[i]}`).join(', ')}
          slotProps={{ loading: { p: 2.5 } }}
          sx={{ pl: 1, py: 2.5, pr: 2.5, height: 320 }}
        />
      ) : (
        <Typography color="text.secondary" sx={{ p: 3 }}>
          Chưa có command. Chọn một thiết bị để gửi lệnh mô phỏng.
        </Typography>
      )}
    </Card>
  );
}
