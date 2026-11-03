import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams, Link as RouterLink } from 'react-router-dom';

import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import Typography from '@mui/material/Typography';
import DialogTitle from '@mui/material/DialogTitle';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';

import { paths } from 'src/routes/paths';

import { formatTimestamp } from 'src/lib/format';
import { usePermissions } from 'src/features/auth/hooks/use-permissions';
import { CommandPanel } from 'src/features/commands/components/command-panel';
import { AssignmentEditor } from 'src/features/policies/components/assignment-editor';

import { Status, Heading, QueryState, ErrorMessage } from 'src/components/console';

import { deviceQueries } from '../queries';
import { useRevokeDevice } from '../mutations';
import { certificateStatus } from '../certificate';

export function DeviceDetailView() {
  const { id = '' } = useParams();
  const query = useQuery(deviceQueries.detail(id));
  const { admin, edit } = usePermissions();
  const [confirm, setConfirm] = useState(false);
  const revoke = useRevokeDevice(id);

  return (
    <>
      <Button component={RouterLink} to={paths.devices.root} sx={{ mb: 2 }}>
        ← Thiết bị
      </Button>
      <QueryState query={query}>
        {(d) => (
          <>
            <Heading
              title={d.name}
              description={`Android mô phỏng · ${d.deviceUid}`}
              action={
                <Button
                  color="error"
                  disabled={!admin || d.status === 'REVOKED'}
                  onClick={() => setConfirm(true)}
                >
                  Thu hồi thiết bị
                </Button>
              }
            />
            <Stack direction="row" gap={1} flexWrap="wrap" mb={2}>
              <Status value={d.connectivity} />
              <Status value={d.compliance} />
              <Typography>Heartbeat: {formatTimestamp(d.lastSeenAt)}</Typography>
            </Stack>
            <Typography variant="body2" sx={{ overflowWrap: 'anywhere', mb: 3 }}>
              Device ID: {d.id}
            </Typography>
            {d.certificate && (
              <Box mb={3}>
                <Stack direction="row" gap={1} alignItems="center" mb={1}>
                  <Typography variant="h6" component="h2">
                    Chứng chỉ thiết bị
                  </Typography>
                  <Status value={certificateStatus(d.certificate)} />
                </Stack>
                <Box
                  component="dl"
                  sx={{
                    display: 'grid',
                    gridTemplateColumns: { xs: '1fr', sm: 'max-content 1fr' },
                    columnGap: 2,
                    rowGap: 0.5,
                    m: 0,
                    '& dt': { color: 'text.secondary' },
                    '& dd': { m: 0, fontFamily: 'monospace', overflowWrap: 'anywhere' },
                  }}
                >
                  <dt>Serial</dt>
                  <dd>{d.certificate.serial}</dd>
                  <dt>SHA-256</dt>
                  <dd>{d.certificate.fingerprint}</dd>
                  <dt>Hết hạn</dt>
                  <dd>{formatTimestamp(d.certificate.notAfter)}</dd>
                </Box>
              </Box>
            )}
            <ErrorMessage error={revoke.error} />
            <Typography variant="h6" component="h2" mb={1}>
              Inventory
            </Typography>
            <Box
              component="pre"
              sx={{
                bgcolor: 'background.neutral',
                p: 2,
                whiteSpace: 'pre-wrap',
                overflowWrap: 'anywhere',
                mb: 3,
              }}
            >
              {JSON.stringify(d.inventory, null, 2)}
            </Box>
            <AssignmentEditor deviceId={id} enabled={edit && d.status === 'ACTIVE'} />
            <Box mt={4}>
              <CommandPanel deviceId={id} enabled={edit && d.status === 'ACTIVE'} />
            </Box>
            <Dialog open={confirm} onClose={() => setConfirm(false)} aria-labelledby="revoke-title">
              <DialogTitle id="revoke-title">Thu hồi quyền của {d.name}?</DialogTitle>
              <DialogContent>
                Chứng chỉ thiết bị bị thu hồi ngay, mọi request ký bằng khóa của nó sẽ bị từ chối.
                Lệnh chưa giao sẽ bị hủy; lệnh đã giao có thể đã thực thi và không thể đảo ngược.
              </DialogContent>
              <DialogActions>
                <Button onClick={() => setConfirm(false)}>Giữ thiết bị</Button>
                <Button
                  color="error"
                  disabled={revoke.isPending}
                  onClick={() => revoke.mutate(undefined, { onSuccess: () => setConfirm(false) })}
                >
                  Xác nhận thu hồi
                </Button>
              </DialogActions>
            </Dialog>
          </>
        )}
      </QueryState>
    </>
  );
}
