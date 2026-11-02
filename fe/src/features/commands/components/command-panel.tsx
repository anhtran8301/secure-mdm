import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';

import { formatTimestamp } from 'src/lib/format';
import { usePagination } from 'src/lib/use-pagination';

import { Form, RHFSelect, RHFTextField } from 'src/components/hook-form';
import { Pager, Status, DataTable, QueryState, ErrorMessage } from 'src/components/console';

import { commandQueries } from '../queries';
import { useCreateCommand } from '../mutations';
import { COMMAND_TYPES, commandSchema, toCommandBody } from '../schemas';

import type { CommandFormValues } from '../schemas';

const TYPE_OPTIONS = COMMAND_TYPES.map((type) => ({ value: type, label: type }));

type Props = { deviceId: string; enabled: boolean };

export function CommandPanel({ deviceId, enabled }: Props) {
  const { page, size, setPage, setSize } = usePagination();
  const query = useQuery(commandQueries.list(deviceId, { page, size }));
  const send = useCreateCommand(deviceId);
  const methods = useForm<CommandFormValues>({
    resolver: zodResolver(commandSchema),
    defaultValues: { type: 'REPORT_STATUS', message: '' },
  });
  const [type, message] = useWatch({ control: methods.control, name: ['type', 'message'] });

  // One key per distinct request: retries reuse it, any edit or success starts a new one.
  const [idempotencyKey, setIdempotencyKey] = useState(() => crypto.randomUUID());
  const { reset: resetSend } = send;
  useEffect(() => {
    setIdempotencyKey(crypto.randomUUID());
    resetSend();
  }, [type, message, resetSend]);

  const submit = (values: CommandFormValues) =>
    send
      .mutateAsync({ body: toCommandBody(values), idempotencyKey })
      .then(() => setIdempotencyKey(crypto.randomUUID()))
      .catch(() => {});

  return (
    <Stack gap={2}>
      <Typography variant="h6" component="h2">
        Commands
      </Typography>
      <Typography color="text.secondary">
        Lệnh được ký; tác động chỉ diễn ra trong simulator. UNKNOWN khác với FAILED: máy chủ chưa
        xác định được kết quả.
      </Typography>
      {enabled && (
        <Form methods={methods} onSubmit={submit}>
          <Stack gap={2}>
            <RHFSelect
              name="type"
              label="Loại command"
              options={TYPE_OPTIONS}
              disabled={send.isPending}
            />
            {type === 'SHOW_MESSAGE' && (
              <RHFTextField
                name="message"
                label="Nội dung thông báo"
                required
                disabled={send.isPending}
                slotProps={{ htmlInput: { maxLength: 500 } }}
              />
            )}
            <Box>
              <Button variant="contained" color="inherit" type="submit" disabled={send.isPending}>
                Gửi command mô phỏng
              </Button>
            </Box>
          </Stack>
        </Form>
      )}
      <ErrorMessage error={send.error} />
      <QueryState query={query}>
        {(data) => (
          <>
            <DataTable
              rows={data.items}
              empty="Chưa có command cho thiết bị này."
              columns={[
                {
                  title: 'Command',
                  render: (c) => (
                    <>
                      <Typography variant="body2">{c.type}</Typography>
                      <Typography variant="caption">{c.id}</Typography>
                    </>
                  ),
                },
                { title: 'Trạng thái', render: (c) => <Status value={c.status} /> },
                { title: 'Attempts', numeric: true, render: (c) => c.attempts },
                { title: 'Thời gian tạo', render: (c) => formatTimestamp(c.createdAt) },
                {
                  title: 'Kết quả',
                  render: (c) => (c.result ? JSON.stringify(c.result) : 'Chưa có'),
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
    </Stack>
  );
}
