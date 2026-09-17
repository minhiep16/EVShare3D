/**
 * EVShare 3D Authentication & Identity API Service
 * Connects directly to Spring Boot AuthController (/api/v1/auth) and UserController (/api/v1/users).
 */

import { apiClient } from './apiClient';
import { ApiResponse } from './types';
import { TokenManager } from './tokenManager';
import { useAppStore } from '@/stores/useAppStore';

export interface UserResponse {
  id: number;
  email: string;
  fullName: string;
  phoneNumber?: string;
  avatar3dUrl?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  roles: string[];
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn?: number;
  user: UserResponse;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  phoneNumber?: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface PasswordResetRequest {
  email: string;
}

export interface PasswordResetConfirmRequest {
  token: string;
  newPassword: string;
}

export interface UpdateProfileRequest {
  fullName?: string;
  phoneNumber?: string;
  avatar3dUrl?: string;
}

export const authApi = {
  /**
   * Registers a new user account with default ROLE_CO_OWNER.
   */
  register: async (payload: RegisterRequest): Promise<UserResponse> => {
    const res = await apiClient.post<ApiResponse<UserResponse>>('/auth/register', payload);
    return res.data.data;
  },

  /**
   * Authenticates user credentials, stores access & refresh tokens, and updates app session.
   */
  login: async (payload: LoginRequest): Promise<AuthResponse> => {
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', payload);
    const authData = res.data.data;

    // Persist tokens
    TokenManager.setTokens(authData.accessToken, authData.refreshToken);

    // Sync Zustand app session state
    useAppStore.getState().setAuthSession({
      token: authData.accessToken,
      userId: authData.user.id,
      username: authData.user.fullName || authData.user.email,
      roles: authData.user.roles,
    });

    return authData;
  },

  /**
   * Refreshes access token and rotates refresh token per RFC 6819.
   */
  refreshToken: async (refreshToken: string): Promise<AuthResponse> => {
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/auth/refresh', {
      refreshToken,
    });
    const authData = res.data.data;

    TokenManager.setTokens(authData.accessToken, authData.refreshToken);

    if (authData.user) {
      useAppStore.getState().setAuthSession({
        token: authData.accessToken,
        userId: authData.user.id,
        username: authData.user.fullName || authData.user.email,
        roles: authData.user.roles,
      });
    }

    return authData;
  },

  /**
   * Invalidates active session, clears stored tokens and resets app session state.
   */
  logout: async (refreshToken?: string): Promise<void> => {
    const tokenToRevoke = refreshToken || TokenManager.getRefreshToken();
    try {
      if (tokenToRevoke) {
        await apiClient.post<ApiResponse<void>>('/auth/logout', {
          refreshToken: tokenToRevoke,
        });
      }
    } catch {
      // Clean up locally even if server revocation call fails
    } finally {
      TokenManager.clearTokens();
      useAppStore.getState().clearAuthSession();
    }
  },

  /**
   * Initiates anti-enumeration password reset request.
   */
  requestPasswordReset: async (payload: PasswordResetRequest): Promise<void> => {
    await apiClient.post<ApiResponse<void>>('/auth/password-reset/request', payload);
  },

  /**
   * Confirms password reset with reset token and new credentials.
   */
  confirmPasswordReset: async (payload: PasswordResetConfirmRequest): Promise<void> => {
    await apiClient.post<ApiResponse<void>>('/auth/password-reset/confirm', payload);
  },

  /**
   * Retrieves authenticated user's profile and roles.
   */
  getCurrentUser: async (): Promise<UserResponse> => {
    const res = await apiClient.get<ApiResponse<UserResponse>>('/users/me');
    return res.data.data;
  },

  /**
   * Updates authenticated user's profile details or 3D avatar preference.
   */
  updateProfile: async (payload: UpdateProfileRequest): Promise<UserResponse> => {
    const res = await apiClient.put<ApiResponse<UserResponse>>('/users/me', payload);
    return res.data.data;
  },
};
