import type { User } from 'src/types/api';

import { useAuthStore } from 'src/stores/auth-store';

import { ApiError } from './api-error';
import { authClient } from './auth-client';

type TokenResponse = { accessToken: string; user: User };

const LOGIN_REQUIRED = 'mdm-login-required';
const LOCK_NAME = 'secure-mdm-refresh';

// Only a non-secret marker is persisted. After an uncertain refresh, require a new login.
let refreshBlocked = sessionStorage.getItem(LOGIN_REQUIRED) === 'true';
let refreshing: Promise<void> | null = null;

const channel =
  typeof BroadcastChannel === 'undefined' ? null : new BroadcastChannel('secure-mdm-session');

function blockRefresh() {
  refreshBlocked = true;
  sessionStorage.setItem(LOGIN_REQUIRED, 'true');
}

if (channel) {
  // Another tab logged in, out or lost its session: this tab must sign in again.
  channel.onmessage = () => {
    blockRefresh();
    useAuthStore.getState().clearSession();
  };
}

async function cookieRequest<T>(path: string, body?: unknown): Promise<T> {
  const { data: csrf } = await authClient.get<{ token: string; headerName: string }>('/csrf', {
    timeout: 10_000,
  });
  const { data } = await authClient.post<T>(path, body, {
    headers: { [csrf.headerName]: csrf.token },
  });
  return data;
}

// Refresh cookies are shared across tabs; Web Locks prevents overlapping one-use refreshes.
function sessionLock<T>(action: () => Promise<T>): Promise<T> {
  if (!navigator.locks) {
    return Promise.reject(new Error('Trình duyệt cần hỗ trợ Web Locks để bảo vệ phiên đăng nhập.'));
  }
  return navigator.locks.request(LOCK_NAME, action);
}

/** Concurrent callers share one refresh; a failed or uncertain refresh is never retried. */
export function refreshSession(): Promise<void> {
  const { setSession, clearSession } = useAuthStore.getState();
  if (refreshBlocked) {
    clearSession();
    return Promise.reject(new ApiError(401, 'Vui lòng đăng nhập lại.'));
  }
  if (!refreshing) {
    refreshing = sessionLock(async () => {
      if (refreshBlocked) throw new ApiError(401, 'Vui lòng đăng nhập lại.');
      const data = await cookieRequest<TokenResponse>('/refresh');
      setSession(data.user, data.accessToken);
    })
      .catch((error) => {
        blockRefresh();
        channel?.postMessage('login-required');
        clearSession();
        throw error;
      })
      .finally(() => {
        refreshing = null;
      });
  }
  return refreshing;
}

export type LoginCredentials = { organizationSlug: string; email: string; password: string };

export async function login(credentials: LoginCredentials) {
  await sessionLock(async () => {
    const data = await cookieRequest<TokenResponse>('/login', credentials);
    refreshBlocked = false;
    sessionStorage.removeItem(LOGIN_REQUIRED);
    channel?.postMessage('session-changed');
    useAuthStore.getState().setSession(data.user, data.accessToken);
  });
}

export async function logout() {
  try {
    await sessionLock(() => cookieRequest('/logout'));
  } finally {
    blockRefresh();
    useAuthStore.getState().clearSession();
    channel?.postMessage('logout');
  }
}
