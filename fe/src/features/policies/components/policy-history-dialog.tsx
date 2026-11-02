import { useQuery } from '@tanstack/react-query';

import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';

import { usePagination } from 'src/lib/use-pagination';

import { Pager, DataTable, QueryState } from 'src/components/console';

import { policyQueries } from '../queries';

export function PolicyHistoryDialog({ id, onClose }: { id: string; onClose: () => void }) {
  const { page, size, setPage, setSize } = usePagination();
  const query = useQuery(policyQueries.versions(id, { page, size }));

  return (
    <Dialog open onClose={onClose} fullWidth aria-labelledby="history-title">
      <DialogTitle id="history-title">Lịch sử policy</DialogTitle>
      <DialogContent>
        <QueryState query={query}>
          {(data) => (
            <>
              <DataTable
                rows={data.items}
                empty="Chưa có version."
                columns={[
                  { title: 'Version', numeric: true, render: (v) => v.version },
                  { title: 'Cấu hình', render: (v) => JSON.stringify(v.config) },
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
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Đóng</Button>
      </DialogActions>
    </Dialog>
  );
}
