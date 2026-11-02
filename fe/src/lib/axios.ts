import type { AxiosError, InternalAxiosRequestConfig } from 'axios';

import axios from 'axios';

import { useAuthStore } from 'src/stores/auth-store';

import { toApiError } from './api-error';
import { refreshSession } from './session';
import { API_BASE_URL, REQUEST_TIMEOUT_MS } from './auth-client';

/** Bearer-authenticated API client. A 401 triggers one shared refresh, then a single retry. */
export const http = axios.create({ baseURL: API_BASE_URL, timeout: REQUEST_TIMEOUT_MS });

http.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.set('Authorization', `Bearer ${token}`);
  return config;
});

type RetriableConfig = InternalAxiosRequestConfig & { retried?: boolean };

http.interceptors.response.use(undefined, async (error: AxiosError) => {
  const config = error.config as RetriableConfig | undefined;
  if (error.response?.status === 401 && config && !config.retried) {
    config.retried = true;
    await refreshSession();
    return http(config);
  }
  throw toApiError(error);
});
