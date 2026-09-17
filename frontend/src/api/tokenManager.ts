/**
 * EVShare 3D Centralized Token Storage & Lifecycle Manager
 * Handles access token, refresh token (RFC 6819), and legacy token migration.
 */

import { AuthTokens } from './types';

export const ACCESS_TOKEN_STORAGE_KEY = 'evshare_access_token';
export const REFRESH_TOKEN_STORAGE_KEY = 'evshare_refresh_token';
export const LEGACY_TOKEN_STORAGE_KEY = 'evshare_token';

export interface StorageLike {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
  removeItem(key: string): void;
}

type TokenChangeListener = (tokens: {
  accessToken: string | null;
  refreshToken: string | null;
}) => void;

class TokenManagerClass {
  private inMemoryAccessToken: string | null = null;
  private inMemoryRefreshToken: string | null = null;
  private customStorage: StorageLike | null = null;
  private listeners: Set<TokenChangeListener> = new Set();

  constructor() {
    this.migrateLegacyToken();
  }

  public setStorage(storage: StorageLike | null): void {
    this.customStorage = storage;
    this.migrateLegacyToken();
  }

  private getActiveStorage(): StorageLike | null {
    if (this.customStorage) return this.customStorage;
    if (typeof localStorage !== 'undefined') {
      return localStorage;
    }
    return null;
  }

  public migrateLegacyToken(): void {
    const storage = this.getActiveStorage();
    if (!storage) return;
    try {
      const legacyToken = storage.getItem(LEGACY_TOKEN_STORAGE_KEY);
      const currentAccess = storage.getItem(ACCESS_TOKEN_STORAGE_KEY);

      if (legacyToken && !currentAccess) {
        storage.setItem(ACCESS_TOKEN_STORAGE_KEY, legacyToken);
      }
    } catch (err) {
      console.warn('[TokenManager] Failed to migrate legacy token:', err);
    }
  }

  public getAccessToken(): string | null {
    const storage = this.getActiveStorage();
    if (storage) {
      try {
        const token = storage.getItem(ACCESS_TOKEN_STORAGE_KEY);
        if (token) return token;
        return storage.getItem(LEGACY_TOKEN_STORAGE_KEY);
      } catch {
        return this.inMemoryAccessToken;
      }
    }
    return this.inMemoryAccessToken;
  }

  public getRefreshToken(): string | null {
    const storage = this.getActiveStorage();
    if (storage) {
      try {
        return storage.getItem(REFRESH_TOKEN_STORAGE_KEY);
      } catch {
        return this.inMemoryRefreshToken;
      }
    }
    return this.inMemoryRefreshToken;
  }

  public setTokens(accessToken: string, refreshToken?: string): void {
    this.inMemoryAccessToken = accessToken;
    if (refreshToken !== undefined) {
      this.inMemoryRefreshToken = refreshToken;
    }

    const storage = this.getActiveStorage();
    if (storage) {
      try {
        storage.setItem(ACCESS_TOKEN_STORAGE_KEY, accessToken);
        storage.setItem(LEGACY_TOKEN_STORAGE_KEY, accessToken);
        if (refreshToken) {
          storage.setItem(REFRESH_TOKEN_STORAGE_KEY, refreshToken);
        }
      } catch (err) {
        console.warn('[TokenManager] Failed to persist tokens to storage:', err);
      }
    }

    this.notifyListeners();
  }

  public setAuthTokens(tokens: AuthTokens): void {
    this.setTokens(tokens.accessToken, tokens.refreshToken);
  }

  public clearTokens(): void {
    this.inMemoryAccessToken = null;
    this.inMemoryRefreshToken = null;

    const storage = this.getActiveStorage();
    if (storage) {
      try {
        storage.removeItem(ACCESS_TOKEN_STORAGE_KEY);
        storage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
        storage.removeItem(LEGACY_TOKEN_STORAGE_KEY);
      } catch (err) {
        console.warn('[TokenManager] Failed to clear storage tokens:', err);
      }
    }

    this.notifyListeners();
  }

  public hasAccessToken(): boolean {
    return Boolean(this.getAccessToken());
  }

  public subscribe(listener: TokenChangeListener): () => void {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  private notifyListeners(): void {
    const tokens = {
      accessToken: this.getAccessToken(),
      refreshToken: this.getRefreshToken(),
    };
    this.listeners.forEach((listener) => {
      try {
        listener(tokens);
      } catch (err) {
        console.error('[TokenManager] Error in token listener:', err);
      }
    });
  }
}

export const TokenManager = new TokenManagerClass();
