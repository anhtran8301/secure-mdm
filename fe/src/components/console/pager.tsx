import TablePagination from '@mui/material/TablePagination';

import { PAGE_SIZE } from 'src/lib/page';

// The backend rejects sizes above 100 (AppConstants.MAX_PAGE_SIZE).
const SIZE_OPTIONS = [5, 10, 20, 50, 100];

const ITEM_LABELS = {
  first: 'Trang đầu',
  last: 'Trang cuối',
  next: 'Trang sau',
  previous: 'Trang trước',
};

type Props = {
  page: number;
  total: number;
  size?: number;
  onChange: (page: number) => void;
  /** Shows the "rows per page" selector; omit for lists with a fixed size. */
  onSizeChange?: (size: number) => void;
};

export function Pager({ page, total, size = PAGE_SIZE, onChange, onSizeChange }: Props) {
  return (
    <TablePagination
      component="div"
      page={page}
      count={total}
      rowsPerPage={size}
      rowsPerPageOptions={onSizeChange ? SIZE_OPTIONS : []}
      onPageChange={(_, next) => onChange(next)}
      onRowsPerPageChange={(event) => onSizeChange?.(Number(event.target.value))}
      labelRowsPerPage="Số dòng mỗi trang:"
      labelDisplayedRows={({ from, to, count }) => `${from}–${to} / ${count}`}
      getItemAriaLabel={(type) => ITEM_LABELS[type]}
      sx={{
        // Wrap instead of overflowing on a phone; right-align on wider screens.
        '& .MuiTablePagination-spacer': { display: 'none' },
        '& .MuiTablePagination-toolbar': {
          flexWrap: 'wrap',
          rowGap: 0.5,
          px: { xs: 0, sm: 2 },
          justifyContent: { xs: 'center', sm: 'flex-end' },
        },
      }}
    />
  );
}
