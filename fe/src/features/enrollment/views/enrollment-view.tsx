import Box from '@mui/material/Box';
import Alert from '@mui/material/Alert';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';

import { formatTimestamp } from 'src/lib/format';
import { usePermissions } from 'src/features/auth/hooks/use-permissions';

import { Heading, ErrorMessage } from 'src/components/console';

import { useIssueEnrollmentToken, useRevokeEnrollmentToken } from '../mutations';

export function EnrollmentView() {
  const { admin } = usePermissions();
  const issue = useIssueEnrollmentToken();
  const revoke = useRevokeEnrollmentToken();

  return (
    <>
      <Heading title="Enrollment" description="Cấp quyền đăng ký một thiết bị Android mô phỏng." />
      <Stack gap={3} sx={{ maxWidth: 700 }}>
        <Typography>
          Token dùng một lần, hết hạn sau 15 phút. Sao chép ngay sau khi tạo; máy chủ chỉ lưu hash.
        </Typography>
        {!admin && (
          <Alert severity="info">Chỉ ORG_ADMIN có quyền phát hành enrollment token.</Alert>
        )}
        <Box>
          <Button
            variant="contained"
            color="inherit"
            disabled={!admin || issue.isPending}
            onClick={() => {
              revoke.reset();
              issue.mutate();
            }}
          >
            Tạo enrollment token
          </Button>
        </Box>
        <ErrorMessage error={issue.error || revoke.error} />
        {issue.data && !revoke.isSuccess && (
          <Stack gap={2}>
            <TextField
              label="Enrollment token"
              value={issue.data.token}
              multiline
              slotProps={{ input: { readOnly: true } }}
            />
            <Typography>Hết hạn: {formatTimestamp(issue.data.expiresAt)}</Typography>
            <Typography>
              Trong thư mục simulator, đặt token vào biến môi trường ENROLLMENT_TOKEN rồi chạy:
            </Typography>
            <Box
              component="pre"
              sx={{
                whiteSpace: 'pre-wrap',
                overflowWrap: 'anywhere',
                p: 2,
                bgcolor: 'background.neutral',
              }}
            >
              corepack yarn dev enroll --state ../.local/device --uid android-demo --name
              &quot;Android Demo&quot;
            </Box>
            <Button onClick={() => revoke.mutate(issue.data.id)} disabled={revoke.isPending}>
              Thu hồi token này
            </Button>
          </Stack>
        )}
      </Stack>
    </>
  );
}
