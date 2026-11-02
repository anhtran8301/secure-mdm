import { useQuery } from '@tanstack/react-query';

import { formatTimestamp } from 'src/lib/format';
import { usePagination } from 'src/lib/use-pagination';

import { Pager, Heading, DataTable, QueryState } from 'src/components/console';

import { auditQueries } from '../queries';

export function AuditView() {
  const { page, size, setPage, setSize } = usePagination();
  const query = useQuery(auditQueries.list({ page, size }));

  return (
    <>
      <Heading
        title="Audit"
        description="Nhật ký thao tác trong tổ chức. Correlation ID dùng để đối chiếu request."
      />
      <QueryState query={query}>
        {(data) => (
          <>
            <DataTable
              rows={data.items}
              empty="Chưa có sự kiện audit."
              columns={[
                { title: 'Thời điểm', render: (a) => formatTimestamp(a.createdAt) },
                { title: 'Hành động', render: (a) => a.action },
                { title: 'Actor', render: (a) => `${a.actorType} · ${a.actorId}` },
                { title: 'Resource', render: (a) => `${a.resourceType} · ${a.resourceId}` },
                { title: 'Correlation ID', render: (a) => a.correlationId },
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
    </>
  );
}
