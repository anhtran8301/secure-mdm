import 'dayjs/locale/vi';

import dayjs from 'dayjs';

// Importing format-time also registers dayjs' relativeTime plugin.
import { fDateTime } from 'src/utils/format-time';

const EMPTY = 'Chưa có';
const TIMESTAMP_PATTERN = 'DD/MM/YYYY HH:mm:ss';

/** Epoch milliseconds -> `02/11/2026 17:05:30`. */
export function formatTimestamp(value: number | null | undefined) {
  return value == null ? EMPTY : fDateTime(value, TIMESTAMP_PATTERN);
}

/** Epoch milliseconds -> `vài giây trước`, `2 phút trước`. */
export function formatRelative(value: number | null | undefined) {
  return value == null ? EMPTY : dayjs(value).locale('vi').fromNow();
}
