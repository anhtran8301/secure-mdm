import type { ReactNode } from 'react';

import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableRow from '@mui/material/TableRow';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import Typography from '@mui/material/Typography';
import TableContainer from '@mui/material/TableContainer';

export type DataColumn<T> = { title: string; render: (row: T) => ReactNode; numeric?: boolean };

/** Table on sm+, definition list on mobile. */
export function DataTable<T extends { id: string }>({
  rows,
  columns,
  empty,
}: {
  rows: T[];
  columns: DataColumn<T>[];
  empty: string;
}) {
  return (
    <>
      <Stack sx={{ display: { xs: 'flex', sm: 'none' } }}>
        {rows.map((row) => (
          <Box
            key={row.id}
            component="dl"
            sx={{ m: 0, py: 2, borderBottom: 1, borderColor: 'divider' }}
          >
            {columns.map((column) => (
              <Box
                key={column.title}
                sx={{
                  display: 'grid',
                  gridTemplateColumns: '100px minmax(0,1fr)',
                  gap: 1,
                  py: 0.5,
                  alignItems: 'start',
                }}
              >
                <Typography component="dt" variant="body2" color="text.secondary">
                  {column.title}
                </Typography>
                <Box
                  component="dd"
                  sx={{
                    m: 0,
                    fontSize: 14,
                    overflowWrap: 'anywhere',
                    fontVariantNumeric: 'tabular-nums',
                  }}
                >
                  {column.render(row)}
                </Box>
              </Box>
            ))}
          </Box>
        ))}
        {!rows.length && <Typography sx={{ py: 3 }}>{empty}</Typography>}
      </Stack>
      <TableContainer sx={{ overflowX: 'auto', display: { xs: 'none', sm: 'block' } }}>
        <Table size="small" sx={{ minWidth: columns.length >= 4 ? 760 : 360 }}>
          <TableHead>
            <TableRow>
              {columns.map((column) => (
                <TableCell
                  key={column.title}
                  align={column.numeric ? 'right' : 'left'}
                  sx={{ bgcolor: 'transparent' }}
                >
                  {column.title}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((row) => (
              <TableRow key={row.id} hover>
                {columns.map((column) => (
                  <TableCell
                    key={column.title}
                    align={column.numeric ? 'right' : 'left'}
                    sx={{ py: 2, fontVariantNumeric: 'tabular-nums', overflowWrap: 'anywhere' }}
                  >
                    {column.render(row)}
                  </TableCell>
                ))}
              </TableRow>
            ))}
            {!rows.length && (
              <TableRow>
                <TableCell colSpan={columns.length} sx={{ py: 4 }}>
                  {empty}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
}
