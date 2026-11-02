import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';

import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import MenuItem from '@mui/material/MenuItem';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';

import { PAGE_SIZE } from 'src/lib/page';

import { Pager, QueryState, ErrorMessage } from 'src/components/console';

import { policyQueries } from '../queries';
import { useAssignPolicy } from '../mutations';

type Props = { deviceId: string; enabled: boolean };

export function AssignmentEditor({ deviceId, enabled }: Props) {
  const assignment = useQuery(policyQueries.assignment(deviceId));
  const [policyPage, setPolicyPage] = useState(0);
  const [versionPage, setVersionPage] = useState(0);
  const [policyId, setPolicyId] = useState('');
  const [versionId, setVersionId] = useState('');
  const policies = useQuery({ ...policyQueries.list({ page: policyPage }), enabled });
  const versions = useQuery(policyQueries.versions(policyId, { page: versionPage }));
  const write = useAssignPolicy(deviceId);

  return (
    <Stack gap={2}>
      <Typography variant="h6" component="h2">
        Policy assignment
      </Typography>
      <QueryState query={assignment}>
        {(a) => (
          <Typography>
            Revision {a.revision} ·{' '}
            {a.policy ? `Version ${a.policy.version} (${a.policy.id})` : 'Chưa gán policy'}
          </Typography>
        )}
      </QueryState>
      {enabled && (
        <>
          <QueryState query={policies}>
            {(data) => (
              <TextField
                select
                label="Policy"
                value={policyId}
                onChange={(event) => {
                  setPolicyId(event.target.value);
                  setVersionId('');
                  setVersionPage(0);
                }}
              >
                <MenuItem value="">Chọn policy</MenuItem>
                {data.items.map((p) => (
                  <MenuItem key={p.id} value={p.id}>
                    {p.name}
                  </MenuItem>
                ))}
              </TextField>
            )}
          </QueryState>
          {policies.data && policies.data.total > PAGE_SIZE && (
            <Pager
              page={policyPage}
              total={policies.data.total}
              onChange={(p) => {
                setPolicyPage(p);
                setPolicyId('');
                setVersionId('');
              }}
            />
          )}
          {policyId && (
            <QueryState query={versions}>
              {(data) => (
                <TextField
                  select
                  label="Version cần gán"
                  value={versionId}
                  onChange={(event) => setVersionId(event.target.value)}
                >
                  <MenuItem value="">Chọn version</MenuItem>
                  {data.items.map((v) => (
                    <MenuItem key={v.id} value={v.id}>
                      Version {v.version}
                    </MenuItem>
                  ))}
                </TextField>
              )}
            </QueryState>
          )}
          {policyId && versions.data && versions.data.total > PAGE_SIZE && (
            <Pager
              page={versionPage}
              total={versions.data.total}
              onChange={(p) => {
                setVersionPage(p);
                setVersionId('');
              }}
            />
          )}
          <Stack direction="row" gap={1}>
            <Button
              variant="outlined"
              disabled={!versionId || write.isPending}
              onClick={() => write.mutate(versionId)}
            >
              Gán version
            </Button>
            <Button
              disabled={!assignment.data?.policy || write.isPending}
              onClick={() => write.mutate(null)}
            >
              Bỏ gán
            </Button>
          </Stack>
        </>
      )}
      <ErrorMessage error={write.error} />
    </Stack>
  );
}
