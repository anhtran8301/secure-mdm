import type { Theme } from '@mui/material/styles';
import type { LabelColor } from 'src/components/label';

import { Label } from 'src/components/label';

const COLORS: Record<string, LabelColor> = {
  ACTIVE: 'success',
  ONLINE: 'success',
  COMPLIANT: 'success',
  SUCCEEDED: 'success',
  FAILED: 'error',
  NON_COMPLIANT: 'error',
  REVOKED: 'error',
  // UNKNOWN is a warning, not an error: the outcome is undetermined, not failed.
  UNKNOWN: 'warning',
  PENDING: 'warning',
  IN_PROGRESS: 'warning',
};

export function statusColor(value: string): LabelColor {
  return COLORS[value] ?? 'default';
}

/** `NON_COMPLIANT` -> `Non compliant`, the way Minimal's status labels read. */
export function statusText(value: string) {
  const text = value.toLowerCase().replace(/_/g, ' ');
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function Status({ value }: { value: string }) {
  return <Label color={statusColor(value)}>{statusText(value)}</Label>;
}

/** The label's colour as a solid chart colour. */
export function statusMainColor(theme: Theme, value: string) {
  const color = statusColor(value);
  return color === 'default' ? theme.palette.grey[500] : theme.palette[color].main;
}
