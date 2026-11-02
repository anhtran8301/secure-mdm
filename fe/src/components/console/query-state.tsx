import type { ReactNode } from 'react';
import type { UseQueryResult } from '@tanstack/react-query';

import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import LinearProgress from '@mui/material/LinearProgress';

import { ErrorMessage } from './error-message';

/** Renders loading / error-with-retry states, then the data. */
export function QueryState<T>({
  query,
  children,
}: {
  query: UseQueryResult<T>;
  children: (data: T) => ReactNode;
}) {
  if (query.isPending) return <LinearProgress aria-label="Đang tải dữ liệu" />;
  if (query.isError) {
    return (
      <Stack gap={1}>
        <ErrorMessage error={query.error} />
        <Button onClick={() => query.refetch()}>Thử lại</Button>
      </Stack>
    );
  }
  return <>{children(query.data)}</>;
}
