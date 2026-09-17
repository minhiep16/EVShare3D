/**
 * EVShare 3D Centralized API Client
 *
 * Implements:
 * - Configurable Base URL & API Versioning (v1)
 * - Automatic Bearer Authentication via TokenManager
 * - Concurrent 401 Refresh Token Rotation Queue (RFC 6819)
 * - Normalized Error Handling with ApiError
 * - Request Cancellation (AbortController & Deduplication by Key)
 * - Typed DTO & Consistent Response Parsing (requestData, apiGet, apiPost, etc.)
 */

import axios, {
  AxiosInstance,
  AxiosRequestConfig,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from 'axios';
import { TokenManager } from './tokenManager';
import { ApiError, isCancelError } from './apiError';
import { ApiResponse, AuthTokens } from './types';
import { useAppStore } from '@/stores/useAppStore';

const DEFAULT_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';
const DEFAULT_TIMEOUT_MS = 10000;
const DEFAULT_API_VERSION = 'v1';

interface ExtendedAxiosRequestConfig extends InternalAxiosRequestConfig {
  _retryCount?: number;
  _skipAuthRefresh?: boolean;
}

class ApiClientService {
  private axiosInstance: AxiosInstance;
  private currentBaseUrl: string;
  private currentApiVersion: string;

  // Mutex & Queue for RFC 6819 Token Refresh
  private isRefreshing = false;
  private refreshSubscribers: Array<(token: string) => void> = [];
  private refreshFailureSubscribers: Array<(err: ApiError) => void> = [];

  // Key-based pending request cancellation controllers
  private pendingControllers = new Map<string, AbortController>();

  constructor(baseURL = DEFAULT_BASE_URL, apiVersion = DEFAULT_API_VERSION) {
    this.currentBaseUrl = baseURL;
    this.currentApiVersion = apiVersion;

    this.axiosInstance = axios.create({
      baseURL: this.currentBaseUrl,
      timeout: DEFAULT_TIMEOUT_MS,
      headers: {
        'Content-Type': 'application/json',
        'X-API-Version': this.currentApiVersion,
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors(): void {
    // ─── 1. REQUEST INTERCEPTOR ──────────────────────────────────
    this.axiosInstance.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        // Attach version header if missing
        if (!config.headers['X-API-Version']) {
          config.headers['X-API-Version'] = this.currentApiVersion;
        }

        // Attach Authorization Bearer token if not explicitly provided
        const token = TokenManager.getAccessToken();
        if (token && !config.headers.Authorization) {
          config.headers.Authorization = `Bearer ${token}`;
        }

        return config;
      },
      (error) => {
        return Promise.reject(ApiError.fromError(error));
      }
    );

    // ─── 2. RESPONSE INTERCEPTOR & 401 REFRESH QUEUE ──────────────
    this.axiosInstance.interceptors.response.use(
      (response: AxiosResponse) => {
        return response;
      },
      async (error: unknown) => {
        // If request was cancelled by client, reject immediately without refresh attempt
        if (isCancelError(error)) {
          return Promise.reject(ApiError.fromError(error));
        }

        if (!axios.isAxiosError(error) || !error.response) {
          return Promise.reject(ApiError.fromError(error));
        }

        const originalConfig = error.config as ExtendedAxiosRequestConfig | undefined;
        const status = error.response.status;

        // Check if error is 401 Unauthorized
        if (status === 401 && originalConfig && !originalConfig._skipAuthRefresh) {
          const requestUrl = originalConfig.url || '';

          // Do not attempt token refresh on authentication endpoints themselves
          const isAuthEndpoint =
            requestUrl.includes('/auth/login') ||
            requestUrl.includes('/auth/refresh') ||
            requestUrl.includes('/auth/register');

          if (isAuthEndpoint) {
            return Promise.reject(ApiError.fromError(error));
          }

          // Check if already retried once to prevent infinite loops
          if (originalConfig._retryCount && originalConfig._retryCount > 0) {
            return Promise.reject(ApiError.fromError(error));
          }
          originalConfig._retryCount = 1;

          const currentRefreshToken = TokenManager.getRefreshToken();
          if (!currentRefreshToken) {
            // No refresh token available, logout immediately
            this.handleSessionTermination();
            return Promise.reject(ApiError.fromError(error));
          }

          // If refresh is already in flight, queue this request until refresh completes
          if (this.isRefreshing) {
            return new Promise((resolve, reject) => {
              this.refreshSubscribers.push((newToken: string) => {
                if (originalConfig.headers) {
                  originalConfig.headers.Authorization = `Bearer ${newToken}`;
                }
                resolve(this.axiosInstance(originalConfig));
              });

              this.refreshFailureSubscribers.push((refreshErr: ApiError) => {
                reject(refreshErr);
              });
            });
          }

          // Begin Token Refresh Flow (RFC 6819)
          this.isRefreshing = true;

          try {
            // Call Spring Boot /api/v1/auth/refresh without triggering recursive interceptors
            const refreshUrl = `${this.currentBaseUrl}/auth/refresh`.replace(/([^:]\/)\/+/g, '$1');
            const refreshResponse = await axios.post<ApiResponse<AuthTokens>>(
              refreshUrl,
              { refreshToken: currentRefreshToken },
              {
                headers: {
                  'Content-Type': 'application/json',
                  'X-API-Version': this.currentApiVersion,
                },
              }
            );

            const authData = refreshResponse.data.data;
            const newAccessToken = authData.accessToken;
            const newRefreshToken = authData.refreshToken || currentRefreshToken;

            // Persist rotated tokens
            TokenManager.setTokens(newAccessToken, newRefreshToken);

            // Synchronize Zustand app session state
            const currentAuth = useAppStore.getState().auth;
            useAppStore.getState().setAuthSession({
              ...currentAuth,
              token: newAccessToken,
            });

            // Drain queued requests with the fresh token
            this.refreshSubscribers.forEach((callback) => callback(newAccessToken));
            this.refreshSubscribers = [];
            this.refreshFailureSubscribers = [];

            // Replay the initial failed request with the new access token
            if (originalConfig.headers) {
              originalConfig.headers.Authorization = `Bearer ${newAccessToken}`;
            }
            return this.axiosInstance(originalConfig);
          } catch (refreshErr) {
            // Refresh token has expired, been revoked, or is invalid
            const normalizedErr = ApiError.fromError(refreshErr);

            this.handleSessionTermination();

            // Reject all queued requests
            this.refreshFailureSubscribers.forEach((callback) => callback(normalizedErr));
            this.refreshSubscribers = [];
            this.refreshFailureSubscribers = [];

            return Promise.reject(normalizedErr);
          } finally {
            this.isRefreshing = false;
          }
        }

        return Promise.reject(ApiError.fromError(error));
      }
    );
  }

  private handleSessionTermination(): void {
    TokenManager.clearTokens();
    useAppStore.getState().clearAuthSession();
  }

  // ─── CONFIGURATION & VERSIONING ─────────────────────────────

  public setBaseUrl(url: string): void {
    this.currentBaseUrl = url;
    this.axiosInstance.defaults.baseURL = url;
  }

  public getBaseUrl(): string {
    return this.currentBaseUrl;
  }

  public setApiVersion(version: string): void {
    this.currentApiVersion = version;
    this.axiosInstance.defaults.headers.common['X-API-Version'] = version;
  }

  public getApiVersion(): string {
    return this.currentApiVersion;
  }

  public buildVersionedUrl(endpoint: string, version = this.currentApiVersion): string {
    const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
    return `/api/${version}${cleanEndpoint}`;
  }

  // ─── REQUEST CANCELLATION UTILITIES ──────────────────────────

  public createAbortController(): { controller: AbortController; signal: AbortSignal } {
    const controller = new AbortController();
    return { controller, signal: controller.signal };
  }

  /**
   * Cancels any pending request associated with the specified key.
   */
  public cancelPending(key: string, reason = 'Superseded by newer request'): void {
    const existing = this.pendingControllers.get(key);
    if (existing) {
      existing.abort(reason);
      this.pendingControllers.delete(key);
    }
  }

  /**
   * Executes a GET request, automatically cancelling any previous in-flight request
   * with the identical key (e.g. rapid 3D searches or timeline interval switching).
   */
  public async getWithKey<T = unknown>(
    key: string,
    url: string,
    config?: AxiosRequestConfig
  ): Promise<AxiosResponse<T>> {
    this.cancelPending(key);
    const controller = new AbortController();
    this.pendingControllers.set(key, controller);

    try {
      const response = await this.axiosInstance.get<T>(url, {
        ...config,
        signal: controller.signal,
      });
      return response;
    } finally {
      if (this.pendingControllers.get(key) === controller) {
        this.pendingControllers.delete(key);
      }
    }
  }

  // ─── CONSISTENT RESPONSE PARSING & UNWRAPPING ─────────────────

  /**
   * Executes an HTTP request, validates the Spring Boot ApiResponse envelope,
   * and directly unwraps the inner `data` payload.
   */
  public async requestData<T = unknown>(config: AxiosRequestConfig): Promise<T> {
    try {
      const response = await this.axiosInstance.request<ApiResponse<T>>(config);
      if (response.data && typeof response.data === 'object' && 'data' in response.data) {
        return response.data.data;
      }
      return (response.data as unknown) as T;
    } catch (err) {
      throw ApiError.fromError(err);
    }
  }

  public async apiGet<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return this.requestData<T>({ ...config, method: 'GET', url });
  }

  public async apiPost<T = unknown>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig
  ): Promise<T> {
    return this.requestData<T>({ ...config, method: 'POST', url, data });
  }

  public async apiPut<T = unknown>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig
  ): Promise<T> {
    return this.requestData<T>({ ...config, method: 'PUT', url, data });
  }

  public async apiPatch<T = unknown>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig
  ): Promise<T> {
    return this.requestData<T>({ ...config, method: 'PATCH', url, data });
  }

  public async apiDelete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return this.requestData<T>({ ...config, method: 'DELETE', url });
  }

  // ─── RAW AXIOS INSTANCE ACCESS ───────────────────────────────

  public get raw(): AxiosInstance {
    return this.axiosInstance;
  }

  // Delegate core Axios methods for 100% backwards compatibility
  public get = <T = unknown, R = AxiosResponse<T>, D = unknown>(
    url: string,
    config?: AxiosRequestConfig<D>
  ): Promise<R> => this.axiosInstance.get<T, R, D>(url, config);

  public post = <T = unknown, R = AxiosResponse<T>, D = unknown>(
    url: string,
    data?: D,
    config?: AxiosRequestConfig<D>
  ): Promise<R> => this.axiosInstance.post<T, R, D>(url, data, config);

  public put = <T = unknown, R = AxiosResponse<T>, D = unknown>(
    url: string,
    data?: D,
    config?: AxiosRequestConfig<D>
  ): Promise<R> => this.axiosInstance.put<T, R, D>(url, data, config);

  public patch = <T = unknown, R = AxiosResponse<T>, D = unknown>(
    url: string,
    data?: D,
    config?: AxiosRequestConfig<D>
  ): Promise<R> => this.axiosInstance.patch<T, R, D>(url, data, config);

  public delete = <T = unknown, R = AxiosResponse<T>, D = unknown>(
    url: string,
    config?: AxiosRequestConfig<D>
  ): Promise<R> => this.axiosInstance.delete<T, R, D>(url, config);

  public request = <T = unknown, R = AxiosResponse<T>, D = unknown>(
    config: AxiosRequestConfig<D>
  ): Promise<R> => this.axiosInstance.request<T, R, D>(config);

  public get interceptors() {
    return this.axiosInstance.interceptors;
  }

  public get defaults() {
    return this.axiosInstance.defaults;
  }
}

export const apiClient = new ApiClientService();
export default apiClient;
