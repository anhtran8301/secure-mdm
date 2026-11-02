import { useState } from 'react';

import { PAGE_SIZE } from './page';

/** Page index plus rows per page; changing the size returns to the first page. */
export function usePagination(initialSize = PAGE_SIZE) {
  const [page, setPage] = useState(0);
  const [size, setSizeState] = useState(initialSize);

  const setSize = (next: number) => {
    setSizeState(next);
    setPage(0);
  };

  return { page, size, setPage, setSize };
}
