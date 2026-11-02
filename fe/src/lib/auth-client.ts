import axios from 'axios';

import { toApiError } from './api-error';

export const API_BASE_URL = '/api/v1';
export const REQUEST_TIMEOUT_MS = 15_000;

/** Cookie-only auth endpoints (CSRF, login, refresh, logout): no bearer token, no retry. */
export const authClient = axios.create({
  baseURL: `${API_BASE_URL}/auth`,
  timeout: REQUEST_TIMEOUT_MS,
  withCredentials: true,
});

authClient.interceptors.response.use(undefined, (error) => {
  throw toApiError(error);
});
