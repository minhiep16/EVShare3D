/**
 * EVShare 3D Strongly Typed Normalized API Error Subsystem
 * Unifies network failures, timeout errors, request cancellations,
 * and Spring Boot ApiErrorResponse envelopes with field-level validation errors.
 */

import axios, { AxiosError } from 'axios';
import { ApiErrorResponse, ValidationError } from './types';

export class ApiError extends Error {
  public readonly status: number;
  public readonly code: string;
  public readonly path?: string;
  public readonly validationErrors?: ValidationError[];
  public readonly timestamp: string;
  public readonly isCancel: boolean;
  public readonly isNetworkError: boolean;
  public readonly isAuthError: boolean;
  public readonly isTimeout: boolean;
  public readonly rawError?: unknown;

  constructor(params: {
    message: string;
    status?: number;
    code?: string;
    path?: string;
    validationErrors?: ValidationError[];
    timestamp?: string;
    isCancel?: boolean;
    isNetworkError?: boolean;
    isAuthError?: boolean;
    isTimeout?: boolean;
    rawError?: unknown;
  }) {
    super(params.message);
    this.name = 'ApiError';
    this.status = params.status ?? 0;
    this.code = params.code ?? 'UNKNOWN_ERROR';
    this.path = params.path;
    this.validationErrors = params.validationErrors;
    this.timestamp = params.timestamp ?? new Date().toISOString();
    this.isCancel = params.isCancel ?? false;
    this.isNetworkError = params.isNetworkError ?? false;
    this.isAuthError = params.isAuthError ?? (this.status === 401 || this.status === 403);
    this.isTimeout = params.isTimeout ?? false;
    this.rawError = params.rawError;

    // Restore prototype chain for ES5/ES6 compatibility
    Object.setPrototypeOf(this, ApiError.prototype);
  }

  /**
   * Transforms any unknown error (including AxiosError or DOMException) into a typed ApiError.
   */
  public static fromError(error: unknown): ApiError {
    if (error instanceof ApiError) {
      return error;
    }

    // 1. Request Cancellation (Axios CanceledError or DOM AbortError)
    if (axios.isCancel(error) || (error instanceof DOMException && error.name === 'AbortError')) {
      const msg = error instanceof Error ? error.message : 'Request was cancelled';
      return new ApiError({
        message: msg || 'Request cancelled by client',
        status: 0,
        code: 'CANCELED',
        isCancel: true,
        rawError: error,
      });
    }

    // 2. Axios Error
    if (axios.isAxiosError(error)) {
      const axiosErr = error as AxiosError<ApiErrorResponse>;

      // Timeout
      if (axiosErr.code === 'ECONNABORTED' || axiosErr.message.includes('timeout')) {
        return new ApiError({
          message: 'Request timed out. Please check connection and retry.',
          status: 0,
          code: 'TIMEOUT',
          isTimeout: true,
          rawError: error,
        });
      }

      // Network Offline / Failure without response
      if (!axiosErr.response) {
        return new ApiError({
          message: axiosErr.message || 'Network connection failed. Backend service may be unreachable.',
          status: 0,
          code: 'NETWORK_ERROR',
          isNetworkError: true,
          rawError: error,
        });
      }

      // Backend returned an HTTP response
      const res = axiosErr.response;
      const status = res.status;
      const data = res.data;

      // Extract message from standard Spring Boot ApiErrorResponse or generic body
      let message = 'An unexpected server error occurred';
      let path: string | undefined;
      let validationErrors: ValidationError[] | undefined;

      if (data && typeof data === 'object') {
        if ('message' in data && typeof data.message === 'string' && data.message.trim()) {
          message = data.message;
        } else if ('error' in data && typeof data.error === 'string' && data.error.trim()) {
          message = data.error;
        }

        if ('path' in data && typeof data.path === 'string') {
          path = data.path;
        }

        if ('validationErrors' in data && Array.isArray(data.validationErrors)) {
          validationErrors = data.validationErrors;
        }
      }

      let code = `HTTP_${status}`;
      if (status === 400) code = validationErrors?.length ? 'VALIDATION_ERROR' : 'BAD_REQUEST';
      else if (status === 401) code = 'UNAUTHORIZED';
      else if (status === 403) code = 'FORBIDDEN';
      else if (status === 404) code = 'NOT_FOUND';
      else if (status === 409) code = 'CONFLICT';
      else if (status === 500) code = 'INTERNAL_SERVER_ERROR';

      return new ApiError({
        message,
        status,
        code,
        path,
        validationErrors,
        timestamp: (data && 'timestamp' in data && typeof data.timestamp === 'string')
          ? data.timestamp
          : new Date().toISOString(),
        isAuthError: status === 401 || status === 403,
        rawError: error,
      });
    }

    // 3. Generic Error
    if (error instanceof Error) {
      return new ApiError({
        message: error.message,
        status: 0,
        code: 'CLIENT_ERROR',
        rawError: error,
      });
    }

    // 4. Unknown string or object
    return new ApiError({
      message: String(error) || 'An unknown error occurred',
      status: 0,
      code: 'UNKNOWN_ERROR',
      rawError: error,
    });
  }

  /**
   * Returns a friendly formatted string including field validation details if available.
   */
  public getDetailedMessage(): string {
    if (this.validationErrors && this.validationErrors.length > 0) {
      const details = this.validationErrors
        .map((v) => `${v.field}: ${v.message}`)
        .join('; ');
      return `${this.message} (${details})`;
    }
    return this.message;
  }
}

export function isApiError(err: unknown): err is ApiError {
  return err instanceof ApiError;
}

export function isCancelError(err: unknown): boolean {
  if (err instanceof ApiError) return err.isCancel;
  return axios.isCancel(err) || (err instanceof DOMException && err.name === 'AbortError');
}

export function getErrorMessage(err: unknown): string {
  if (err instanceof ApiError) {
    return err.getDetailedMessage();
  }
  if (err instanceof Error) {
    return err.message;
  }
  return String(err || 'Unknown error occurred');
}
