/**
 * EVShare 3D Centralized API Types & DTO Definitions
 * Aligns with Spring Boot backend response envelopes (ApiResponse & ApiErrorResponse).
 */

export interface ApiResponse<T = unknown> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface ValidationError {
  field: string;
  rejectedValue?: string | null;
  message: string;
}

export interface ApiErrorResponse {
  success: boolean;
  status: number;
  error: string;
  message: string;
  path: string;
  timestamp: string;
  validationErrors?: ValidationError[];
}

export interface PagedData<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresIn?: number;
}

export interface ApiClientConfig {
  baseURL?: string;
  apiVersion?: string;
  timeout?: number;
  headers?: Record<string, string>;
  tokenStorageKey?: string;
  refreshTokenStorageKey?: string;
}

export interface RequestCancellationHandle {
  controller: AbortController;
  cancel: (reason?: string) => void;
}
