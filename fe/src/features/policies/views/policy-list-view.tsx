import type { Policy } from 'src/types/api';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';

import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';

import { usePagination } from 'src/lib/use-pagination';
import { usePermissions } from 'src/features/auth/hooks/use-permissions';

import { Iconify } from 'src/components/iconify';
import { Pager, Heading, DataTable, QueryState } from 'src/components/console';

import { policyQueries } from '../queries';
import { PolicyDialog } from '../components/policy-dialog';
import { PolicyHistoryDialog } from '../components/policy-history-dialog';

/** `undefined` = closed, `null` = new policy, Policy = new version of that policy. */
type Editing = Policy | null | undefined;

export function PolicyListView() {
  const { edit } = usePermissions();
  const { page, size, setPage, setSize } = usePagination();
  const query = useQuery(policyQueries.list({ page, size }));
  const [editing, setEditing] = useState<Editing>(undefined);
  const [history, setHistory] = useState('');

  return (
    <>
      <Heading
        title="Policies"
        description="Mỗi version là bất biến. Thiết bị chỉ đổi version khi được gán rõ ràng."
        action={
          edit && (
            <Button
              variant="contained"
              color="inherit"
              startIcon={<Iconify icon="mingcute:add-line" />}
              onClick={() => setEditing(null)}
            >
              Tạo policy
            </Button>
          )
        }
      />
      <QueryState query={query}>
        {(data) => (
          <>
            <DataTable
              rows={data.items}
              empty="Chưa có policy. Tạo yêu cầu screen lock, phiên bản Android và danh sách ứng dụng bị chặn."
              columns={[
                { title: 'Policy', render: (p) => p.name },
                { title: 'Version mới nhất', numeric: true, render: (p) => p.version },
                {
                  title: 'Yêu cầu',
                  render: (p) =>
                    `API ≥ ${p.config.minAndroidApiLevel} · Screen lock ${p.config.requireScreenLock ? 'bắt buộc' : 'tùy chọn'} · Mã hóa ổ đĩa ${p.config.requireDiskEncryption ? 'bắt buộc' : 'tùy chọn'} · ${p.config.blockedPackages.length} ứng dụng bị chặn`,
                },
                {
                  title: 'Thao tác',
                  render: (p) => (
                    <Stack direction="row">
                      <Button onClick={() => setHistory(p.id)}>Lịch sử</Button>
                      {edit && <Button onClick={() => setEditing(p)}>Tạo version</Button>}
                    </Stack>
                  ),
                },
              ]}
            />
            <Pager
              page={page}
              size={size}
              total={data.total}
              onChange={setPage}
              onSizeChange={setSize}
            />
          </>
        )}
      </QueryState>
      {editing !== undefined && (
        <PolicyDialog policy={editing} onClose={() => setEditing(undefined)} />
      )}
      {history && <PolicyHistoryDialog id={history} onClose={() => setHistory('')} />}
    </>
  );
}
