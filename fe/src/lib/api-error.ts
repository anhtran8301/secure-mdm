import { isAxiosError } from 'axios';

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string
  ) {
    super(message);
  }
}

/**
 * Normalizes backend errors: {code,message,status}, Spring ProblemDetail {detail} and
 * bean-validation field maps {field: message}.
 */
export function toApiError(error: unknown): Error {
  if (error instanceof ApiError || !isAxiosError(error)) {
    return error instanceof Error ? error : new Error(String(error));
  }
  const { response } = error;
  if (!response) {
    return new ApiError(
      0,
      error.code === 'ECONNABORTED' ? 'Yêu cầu quá thời gian chờ.' : error.message
    );
  }
  const { data, status } = response;
  if (typeof data === 'string' && data.trim()) {
    return new ApiError(status, 'Máy chủ trả về phản hồi không hợp lệ.');
  }
  const body = (data ?? {}) as Record<string, unknown>;
  const message =
    typeof body.message === 'string'
      ? body.message
      : typeof body.detail === 'string'
        ? body.detail
        : Object.entries(body)
            .filter(([, value]) => typeof value === 'string')
            .map(([key, value]) => `${key}: ${value}`)
            .join('; ');
  return new ApiError(status, message || `Yêu cầu thất bại (${status}).`);
}
