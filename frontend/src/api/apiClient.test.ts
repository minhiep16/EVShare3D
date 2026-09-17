import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import axios, { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { apiClient } from './apiClient';
import { TokenManager, StorageLike } from './tokenManager';
import { ApiError, isApiError, isCancelError } from './apiError';
import { useAppStore } from '@/stores/useAppStore';

class MockMemoryStorage implements StorageLike {
  private data: Record<string, string> = {};

  getItem(key: string): string | null {
    return this.data[key] ?? null;
  }
  setItem(key: string, value: string): void {
    this.data[key] = String(value);
  }
  removeItem(key: string): void {
    delete this.data[key];
  }
  clear(): void {
    this.data = {};
  }
}

describe('Centralized API Client Subsystem (09-P)', () => {
  let mockStorage: MockMemoryStorage;

  beforeEach(() => {
    vi.clearAllMocks();
    mockStorage = new MockMemoryStorage();
    TokenManager.setStorage(mockStorage);
    TokenManager.clearTokens();
    useAppStore.getState().clearAuthSession();
    apiClient.setBaseUrl('/api/v1');
    apiClient.setApiVersion('v1');
  });

  afterEach(() => {
    TokenManager.clearTokens();
    TokenManager.setStorage(null);
    useAppStore.getState().clearAuthSession();
  });

  describe('1. Base URL & API Versioning', () => {
    it('initializes with default base URL /api/v1 and API version v1', () => {
      expect(apiClient.getBaseUrl()).toBe('/api/v1');
      expect(apiClient.getApiVersion()).toBe('v1');
    });

    it('allows dynamic base URL and version configuration', () => {
      apiClient.setBaseUrl('https://api.evshare.io/api/v2');
      apiClient.setApiVersion('v2');
      expect(apiClient.getBaseUrl()).toBe('https://api.evshare.io/api/v2');
      expect(apiClient.getApiVersion()).toBe('v2');
    });

    it('builds versioned endpoint paths correctly', () => {
      expect(apiClient.buildVersionedUrl('vehicles')).toBe('/api/v1/vehicles');
      expect(apiClient.buildVersionedUrl('/bookings/123')).toBe('/api/v1/bookings/123');
      expect(apiClient.buildVersionedUrl('payments', 'v2')).toBe('/api/v2/payments');
    });
  });

  describe('2. Authentication & Token Management', () => {
    it('persists and retrieves access and refresh tokens via TokenManager', () => {
      TokenManager.setTokens('access_jwt_123', 'refresh_jwt_456');
      expect(TokenManager.getAccessToken()).toBe('access_jwt_123');
      expect(TokenManager.getRefreshToken()).toBe('refresh_jwt_456');
      expect(TokenManager.hasAccessToken()).toBe(true);

      TokenManager.clearTokens();
      expect(TokenManager.getAccessToken()).toBeNull();
      expect(TokenManager.getRefreshToken()).toBeNull();
      expect(TokenManager.hasAccessToken()).toBe(false);
    });

    it('automatically migrates legacy evshare_token to access token', () => {
      mockStorage.setItem('evshare_token', 'legacy_jwt_abc');
      TokenManager.migrateLegacyToken();
      expect(TokenManager.getAccessToken()).toBe('legacy_jwt_abc');
    });

    it('notifies token change subscribers when tokens update', () => {
      const listener = vi.fn();
      const unsub = TokenManager.subscribe(listener);

      TokenManager.setTokens('token_a', 'token_b');
      expect(listener).toHaveBeenCalledWith({
        accessToken: 'token_a',
        refreshToken: 'token_b',
      });

      unsub();
      TokenManager.clearTokens();
      expect(listener).toHaveBeenCalledTimes(1); // not called again after unsubscribe
    });
  });

  describe('3. RFC 6819 Token Refresh Mutex Queue', () => {
    it('refreshes token on 401 and replays queued requests with the rotated token', async () => {
      TokenManager.setTokens('expired_access_token', 'valid_refresh_token');

      // Mock axios.post for the refresh call
      const postSpy = vi.spyOn(axios, 'post').mockResolvedValueOnce({
        status: 200,
        data: {
          success: true,
          message: 'Token refreshed successfully',
          data: {
            accessToken: 'new_rotated_access_token',
            refreshToken: 'new_rotated_refresh_token',
          },
          timestamp: new Date().toISOString(),
        },
      });

      let callCount = 0;
      apiClient.raw.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
        callCount++;
        if (callCount === 1) {
          // First attempt returns 401
          const err = new AxiosError(
            'Unauthorized',
            'ERR_BAD_REQUEST',
            config,
            {},
            {
              status: 401,
              statusText: 'Unauthorized',
              data: { success: false, message: 'Access token expired' },
              headers: {},
              config,
            } as AxiosResponse
          );
          return Promise.reject(err);
        }

        // Retry attempt verifies updated Bearer token header
        expect(config.headers.Authorization).toBe('Bearer new_rotated_access_token');

        return {
          status: 200,
          statusText: 'OK',
          data: {
            success: true,
            message: 'OK',
            data: { id: 1, name: 'Model S' },
            timestamp: new Date().toISOString(),
          },
          headers: {},
          config,
        } as AxiosResponse;
      };

      const result = await apiClient.requestData<{ id: number; name: string }>({
        method: 'GET',
        url: '/vehicles/1',
      });

      expect(result).toEqual({ id: 1, name: 'Model S' });
      expect(postSpy).toHaveBeenCalledTimes(1);
      expect(TokenManager.getAccessToken()).toBe('new_rotated_access_token');
      expect(TokenManager.getRefreshToken()).toBe('new_rotated_refresh_token');
    });

    it('terminates session and wipes tokens when refresh token is rejected', async () => {
      TokenManager.setTokens('expired_access_token', 'revoked_refresh_token');
      useAppStore.getState().setAuthSession({
        token: 'expired_access_token',
        userId: 1,
        username: 'test_user',
        roles: ['ROLE_CO_OWNER'],
      });

      vi.spyOn(axios, 'post').mockRejectedValueOnce(
        new AxiosError('Invalid refresh token', 'ERR_BAD_REQUEST', {} as any, {}, {
          status: 401,
          statusText: 'Unauthorized',
          data: { success: false, message: 'Refresh token has been revoked' },
          headers: {},
          config: {} as any,
        } as AxiosResponse)
      );

      apiClient.raw.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
        const err = new AxiosError(
          'Unauthorized',
          'ERR_BAD_REQUEST',
          config,
          {},
          {
            status: 401,
            statusText: 'Unauthorized',
            data: { success: false, message: 'Access token expired' },
            headers: {},
            config,
          } as AxiosResponse
        );
        return Promise.reject(err);
      };

      await expect(
        apiClient.requestData({ method: 'GET', url: '/vehicles' })
      ).rejects.toThrow();

      expect(TokenManager.hasAccessToken()).toBe(false);
      expect(useAppStore.getState().isAuthenticated).toBe(false);
    });

    it('does not attempt token refresh for auth endpoints themselves', async () => {
      const postSpy = vi.spyOn(axios, 'post');

      apiClient.raw.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
        const err = new AxiosError(
          'Bad credentials',
          'ERR_BAD_REQUEST',
          config,
          {},
          {
            status: 401,
            statusText: 'Unauthorized',
            data: { success: false, message: 'Invalid email or password' },
            headers: {},
            config,
          } as AxiosResponse
        );
        return Promise.reject(err);
      };

      await expect(
        apiClient.requestData({ method: 'POST', url: '/auth/login' })
      ).rejects.toThrow();

      expect(postSpy).not.toHaveBeenCalled();
    });
  });

  describe('4. Request Cancellation & AbortController', () => {
    it('cancels in-flight requests using standard AbortController signal', async () => {
      const { controller, signal } = apiClient.createAbortController();

      apiClient.raw.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
        if (config.signal?.aborted) {
          const err = new AxiosError('canceled', 'ERR_CANCELED', config);
          return Promise.reject(err);
        }
        return {
          status: 200,
          statusText: 'OK',
          headers: {},
          config,
          data: { success: true, data: [] },
        } as AxiosResponse;
      };

      controller.abort('User cancelled action');

      try {
        await apiClient.requestData({
          method: 'GET',
          url: '/vehicles',
          signal,
        });
        expect.unreachable('Should have thrown ApiError on cancel');
      } catch (err) {
        expect(isCancelError(err)).toBe(true);
        expect((err as ApiError).isCancel).toBe(true);
      }
    });

    it('cancels prior in-flight request when getWithKey is invoked with same key', async () => {
      let firstRequestCanceled = false;

      apiClient.raw.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
        if (config.signal?.aborted) {
          const err = new AxiosError('canceled', 'ERR_CANCELED', config);
          return Promise.reject(err);
        }
        return {
          status: 200,
          statusText: 'OK',
          headers: {},
          config,
          data: { success: true, data: 'result' },
        } as AxiosResponse;
      };

      const p1 = apiClient
        .getWithKey('search_telemetry', '/vehicles/search?q=1')
        .catch((err) => {
          firstRequestCanceled = isCancelError(err);
          return null;
        });

      const p2 = await apiClient.getWithKey('search_telemetry', '/vehicles/search?q=2');

      await p1;
      expect(firstRequestCanceled).toBe(true);
      expect(p2.status).toBe(200);
    });
  });

  describe('5. Error Handling & Spring Boot ApiErrorResponse Parsing', () => {
    it('normalizes 400 Bad Request with field validation errors', async () => {
      const validationResponse = {
        success: false,
        status: 400,
        error: 'Bad Request',
        message: 'Validation failed for request payload',
        path: '/api/v1/bookings',
        timestamp: '2026-09-16T12:00:00Z',
        validationErrors: [
          { field: 'startTime', message: 'startTime cannot be in the past' },
          { field: 'endTime', message: 'endTime must be after startTime' },
        ],
      };

      apiClient.raw.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
        const err = new AxiosError('Bad Request', 'ERR_BAD_REQUEST', config, {}, {
          status: 400,
          statusText: 'Bad Request',
          data: validationResponse,
          headers: {},
          config,
        } as AxiosResponse);
        return Promise.reject(err);
      };

      try {
        await apiClient.requestData({ method: 'POST', url: '/bookings' });
        expect.unreachable('Should have thrown');
      } catch (err) {
        expect(isApiError(err)).toBe(true);
        const apiErr = err as ApiError;
        expect(apiErr.status).toBe(400);
        expect(apiErr.code).toBe('VALIDATION_ERROR');
        expect(apiErr.validationErrors).toHaveLength(2);
        expect(apiErr.getDetailedMessage()).toContain('startTime: startTime cannot be in the past');
      }
    });

    it('normalizes network drops without response into NETWORK_ERROR', async () => {
      apiClient.raw.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
        const err = new AxiosError('Network Error', 'ERR_NETWORK', config);
        return Promise.reject(err);
      };

      try {
        await apiClient.requestData({ method: 'GET', url: '/health' });
        expect.unreachable('Should have thrown');
      } catch (err) {
        const apiErr = err as ApiError;
        expect(apiErr.isNetworkError).toBe(true);
        expect(apiErr.code).toBe('NETWORK_ERROR');
      }
    });

    it('normalizes request timeouts into TIMEOUT error', async () => {
      apiClient.raw.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
        const err = new AxiosError('timeout of 10000ms exceeded', 'ECONNABORTED', config);
        return Promise.reject(err);
      };

      try {
        await apiClient.requestData({ method: 'GET', url: '/expensive-calculation' });
        expect.unreachable('Should have thrown');
      } catch (err) {
        const apiErr = err as ApiError;
        expect(apiErr.isTimeout).toBe(true);
        expect(apiErr.code).toBe('TIMEOUT');
      }
    });
  });

  describe('6. Consistent Response Parsing & Typed Helpers', () => {
    it('unwraps data payload cleanly with requestData', async () => {
      apiClient.raw.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
        return {
          status: 200,
          statusText: 'OK',
          headers: {},
          config,
          data: {
            success: true,
            message: 'Operation completed successfully',
            data: { balance: 5000000, currency: 'VND' },
            timestamp: '2026-09-16T12:00:00Z',
          },
        } as AxiosResponse;
      };

      const data = await apiClient.requestData<{ balance: number; currency: string }>({
        method: 'GET',
        url: '/ownership-groups/1/fund/balance',
      });

      expect(data.balance).toBe(5000000);
      expect(data.currency).toBe('VND');
    });

    it('supports apiGet, apiPost, apiPut, and apiDelete typed methods', async () => {
      const mockEnvelope = {
        success: true,
        message: 'Success',
        data: { ok: true },
        timestamp: '2026-09-16T12:00:00Z',
      };

      apiClient.raw.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
        return {
          status: 200,
          statusText: 'OK',
          headers: {},
          config,
          data: mockEnvelope,
        } as AxiosResponse;
      };

      const getRes = await apiClient.apiGet<{ ok: boolean }>('/status');
      const postRes = await apiClient.apiPost<{ ok: boolean }>('/action', { payload: 1 });
      const putRes = await apiClient.apiPut<{ ok: boolean }>('/update', { name: 'New' });
      const delRes = await apiClient.apiDelete<{ ok: boolean }>('/item/1');

      expect(getRes.ok).toBe(true);
      expect(postRes.ok).toBe(true);
      expect(putRes.ok).toBe(true);
      expect(delRes.ok).toBe(true);
    });
  });
});
