import { it, vi, expect, describe, afterEach, beforeEach } from 'vitest';

import { formatRelative, formatTimestamp } from './format';

const NOW = new Date(2026, 10, 2, 17, 5, 30).getTime();

describe('time formatting', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(NOW);
  });
  afterEach(() => vi.useRealTimers());

  it('formats an absolute timestamp as DD/MM/YYYY HH:mm:ss', () => {
    expect(formatTimestamp(NOW)).toBe('02/11/2026 17:05:30');
  });

  it('formats a relative time in Vietnamese', () => {
    expect(formatRelative(NOW - 5 * 60 * 1000)).toBe('5 phút trước');
  });

  it('shows a placeholder when there is no value', () => {
    expect(formatTimestamp(null)).toBe('Chưa có');
    expect(formatRelative(undefined)).toBe('Chưa có');
  });
});
