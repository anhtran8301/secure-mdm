import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios';

import { AxiosError, AxiosHeaders } from 'axios';
import { it, vi, expect, describe, beforeEach } from 'vitest';

const user = { id: 'u', organizationId: 'org', organizationSlug: 'alpha', role: 'ORG_ADMIN' };

type Handler = (config: InternalAxiosRequestConfig) => Promise<{ status: number; data: unknown }>;

/** Fake transport: resolves 2xx and rejects other statuses like axios' own adapters. */
function adapter(handler: Handler) {
  return async (config: InternalAxiosRequestConfig): Promise<AxiosResponse> => {
    const { status, data } = await handler(config);
    const response = { data, status, statusText: '', headers: new AxiosHeaders(), config };
    if (status >= 400) {
      throw new AxiosError(`HTTP ${status}`, 'ERR_BAD_RESPONSE', config, null, response);
    }
    return response;
  };
}

async function load(handler: Handler) {
  const [{ http }, { authClient }, session, { useAuthStore }, errors] = await Promise.all([
    import('./axios'),
    import('./auth-client'),
    import('./session'),
    import('src/stores/auth-store'),
    import('./api-error'),
  ]);
  http.defaults.adapter = adapter(handler);
  authClient.defaults.adapter = adapter(handler);
  return { http, session, useAuthStore, errors };
}

beforeEach(() => {
  sessionStorage.clear();
  vi.resetModules();
  vi.stubGlobal('BroadcastChannel', undefined);
  Object.defineProperty(navigator, 'locks', {
    configurable: true,
    value: { request: async (_: string, callback: () => unknown) => callback() },
  });
});

describe('session and API client', () => {
  it('coalesces simultaneous expired requests into one refresh', async () => {
    let refreshes = 0;
    const { http } = await load(async (config) => {
      if (config.url === '/csrf')
        return { status: 200, data: { token: 'c', headerName: 'X-XSRF-TOKEN' } };
      if (config.url === '/refresh') {
        refreshes += 1;
        await Promise.resolve();
        return { status: 200, data: { accessToken: 'new-access', user } };
      }
      return config.headers.get('Authorization') === 'Bearer new-access'
        ? { status: 200, data: { items: [] } }
        : { status: 401, data: { message: 'expired' } };
    });
    await Promise.all([http.get('/devices'), http.get('/policies')]);
    expect(refreshes).toBe(1);
  });

  it('does not blindly retry an indeterminate refresh response', async () => {
    const calls: string[] = [];
    const { session, useAuthStore } = await load(async (config) => {
      calls.push(config.url ?? '');
      if (config.url === '/csrf')
        return { status: 200, data: { token: 'c', headerName: 'X-XSRF-TOKEN' } };
      throw new AxiosError('network interrupted', 'ERR_NETWORK', config);
    });
    await expect(session.refreshSession()).rejects.toThrow('network interrupted');
    await expect(session.refreshSession()).rejects.toThrow('Vui lòng đăng nhập lại.');
    expect(calls).toEqual(['/csrf', '/refresh']);
    expect(useAuthStore.getState()).toMatchObject({ user: null, accessToken: null, ready: true });
  });

  it('normalizes common, ProblemDetail and validation errors', async () => {
    const bodies: unknown[] = [
      { code: 'MDM403', message: 'Denied', status: 'FORBIDDEN' },
      { detail: 'Invalid body' },
      { email: 'must be valid' },
    ];
    const { http } = await load(async () => ({ status: 400, data: bodies.shift() }));
    for (const message of ['Denied', 'Invalid body', 'email: must be valid']) {
      await expect(http.get('/x')).rejects.toThrow(message);
    }
  });
});
