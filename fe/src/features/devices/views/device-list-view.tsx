import { debounce } from 'es-toolkit';
import { useQuery } from '@tanstack/react-query';
import { useMemo, useState, useEffect } from 'react';

import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';

import { Iconify } from 'src/components/iconify';
import { Pager, Heading, QueryState } from 'src/components/console';

import { deviceQueries } from '../queries';
import { useDeviceFilterStore } from '../store';
import { DeviceTable } from '../components/device-table';

const SEARCH_DEBOUNCE_MS = 400;

export function DeviceListView() {
  const { search, page, size, setSearch, setPage, setSize } = useDeviceFilterStore();
  const [draft, setDraft] = useState(search);
  const query = useQuery(deviceQueries.list({ page, size, search }));

  // Typing filters after a short pause; the button and Enter apply the filter immediately.
  const applySearch = useMemo(() => debounce(setSearch, SEARCH_DEBOUNCE_MS), [setSearch]);
  useEffect(() => () => applySearch.cancel(), [applySearch]);

  return (
    <>
      <Heading title="Thiết bị" description="Thiết bị offline sau 90 giây không có heartbeat." />
      <Stack
        component="form"
        direction="row"
        gap={1}
        mb={2}
        onSubmit={(event) => {
          event.preventDefault();
          applySearch.cancel();
          setSearch(draft.trim());
        }}
      >
        <TextField
          size="small"
          label="Tìm tên thiết bị"
          value={draft}
          slotProps={{ htmlInput: { maxLength: 120 } }}
          onChange={(event) => {
            setDraft(event.target.value);
            applySearch(event.target.value.trim());
          }}
        />
        <Button type="submit" startIcon={<Iconify icon="eva:search-fill" />}>
          Tìm kiếm
        </Button>
      </Stack>
      <QueryState query={query}>
        {(data) => (
          <>
            <DeviceTable rows={data.items} />
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
